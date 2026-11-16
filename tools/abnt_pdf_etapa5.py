#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio da Etapa 5 (fidelidade, promocoes e auditoria) em PDF.

Uso:
    python tools/abnt_pdf_etapa5.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from abnt_doc import AbntReport  # noqa: E402

OUTPUT = "docs/etapa-5-fidelidade-promocoes-auditoria-abnt.pdf"


def build():
    report = AbntReport(
        OUTPUT,
        "RDN-API: FIDELIDADE, PROMOCOES E AUDITORIA",
        "Relatorio tecnico da Etapa 5: consentimento, pontos, campanhas e trilha de auditoria",
    )
    report.cover().summary()

    report.h1("INTRODUCAO")
    report.p(
        "As Etapas 1 a 4 colocaram o fluxo critico de pe: pedido persistido, autenticacao com JWT, "
        "estoque por unidade, pagamento mock idempotente e status atualizado. Restavam tres requisitos "
        "explicitos do roteiro: fidelizacao com consentimento, promocoes aplicadas ao pedido e registro "
        "de acoes sensiveis para auditoria.",
    )
    report.p(
        "A Etapa 5 tambem fecha uma lacuna funcional: a consulta de cardapio por unidade. O roteiro exige "
        "visualizacao do cardapio por unidade, mas a API de pedidos so conhecia produtos por identificador. "
        "Foram adicionados GET /unidades e GET /unidades/{id}/cardapio, alem do tratamento de parametro de "
        "query invalido, que antes virava 500.",
    )

    report.h1("ARQUITETURA E ESCOPO DA ETAPA 5")
    report.p(
        "O desenho segue o mesmo padrao das etapas anteriores: dominio puro, portas na aplicacao, "
        "adaptadores na infraestrutura e controllers finos. Nenhuma regra de ponto, desconto ou auditoria "
        "foi escrita no controller.",
    )
    report.table(
        "Artefatos produzidos na Etapa 5",
        ["Camada", "Artefato", "Papel"],
        [
            ["Database", "V5__loyalty_promotion_audit.sql",
             "audit_logs, orders.promotion_id, check de sinal e seed de campanhas"],
            ["Domain", "Order.subtotal/applyDiscount/addDiscount",
             "Desconto calculado e limitado pelo agregado"],
            ["Domain", "Promotion.isWithinValidity/applyDiscount",
             "Vigencia e percentual sobre o subtotal"],
            ["Domain", "LoyaltyPolicy, LoyaltyTransaction.earned/redeemed",
             "Conversao de pontos e extrato assinado"],
            ["Application", "LoyaltyService, PromotionService, AuditQueryService",
             "Casos de uso de fidelidade, campanhas e consulta de auditoria"],
            ["Infrastructure", "LoyaltyAccountGatewayAdapter, PromotionGatewayAdapter",
             "Persistencia com lock pessimista no resgate"],
            ["Infrastructure", "AuditGatewayAdapter, AuditLogJpaEntity",
             "Trilha em banco e replica no log da aplicacao"],
            ["API", "LoyaltyController, PromotionController, CatalogController, AuditController",
             "Contratos HTTP da etapa"],
        ],
        widths=[0.16, 0.40, 0.44],
    )

    report.h1("IMPLEMENTACAO")
    report.h2("Migration V5 e integridade")
    report.p(
        "V1 a V4 sao imutaveis por causa do checksum do Flyway. A V5 cria a tabela de auditoria, "
        "acrescenta a referencia de promocao no pedido, adiciona o CHECK de sinal do extrato e semeia "
        "tres campanhas. A terceira campanha esta ativa, mas com janela encerrada, para o teste negativo.",
    )
    report.code(
        """
CREATE TABLE audit_logs (
    id UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    actor_id UUID,
    actor_email VARCHAR(160),
    actor_role VARCHAR(20),
    action VARCHAR(60) NOT NULL,
    resource VARCHAR(40) NOT NULL,
    resource_id UUID,
    details VARCHAR(1000),
    ip_address VARCHAR(45),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

ALTER TABLE orders ADD COLUMN promotion_id UUID;
ALTER TABLE orders ADD CONSTRAINT fk_orders_promotion
    FOREIGN KEY (promotion_id) REFERENCES promotions (id);

ALTER TABLE loyalty_transactions ADD CONSTRAINT ck_loyalty_tx_sign CHECK (
    (type = 'EARNED' AND points > 0) OR
    (type IN ('REDEEMED', 'EXPIRED') AND points < 0));
""",
    )
    report.p(
        "A tabela de auditoria nao tem FK para users de proposito. O ator e guardado como snapshot "
        "(id, e-mail e perfil) para o historico sobreviver a desativacao do usuario.",
    )

    report.h2("Promocao calculada pelo dominio")
    report.p(
        "O pedido passou a expor subtotal(), applyDiscount(valor) e addDiscount(valor). O primeiro "
        "define o desconto limitado ao subtotal; o segundo acumula, o que permite aplicar um resgate "
        "de pontos sobre um pedido que ja recebeu promocao. Promotion.applyDiscount usa o subtotal, e "
        "nao o total, corrigindo o calculo percentual no momento da criacao.",
    )
    report.code(
        """
public BigDecimal applyDiscount(BigDecimal discount) {
    if (discount == null || discount.signum() < 0) {
        throw new IllegalArgumentException("desconto nao pode ser nulo ou negativo");
    }
    BigDecimal subtotal = subtotal();
    this.discountAmount = discount.compareTo(subtotal) > 0 ? subtotal : discount;
    return calculateTotal();
}

public BigDecimal addDiscount(BigDecimal extra) {
    BigDecimal current = discountAmount == null ? BigDecimal.ZERO : discountAmount;
    return applyDiscount(current.add(extra));
}
""",
    )
    report.p(
        "POST /pedidos aceita promocaoId opcional. Promocao inexistente responde 404; inativa, expirada "
        "ou abaixo do valor minimo responde 409 PROMOCAO_NAO_APLICAVEL. Apenas uma campanha por pedido, "
        "registrada em orders.promotion_id.",
    )

    report.h2("Fidelidade com consentimento")
    report.p(
        "O consentimento e pre-condicao de tratamento. PUT /fidelidade/consentimento cria a conta no "
        "primeiro opt-in e o opt-out preserva o saldo, mas congela o uso ate novo consentimento. Sem "
        "conta ou sem consentimento, o cliente nao acumula nem resgata pontos.",
    )
    report.table(
        "Regras de fidelidade",
        ["Regra", "Comportamento"],
        [
            ["Acumulo", "1 ponto por real pago, arredondado para baixo, somente em pagamento APPROVED"],
            ["Idempotencia", "Pedido ja com lancamento EARNED nao recebe novo acumulo"],
            ["Extrato", "EARNED positivo; REDEEMED e EXPIRED negativos"],
            ["Resgate", "1 ponto = R$ 0,10, aplicado como desconto no pedido"],
            ["Status do pedido", "Resgate exige AGUARDANDO_PAGAMENTO"],
            ["Concorrencia", "Conta lida com lock pessimista antes do debito"],
            ["Repeticao", "Segundo resgate no mesmo pedido responde 409 RESGATE_JA_APLICADO"],
        ],
        widths=[0.26, 0.74],
    )
    report.code(
        """
LoyaltyAccount account = accountGateway.findForUpdateByCustomerId(customerId)
        .orElseThrow(ConsentRequiredException::new);
if (!account.hasConsent()) {
    throw new ConsentRequiredException();
}
Order order = orderGateway.findWithItemsById(command.pedidoId())
        .orElseThrow(() -> new ResourceNotFoundException("Pedido", command.pedidoId()));
checkOwnership(order, customerId);
if (OrderStatus.AGUARDANDO_PAGAMENTO != order.getStatus()) {
    throw new RedemptionNotAllowedException(order.getId(), order.getStatus());
}
if (accountGateway.existsTransaction(order.getId(), TransactionType.REDEEMED)) {
    throw new RedemptionAlreadyAppliedException(order.getId());
}
""",
    )
    report.p(
        "O acumulo acontece no RequestPaymentService, depois da aprovacao. A conta e travada, o saldo "
        "e atualizado e o lancamento EARNED e gravado na mesma transacao do pagamento.",
    )

    report.h2("Auditoria de acoes sensiveis")
    report.p(
        "A trilha e explicita: cada caso de uso chama AuditGateway. Nao ha aspecto magico. Login grava "
        "sucesso e falha; pedido grava criacao, transicao e cancelamento; pagamento grava aprovacao, "
        "recusa e falha; fidelidade grava consentimento, acumulo e resgate; promocao aplicada gera "
        "registro proprio.",
    )
    report.table(
        "Acoes auditadas",
        ["Acao", "Origem", "Recurso"],
        [
            ["LOGIN_SUCCESS / LOGIN_FAILURE", "LoginService", "AUTH"],
            ["ORDER_CREATED", "CreateOrderService", "ORDER"],
            ["ORDER_STATUS_CHANGED / ORDER_CANCELLED", "ChangeOrderStatusService", "ORDER"],
            ["PAYMENT_APPROVED / DECLINED / FAILED", "RequestPaymentService", "PAYMENT"],
            ["PROMOTION_APPLIED", "CreateOrderService", "ORDER"],
            ["LOYALTY_CONSENT_GRANTED / REVOKED", "LoyaltyService", "LOYALTY_ACCOUNT"],
            ["LOYALTY_POINTS_EARNED / REDEEMED", "LoyaltyService", "LOYALTY_ACCOUNT"],
        ],
        widths=[0.44, 0.34, 0.22],
    )
    report.p(
        "details guarda apenas um resumo curto, como canal, total, transicao ou id da campanha. Nunca "
        "armazena senha, token ou payload completo de pagamento. GET /auditoria e restrito a ADMIN.",
    )

    report.h2("Consulta de catalogo e parametro invalido")
    report.p(
        "GET /unidades lista as unidades abertas com endereco. GET /unidades/{id}/cardapio devolve o "
        "cardapio com os itens e a disponibilidade. A consulta usa os repositorios e mappers existentes, "
        "sem novas tabelas.",
    )
    report.p(
        "O tratamento de MethodArgumentTypeMismatchException foi incluido no GlobalExceptionHandler. "
        "Assim, ?canalPedido=XYZ responde 400 PARAMETRO_INVALIDO em vez de cair no handler generico e "
        "devolver 500.",
    )

    report.h2("Contratos entregues")
    report.table(
        "Endpoints da Etapa 5",
        ["Metodo/Rota", "Perfis", "Sucesso", "Falhas previstas"],
        [
            ["POST /pedidos (promocaoId)", "CUSTOMER, STAFF, ADMIN", "201 com desconto e total",
             "400, 401, 403, 404 promocao, 409 nao aplicavel"],
            ["GET /promocoes", "Autenticado", "200 com campanhas e vigencia", "401"],
            ["GET /unidades", "Autenticado", "200 com unidades abertas", "401"],
            ["GET /unidades/{id}/cardapio", "Autenticado", "200 com itens", "401, 404"],
            ["GET /fidelidade", "CUSTOMER", "200 com saldo e consentimento", "401, 403"],
            ["GET /fidelidade/transacoes", "CUSTOMER", "200 paginado", "401, 403"],
            ["PUT /fidelidade/consentimento", "CUSTOMER", "200 com conta atualizada", "400, 401, 403"],
            ["POST /fidelidade/resgates", "CUSTOMER", "200 com desconto e novo saldo",
             "400, 401, 403, 404, 409"],
            ["GET /auditoria", "ADMIN", "200 paginado e filtravel", "400, 401, 403"],
        ],
        widths=[0.32, 0.24, 0.24, 0.20],
    )

    report.h1("DECISOES E TRADE-OFFS")
    report.table(
        "Decisoes arquiteturais da Etapa 5",
        ["Decisao", "Alternativa descartada", "Consequencia aceita"],
        [
            ["Resgate vira desconto no pedido", "Debito de pontos sem efeito",
             "Fecha o ciclo com o pagamento; exige pedido aguardando"],
            ["Pontos assinados no extrato", "Sempre positivos com tipo",
             "Soma direta confere o saldo; leitura precisa do sinal"],
            ["Promocao explicita no request", "Melhor promocao automatica",
             "Total previsivel; cliente escolhe a campanha"],
            ["Auditoria explicita", "Aspecto ou evento assincrono",
             "Mais chamadas no codigo; rastreabilidade direta"],
            ["Audit sem FK para users", "FK com cascade",
             "Historico preservado; sem integridade referencial do ator"],
            ["Login tolera falha de auditoria", "Falhar o login junto",
             "Autenticacao nao depende do banco de auditoria"],
        ],
        widths=[0.30, 0.30, 0.40],
    )

    report.h1("CONCEITOS FUNDAMENTAIS")
    report.bullets([
        "Consentimento: base legal e pre-condicao tecnica para tratar dados de fidelidade.",
        "Minimizacao: a auditoria guarda o necessario, nao o payload inteiro.",
        "Idempotencia de pontos: um pedido gera no maximo um lancamento EARNED.",
        "Lock pessimista: serializa o debito de pontos para nao gastar o mesmo saldo duas vezes.",
        "Desconto no agregado: o pedido limita o desconto ao subtotal, inclusive no resgate.",
        "Trilha de auditoria: registro append-only de acoes sensiveis, consultavel por ADMIN.",
        "Erro padronizado: toda falha continua no formato ApiError.",
    ])

    report.h1("ERROS COMUNS E ARMADILHAS")
    report.bullets([
        "Calcular percentual sobre o total ja descontado gera desconto menor que o esperado.",
        "Deixar o cliente enviar total ou desconto permite fraude no valor cobrado.",
        "Acumular pontos antes da aprovacao do pagamento cria saldo sem receita.",
        "Nao travar a conta no resgate permite saldo negativo em requisicoes concorrentes.",
        "Auditar senha, token ou payload completo viola a minimizacao da LGPD.",
        "Editar V1 para criar audit_logs quebra o checksum do Flyway.",
        "Nao tratar tipo invalido de query param transforma erro de cliente em 500.",
    ])

    report.h1("EXERCICIOS DE FIXACAO")
    report.p(
        "Exercicio 1. Um pedido de R$ 51,80 recebe uma promocao de 10%. Qual o desconto e o total? "
        "Se o mesmo pedido resgatar 100 pontos depois, qual o novo total?",
    )
    report.p(
        "Gabarito: desconto de R$ 5,18 e total de R$ 46,62. O resgate de 100 pontos vale R$ 10,00, "
        "entao o total final e R$ 36,62.",
    )
    report.p(
        "Exercicio 2. Por que o resgate exige pedido em AGUARDANDO_PAGAMENTO e nao aceita pedido ja pago?",
    )
    report.p(
        "Gabarito: o valor cobrado e o total do pedido. Se o pagamento ja foi aprovado, alterar o total "
        "criaria divergencia entre cobranca e pedido. O resgate so faz sentido antes da cobranca.",
    )
    report.p(
        "Exercicio 3. O cliente pagou, ganhou 51 pontos e tenta resgatar 100. O que a API responde?",
    )
    report.p(
        "Gabarito: 409 PONTOS_INSUFICIENTES, com detalhe do saldo disponivel e do valor solicitado.",
    )

    report.h1("CHECKLIST DE AUTOAVALIACAO")
    report.bullets([
        "Sei explicar por que o consentimento e pre-condicao de fidelidade.",
        "Sei diferenciar opt-in, opt-out e congelamento de saldo.",
        "Sei dizer quando um pedido ganha pontos e quando nao ganha.",
        "Sei explicar por que pontos sao assinados no extrato.",
        "Sei justificar o lock pessimista no resgate.",
        "Sei apontar onde a promocao e validada e onde o desconto e limitado.",
        "Sei listar as acoes sensiveis auditadas e o que nunca vai em details.",
        "Sei explicar por que audit_logs nao tem FK para users.",
    ])

    report.h1("CONSIDERACOES FINAIS E PROXIMO PASSO")
    report.p(
        "A Etapa 5 completa os requisitos de fidelizacao, promocoes, auditoria e consulta de cardapio. "
        "O fluxo critico segue fechado e a suite automatizada cobre os cenarios positivos e negativos "
        "da etapa, incluindo consentimento, saldo, promocao expirada, resgate repetido e acesso a "
        "auditoria.",
    )
    report.p(
        "O proximo passo e a Etapa 6: documentacao OpenAPI/Swagger, README reproduzivel, colecao "
        "Postman com pelo menos dez cenarios, validacao em PostgreSQL real e relatorio final unico "
        "em ABNT, com declaracao de uso de IA.",
    )

    report.references([
        "ASSOCIACAO BRASILEIRA DE NORMAS TECNICAS. NBR 14724: informacao e documentacao - trabalhos "
        "academicos - apresentacao. Rio de Janeiro: ABNT, 2011.",
        "ASSOCIACAO BRASILEIRA DE NORMAS TECNICAS. NBR 6023: referencias - elaboracao. Rio de Janeiro: "
        "ABNT, 2018.",
        "BRASIL. Lei no 13.709, de 14 de agosto de 2018. Lei Geral de Protecao de Dados Pessoais (LGPD). "
        "Brasilia, DF: Presidencia da Republica, 2018.",
        "RED GATE. Flyway documentation. Disponivel em: https://documentation.red-gate.com/flyway. "
        "Acesso em: 13 nov. 2026.",
        "SPRING. Spring Boot reference documentation. Disponivel em: https://docs.spring.io/spring-boot/. "
        "Acesso em: 13 nov. 2026.",
    ])

    return report.save()


if __name__ == "__main__":
    path = build()
    print("PDF gerado em %s" % path)
