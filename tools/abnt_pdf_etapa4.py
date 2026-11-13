#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio da Etapa 4 (pagamento mock) em PDF seguindo a NBR 14724.

Uso:
    python tools/abnt_pdf_etapa4.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from abnt_doc import AbntReport  # noqa: E402

OUTPUT = "docs/etapa-4-pagamento-abnt.pdf"


def build():
    report = AbntReport(
        OUTPUT,
        "RDN-API: PAGAMENTO SIMULADO E STATUS DO PEDIDO",
        "Relat\u00f3rio t\u00e9cnico da Etapa 4: gateway mock, idempot\u00eancia e efeito no pedido",
    )
    report.cover().summary()

    report.h1("INTRODU\u00c7\u00c3O")
    report.p(
        "A Etapa 3 entregou a API de pedidos: cria\u00e7\u00e3o com valida\u00e7\u00e3o de estoque, listagem filtrada, "
        "consulta e mudan\u00e7a de status. Faltava a segunda metade do fluxo cr\u00edtico recomendado pelo roteiro: "
        "solicitar o pagamento a um provedor externo simulado e refletir o resultado no pedido.",
    )
    report.p(
        "O provedor \u00e9 simulado por exig\u00eancia do enunciado, mas o desenho n\u00e3o \u00e9 de brinquedo: existe porta "
        "de sa\u00edda, adaptador substitu\u00edvel, chave de idempot\u00eancia obrigat\u00f3ria, registro do payload "
        "bruto para auditoria e transi\u00e7\u00e3o de status derivada do resultado.",
    )

    report.h1("ARQUITETURA E ESCOPO DA ETAPA 4")
    report.p(
        "Tr\u00eas pe\u00e7as novas sustentam a etapa: um valor a mais no enum de status, uma porta para o provedor "
        "externo e um caso de uso que orquestra as duas coisas. O dom\u00ednio de pagamento j\u00e1 existia desde a "
        "Etapa 1 e n\u00e3o foi reescrito.",
    )
    report.table(
        "Artefatos produzidos na Etapa 4",
        ["Camada", "Artefato", "Papel"],
        [
            ["Domain", "OrderStatus.AGUARDANDO_PAGAMENTO",
             "Estado inicial de pedido pago via MOCK"],
            ["Domain", "Order.awaitPayment / confirmPayment",
             "Cria\u00e7\u00e3o em espera e libera\u00e7\u00e3o para a cozinha"],
            ["Application", "PaymentProvider, ChargeRequest, ChargeResult",
             "Porta e contrato do provedor externo"],
            ["Application", "RequestPaymentService",
             "Idempot\u00eancia, cobran\u00e7a e efeito no pedido"],
            ["Infrastructure", "MockPaymentGatewayAdapter",
             "Provedor simulado determin\u00edstico"],
            ["Infrastructure", "PaymentRepositoryAdapter, PaymentMapper.toEntity",
             "Persist\u00eancia da tentativa"],
            ["Database", "V4__add_awaiting_payment_status.sql",
             "Recreia a constraint de status sem editar V1"],
            ["API", "PaymentController, RequestPaymentRequest, PaymentResponse",
             "POST e GET de pagamentos do pedido"],
        ],
        widths=[0.16, 0.38, 0.46],
    )

    report.h1("IMPLEMENTA\u00c7\u00c3O")
    report.h2("Novo estado e migration V4")
    report.p(
        "Pedido pago por MOCK precisa de um estado de espera. Em vez de criar dois valores novos, foi criado "
        "apenas AGUARDANDO_PAGAMENTO: RECEIVED passou a significar pagamento confirmado, na fila da cozinha. "
        "A transi\u00e7\u00e3o v\u00e1lida \u00e9 AGUARDANDO_PAGAMENTO para RECEIVED quando aprovado e para CANCELLED "
        "quando recusado.",
    )
    report.p(
        "Como V1 j\u00e1 foi aplicada, edit\u00e1-la quebraria o checksum do Flyway. A V4 remove e recria a constraint:",
    )
    report.code(
        """
ALTER TABLE orders DROP CONSTRAINT ck_orders_status;

ALTER TABLE orders ADD CONSTRAINT ck_orders_status CHECK (status IN
    ('AGUARDANDO_PAGAMENTO', 'RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED'));
""",
    )
    report.p(
        "No dom\u00ednio, awaitPayment s\u00f3 aceita pedido sem status e confirmPayment delega para changeStatus. "
        "Nenhuma regra foi duplicada: a tabela de transi\u00e7\u00f5es continua sendo a \u00fanica fonte de verdade.",
    )
    report.code(
        """
public void awaitPayment() {
    if (status != null) {
        throw new IllegalStateException("pedido ja iniciado com status " + status);
    }
    this.status = OrderStatus.AGUARDANDO_PAGAMENTO;
}

public void confirmPayment() {
    changeStatus(OrderStatus.RECEIVED);
}
""",
    )

    report.h2("Porta do provedor e mock determin\u00edstico")
    report.p(
        "A aplica\u00e7\u00e3o conhece apenas PaymentProvider. O adaptador simulado devolve o desfecho pedido no "
        "campo simular, o que mant\u00e9m os testes est\u00e1veis e dispensa rede.",
    )
    report.code(
        """
@Override
public ChargeResult charge(ChargeRequest request) {
    SimulationMode mode = request.simular() == null
            ? SimulationMode.APROVADO : request.simular();
    String transactionId = "MOCK-" + UUID.randomUUID();

    return switch (mode) {
        case APROVADO -> new ChargeResult(true, false, transactionId, payload(mode, request, transactionId));
        case RECUSADO -> new ChargeResult(false, false, null, payload(mode, request, null));
        case FALHA -> new ChargeResult(false, true, null, payload(mode, request, null));
    };
}
""",
    )

    report.h2("Idempot\u00eancia e efeito no pedido")
    report.p(
        "O cabe\u00e7alho Idempotency-Key \u00e9 obrigat\u00f3rio porque a coluna correspondente \u00e9 NOT NULL e UNIQUE. "
        "A mesma chave repetida devolve a tentativa original com 200, sem nova linha em payments. A mesma chave "
        "usada em outro pedido retorna 409.",
    )
    report.code(
        """
Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
if (existing.isPresent()) {
    return replay(existing.get(), pedidoId);
}

if (OrderStatus.AGUARDANDO_PAGAMENTO != order.getStatus()) {
    throw new OrderNotAwaitingPaymentException(pedidoId, order.getStatus());
}

Payment payment = new Payment(order, order.getTotalAmount(),
        command.formaPagamento(), key);
ChargeResult result = paymentProvider.charge(new ChargeRequest(
        pedidoId, order.getTotalAmount(), command.formaPagamento(), key, command.simular()));

applyResult(payment, order, result);
""",
    )
    report.table(
        "Efeito de cada desfecho",
        ["Resultado", "payments.status", "orders.status", "Efeito pr\u00e1tico"],
        [
            ["APROVADO", "APPROVED", "RECEIVED", "Pedido liberado para a cozinha"],
            ["RECUSADO", "DECLINED", "CANCELLED", "Pedido cancelado com motivo registrado"],
            ["FALHA", "FAILED", "AGUARDANDO_PAGAMENTO", "Cliente pode tentar de novo"],
        ],
        widths=[0.16, 0.18, 0.26, 0.40],
    )
    report.p(
        "Falha t\u00e9cnica \u00e9 condi\u00e7\u00e3o transit\u00f3ria; cancelar o pedido por timeout do provedor seria uma "
        "decis\u00e3o de neg\u00f3cio ruim. O registro fica como FAILED, com o payload bruto para investiga\u00e7\u00e3o.",
    )

    report.h2("Contratos")
    report.table(
        "Endpoints entregues na Etapa 4",
        ["M\u00e9todo/Rota", "Perfis", "Sucesso", "Falhas previstas"],
        [
            ["POST /pedidos/{id}/pagamentos", "CUSTOMER, STAFF, ADMIN",
             "201 na primeira tentativa; 200 em replay",
             "400 sem Idempotency-Key; 401; 403; 404; 409 status ou chave"],
            ["GET /pedidos/{id}/pagamentos", "Autenticado", "200 com hist\u00f3rico decrescente",
             "401; 403 outro cliente; 404"],
        ],
        widths=[0.30, 0.22, 0.24, 0.24],
    )
    report.p(
        "O valor cobrado nunca vem do corpo da requisi\u00e7\u00e3o: \u00e9 sempre order.getTotalAmount(). O request "
        "traz apenas forma de pagamento e, no mock, o desfecho desejado.",
    )

    report.h1("DECIS\u00d5ES E TRADE-OFFS")
    report.table(
        "Decis\u00f5es arquiteturais da Etapa 4",
        ["Decis\u00e3o", "Alternativa descartada", "Consequ\u00eancia aceita"],
        [
            ["Um \u00fanico status novo", "AGUARDANDO_PAGAMENTO e PAGO",
             "RECEIVED precisa ser lido como \u201cpago, na fila\u201d"],
            ["V4 recriando a constraint", "Editar V1",
             "Uma migration a mais; checksum do Flyway preservado"],
            ["Campo simular no request", "Regra por valor ou aleat\u00f3ria",
             "Determinismo nos testes; campo inexistente em produ\u00e7\u00e3o"],
            ["Idempotency-Key no cabe\u00e7alho", "Chave no corpo",
             "Padr\u00e3o de mercado; cliente precisa montar o header"],
            ["Falha n\u00e3o cancela pedido", "Cancelar por timeout",
             "Pedido permanece retent\u00e1vel"],
            ["Hist\u00f3rico por pedido", "Apenas o pagamento corrente",
             "Mais linhas em payments; auditoria completa"],
        ],
        widths=[0.28, 0.32, 0.40],
    )

    report.h1("CONCEITOS FUNDAMENTAIS")
    report.bullets([
        "Idempot\u00eancia: repetir a mesma chave devolve o mesmo resultado, sem novo efeito.",
        "Gateway/porta: a aplica\u00e7\u00e3o declara o contrato; o provedor real ou mock implementa.",
        "Payload bruto: guardar a resposta original permite auditar diverg\u00eancia de valor.",
        "Estado transit\u00f3rio: FAILED n\u00e3o encerra o pedido; apenas a tentativa.",
        "Migration versionada: V1 imut\u00e1vel; mudan\u00e7a de constraint vai em V4.",
        "Replay: resposta 200 com o recurso j\u00e1 existente, em vez de 201 duplicado.",
    ])

    report.h1("ERROS COMUNS E ARMADILHAS")
    report.bullets([
        "Aceitar valor no corpo da cobran\u00e7a permite pagar menos que o total do pedido.",
        "N\u00e3o tratar a viola\u00e7\u00e3o da unique de idempotency_key deixa a corrida entre duas requisi\u00e7\u00f5es virar 500.",
        "Cancelar o pedido em falha t\u00e9cnica impede a retentativa e pune o cliente por instabilidade do provedor.",
        "Editar V1 para acrescentar o novo status quebra o checksum e a aplica\u00e7\u00e3o n\u00e3o sobe.",
        "Aprovar o pagamento sem mudar o status deixa o pedido preso em AGUARDANDO_PAGAMENTO.",
        "Guardar a senha ou dados sens\u00edveis no payload de auditoria viola a minimiza\u00e7\u00e3o da LGPD.",
    ])

    report.h1("EXERC\u00cdCIOS DE FIXA\u00c7\u00c3O")
    report.p(
        "Exerc\u00edcio 1. A primeira requisi\u00e7\u00e3o de pagamento chega com a chave K e \u00e9 aprovada. A segunda "
        "chega com a mesma chave K. Quantas linhas existem em payments e qual status \u00e9 devolvido?",
    )
    report.p(
        "Gabarito: uma linha, e a resposta \u00e9 200 com o pagamento original. O provedor n\u00e3o \u00e9 chamado de novo.",
    )
    report.p(
        "Exerc\u00edcio 2. Um pedido est\u00e1 em PREPARING e recebe uma nova solicita\u00e7\u00e3o de pagamento. Qual "
        "resposta e por qu\u00ea?",
    )
    report.p(
        "Gabarito: 409 PEDIDO_NAO_AGUARDA_PAGAMENTO. A cobran\u00e7a s\u00f3 \u00e9 permitida em AGUARDANDO_PAGAMENTO; "
        "qualquer outro estado j\u00e1 passou da etapa de pagamento.",
    )
    report.p(
        "Exerc\u00edcio 3. Por que a aprova\u00e7\u00e3o e a mudan\u00e7a de status est\u00e3o no mesmo m\u00e9todo transacional?",
    )
    report.p(
        "Gabarito: para evitar estado parcial. Se o pagamento fosse gravado e o pedido n\u00e3o mudasse de status, "
        "o sistema ficaria com cobran\u00e7a aprovada e pedido preso. A transa\u00e7\u00e3o garante os dois ou nenhum.",
    )

    report.h1("CHECKLIST DE AUTOAVALIA\u00c7\u00c3O")
    report.bullets([
        "Sei explicar por que RECEIVED passou a significar pago.",
        "Sei justificar a V4 em vez de editar a V1.",
        "Sei dizer o que acontece com a mesma chave em outro pedido.",
        "Sei diferenciar DECLINED de FAILED no efeito sobre o pedido.",
        "Sei apontar onde o payload bruto \u00e9 gravado e por qu\u00ea.",
        "Sei explicar por que o valor vem do dom\u00ednio e n\u00e3o do request.",
        "Sei descrever a resposta de replay e o c\u00f3digo 200.",
    ])

    report.h1("CONSIDERA\u00c7\u00d5ES FINAIS E PR\u00d3XIMO PASSO")
    report.p(
        "Com a Etapa 4, o fluxo cr\u00edtico do roteiro est\u00e1 completo: pedido criado com canal e estoque, "
        "pagamento simulado com idempot\u00eancia e status atualizado conforme o desfecho. A su\u00edte automatizada "
        "cobre os cen\u00e1rios positivos e negativos de autentica\u00e7\u00e3o, valida\u00e7\u00e3o, estoque, transi\u00e7\u00e3o "
        "e idempot\u00eancia.",
    )
    report.p(
        "O pr\u00f3ximo passo \u00e9 a Etapa 5: fideliza\u00e7\u00e3o com consentimento, promo\u00e7\u00f5es aplicadas ao pedido e "
        "registro de a\u00e7\u00f5es sens\u00edveis para auditoria, seguida da Etapa 6 com OpenAPI, README e cole\u00e7\u00e3o "
        "Postman.",
    )

    report.references([
        "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. NBR 14724: informa\u00e7\u00e3o e documenta\u00e7\u00e3o "
        "\u2014 trabalhos acad\u00eamicos \u2014 apresenta\u00e7\u00e3o. Rio de Janeiro: ABNT, 2011.",
        "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. NBR 6023: refer\u00eancias \u2014 elabora\u00e7\u00e3o. "
        "Rio de Janeiro: ABNT, 2018.",
        "BRASIL. Lei n\u00ba 13.709, de 14 de agosto de 2018. Lei Geral de Prote\u00e7\u00e3o de Dados Pessoais "
        "(LGPD). Bras\u00edlia, DF: Presid\u00eancia da Rep\u00fablica, 2018.",
        "OWASP FOUNDATION. Password storage cheat sheet. Dispon\u00edvel em: "
        "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html. "
        "Acesso em: 13 nov. 2026.",
        "RED GATE. Flyway documentation. Dispon\u00edvel em: "
        "https://documentation.red-gate.com/flyway. Acesso em: 13 nov. 2026.",
        "SPRING. Spring Boot reference documentation. Dispon\u00edvel em: "
        "https://docs.spring.io/spring-boot/. Acesso em: 13 nov. 2026.",
    ])

    return report.save()


if __name__ == "__main__":
    path = build()
    print("PDF gerado em %s" % path)
