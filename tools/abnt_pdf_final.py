#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio final do projeto em PDF seguindo a NBR 14724.

Uso:
    python tools/abnt_pdf_final.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from abnt_doc import AbntReport  # noqa: E402

OUTPUT = "docs/projeto-back-end-final-abnt.pdf"


def build():
    report = AbntReport(
        OUTPUT,
        "RDN-API: BACK-END MULTICANAL PARA REDE DE LANCHONETES",
        "Relatorio final do Projeto Multidisciplinar - Trilha Back-End",
    )
    report.cover().summary()

    report.h1("1 INTRODUCAO E OBJETIVOS")
    report.p(
        "Este relatorio descreve a solucao de back-end desenvolvida para uma rede de lanchonetes em "
        "expansao. O sistema precisa atender pedidos em multiplos canais, controlar estoque por unidade, "
        "simular o pagamento externo, manter um programa de fidelidade com consentimento e registrar "
        "acoes sensiveis para auditoria.",
    )
    report.p(
        "O objetivo tecnico foi entregar uma API REST executavel, persistida em banco relacional, "
        "organizada em camadas e reproduzivel por terceiros. O fluxo critico escolhido foi o Fluxo A do "
        "roteiro: Pedido, Pagamento mock e Atualizacao de status. A solucao tambem cobre consulta de "
        "cardapio, promocoes, fidelidade e auditoria.",
    )
    report.p(
        "A API foi construida com Java 25, Spring Boot 4.1.2, Spring Data JPA, Spring Security com JWT, "
        "Flyway e PostgreSQL 16 como banco alvo. Os testes automatizados usam H2 em modo PostgreSQL para "
        "serem reproduziveis sem Docker.",
    )

    report.h1("2 ANALISE E REQUISITOS")
    report.h2("2.1 Requisitos funcionais")
    report.table(
        "Requisitos funcionais e cobertura",
        ["Requisito", "Implementacao", "Situacao"],
        [
            ["Cadastro e autenticacao com perfis", "JWT, BCrypt e perfis ADMIN, STAFF, KITCHEN e CUSTOMER", "Implementado"],
            ["Consulta de cardapio por unidade", "GET /unidades e GET /unidades/{id}/cardapio", "Implementado"],
            ["Realizacao de pedidos", "POST /pedidos com canal, itens, promocao e calculo de total", "Implementado"],
            ["Atualizacao de status", "PATCH /pedidos/{id}/status com maquina de estados", "Implementado"],
            ["Controle de estoque", "Baixa por unidade com lock e recusa de saldo insuficiente", "Implementado no fluxo A"],
            ["Pagamento externo mock", "POST /pedidos/{id}/pagamentos com idempotencia", "Implementado"],
            ["Fidelizacao com consentimento", "Opt-in/out, saldo, extrato, acumulo e resgate", "Implementado"],
            ["Promocoes e campanhas", "GET /promocoes e promocao opcional no pedido", "Implementado"],
            ["Auditoria de acoes sensiveis", "audit_logs e GET /auditoria restrito a ADMIN", "Implementado"],
            ["Multicanalidade", "canalPedido obrigatorio e filtro por canal", "Implementado"],
        ],
        widths=[0.30, 0.48, 0.22],
    )

    report.h2("2.2 Requisitos nao funcionais")
    report.table(
        "Requisitos nao funcionais e evidencias",
        ["Requisito", "Evidencia"],
        [
            ["Seguranca e LGPD", "Hash BCrypt, JWT, autorizacao por perfil, consentimento e minimizacao"],
            ["Padrao de erro", "ApiError com error, message, details, timestamp e path"],
            ["Persistencia consistente", "Flyway V1 a V5 e ddl-auto=validate"],
            ["Tolerancia a falha de pagamento", "Resultado FAILED mantem o pedido retentavel"],
            ["Idempotencia", "Idempotency-Key em pagamentos e replay sem duplicar"],
            ["Concorrencia", "Lock pessimista no estoque e na conta de fidelidade"],
            ["Documentacao", "OpenAPI, Swagger, README e colecao Postman"],
            ["Testabilidade", "Suite automatizada com testes unitarios e integracao HTTP"],
        ],
        widths=[0.30, 0.70],
    )

    report.h2("2.3 Multicanalidade")
    report.p(
        "O canal de origem e um dado de dominio. O pedido nasce com canalPedido obrigatorio, aceitando "
        "APP, TOTEM, BALCAO, PICKUP e WEB. A listagem operacional permite filtrar por canal e por status, "
        "o que sustenta a consolidacao por canal exigida pelo roteiro.",
    )

    report.h2("2.4 Priorizacao")
    report.p(
        "A prioridade foi fechar o Fluxo A ponta a ponta antes de ampliar o escopo. O Fluxo B, com "
        "endpoints administrativos de entrada e saida de estoque, nao foi implementado; a restricao de "
        "venda por saldo existe e e aplicada no pedido. Login social, refresh token e CRUD administrativo "
        "de cardapio tambem ficaram fora do MVP.",
    )

    report.h1("3 DIAGRAMAS E MODELAGEM")
    report.h2("3.1 Casos de uso")
    report.p(
        "Atores: Cliente, Atendente, Cozinha, Administrador e Gateway de Pagamento. Os casos principais "
        "sao autenticar, consultar cardapio, criar pedido, solicitar pagamento, acompanhar status, "
        "cancelar pedido, consentir com fidelidade, resgatar pontos, consultar campanhas e consultar "
        "auditoria.",
    )
    report.p(
        "Feature critica: Realizar pedido e solicitar pagamento. Pre-condicao: usuario autenticado e "
        "unidade aberta. Fluxo principal: cria o pedido, valida catalogo, aplica promocao opcional, baixa "
        "estoque, solicita o pagamento mock com chave de idempotencia e atualiza o status conforme o "
        "resultado. Excecoes: estoque insuficiente, produto inexistente, promocao invalida, chave "
        "reutilizada e pagamento recusado.",
    )

    report.h2("3.2 DER e modelo de dados")
    report.p(
        "O DER completo esta em docs/DER.md. As entidades principais sao users, branches, menus, "
        "menu_items, inventory_items, inventory_movements, orders, order_items, payments, "
        "loyalty_accounts, loyalty_transactions, promotions, auth_sessions e audit_logs.",
    )
    report.table(
        "Relacionamentos relevantes",
        ["Relacao", "Cardinalidade", "Regra"],
        [
            ["branches - menus", "1 para 0..1", "Um cardapio por filial"],
            ["menus - menu_items", "1 para N", "Itens do cardapio"],
            ["branches - inventory_items", "1 para N", "Estoque por unidade"],
            ["menu_items - inventory_items", "1 para N", "Saldo por item e unidade"],
            ["orders - order_items", "1 para N", "Itens com preco copiado"],
            ["orders - payments", "1 para N", "Tentativas de cobranca"],
            ["promotions - orders", "1 para N", "Campanha aplicada ao pedido"],
            ["users - loyalty_accounts", "1 para 0..1", "Conta com consentimento"],
            ["loyalty_accounts - loyalty_transactions", "1 para N", "Extrato de pontos"],
        ],
        widths=[0.34, 0.18, 0.48],
    )

    report.h2("3.3 Classes e sequencia")
    report.p(
        "No dominio, Order concentra itens, total, desconto e transicoes de status. Promotion calcula "
        "vigencia e desconto. LoyaltyAccount controla consentimento e saldo. LoyaltyPolicy converte reais "
        "em pontos. Payment guarda a tentativa e o resultado do gateway.",
    )
    report.p(
        "A sequencia do fluxo critico e: controller recebe o request, caso de uso valida catalogo e "
        "estoque, dominio calcula o total, OrderGateway persiste, PaymentController solicita a cobranca, "
        "RequestPaymentService aplica o resultado, o pedido muda de status e a auditoria registra o "
        "evento. A aprovacao ainda acumula pontos quando o cliente consentiu.",
    )

    report.h1("4 API E ENDPOINTS")
    report.table(
        "Contratos principais",
        ["Metodo/Rota", "Perfis", "Sucesso", "Erros previstos"],
        [
            ["POST /auth/login", "publico", "200 com accessToken", "400, 401, 403"],
            ["GET /auth/me", "autenticado", "200 com usuario", "401"],
            ["GET /unidades", "autenticado", "200 com unidades", "401"],
            ["GET /unidades/{id}/cardapio", "autenticado", "200 com itens", "401, 404"],
            ["GET /promocoes", "autenticado", "200 com campanhas", "401"],
            ["POST /pedidos", "CUSTOMER, STAFF, ADMIN", "201 com pedido", "400, 401, 403, 404, 409"],
            ["GET /pedidos", "ADMIN, STAFF", "200 paginado", "400, 401, 403"],
            ["GET /pedidos/{id}", "autenticado", "200 com itens", "401, 403, 404"],
            ["PATCH /pedidos/{id}/status", "KITCHEN, STAFF, ADMIN", "200 atualizado", "400, 401, 403, 404, 409"],
            ["POST /pedidos/{id}/pagamentos", "CUSTOMER, STAFF, ADMIN", "201 ou 200 replay", "400, 401, 403, 404, 409"],
            ["GET /pedidos/{id}/pagamentos", "autenticado", "200 historico", "401, 403, 404"],
            ["GET /fidelidade", "CUSTOMER", "200 saldo", "401, 403"],
            ["GET /fidelidade/transacoes", "CUSTOMER", "200 extrato", "401, 403"],
            ["PUT /fidelidade/consentimento", "CUSTOMER", "200 conta", "400, 401, 403"],
            ["POST /fidelidade/resgates", "CUSTOMER", "200 desconto", "400, 401, 403, 404, 409"],
            ["GET /auditoria", "ADMIN", "200 eventos", "400, 401, 403"],
        ],
        widths=[0.30, 0.22, 0.22, 0.26],
    )
    report.p(
        "Todas as falhas usam o mesmo JSON: error, message, details, timestamp e path. Os status "
        "coerentes incluem 201 para criacao, 200 para consulta e replay, 400 para validacao, 401 para "
        "nao autenticado, 403 para perfil sem permissao, 404 para recurso inexistente e 409 para conflito "
        "de regra de negocio.",
    )

    report.h1("5 LGPD, PRIVACIDADE E SEGURANCA")
    report.p(
        "Dados pessoais coletados: nome, e-mail, perfil, hash de senha e, no programa de fidelidade, "
        "saldo e extrato de pontos. A finalidade e autenticar o usuario, executar pedidos e operar o "
        "programa de fidelidade. A base legal do tratamento de fidelidade e o consentimento explicito, "
        "registrado com data e hora; o cliente pode revogar o consentimento.",
    )
    report.table(
        "Controles de seguranca e privacidade",
        ["Controle", "Como foi aplicado"],
        [
            ["Hash de senha", "BCrypt com custo 10; a senha pura nao e persistida"],
            ["Autenticacao", "JWT HS256 com segredo configuravel e expiracao"],
            ["Autorizacao", "Perfis ADMIN, STAFF, KITCHEN e CUSTOMER nas rotas"],
            ["Minimizacao", "Respostas sem passwordHash; auditoria sem payload sensivel"],
            ["Consentimento", "Opt-in/opt-out em loyalty_accounts com timestamp"],
            ["Auditoria", "audit_logs com ator snapshot, acao, recurso e IP"],
            ["Retencao", "Politica documentada; sem automacao de expurgo no MVP"],
        ],
        widths=[0.28, 0.72],
    )
    report.p(
        "A auditoria nao tem FK para users. O ator e gravado como snapshot para preservar a "
        "rastreabilidade mesmo se o usuario for desativado. Nao sao gravados senha, token ou payload "
        "completo de pagamento.",
    )

    report.h1("6 ENTREGA TECNICA E DOCUMENTACAO")
    report.p(
        "O projeto esta organizado em domain, application, infrastructure e api. O README da raiz "
        "explica requisitos, variaveis de ambiente, docker-compose, migrations, execucao, Swagger, "
        "usuarios de teste, testes automatizados e colecao Postman.",
    )
    report.table(
        "Evidencias de entrega",
        ["Evidencia", "Local"],
        [
            ["Repositorio publico", "URL informada no README"],
            ["Swagger UI", "http://localhost:8080/swagger-ui.html"],
            ["OpenAPI JSON", "http://localhost:8080/v3/api-docs"],
            ["Colecao Postman", "postman/rdn-api.postman_collection.json"],
            ["Environment Postman", "postman/rdn-api.postman_environment.json"],
            ["DER", "docs/DER.md"],
            ["Relatorios por etapa", "docs/etapa-1 a docs/etapa-5"],
            ["Relatorio final", "docs/projeto-back-end-final-abnt.pdf"],
        ],
        widths=[0.34, 0.66],
    )
    report.p(
        "O Docker nao esta instalado na maquina de desenvolvimento usada. O docker-compose.yml esta "
        "pronto e o SchemaDdlTest aplica V1 a V5 em banco limpo. A validacao em PostgreSQL real deve ser "
        "executada pelo avaliador conforme o README; essa limitacao esta declarada de forma explicita.",
    )

    report.h1("7 PLANO DE TESTES")
    report.p(
        "A suite automatizada possui 90 testes, incluindo 20 cenarios HTTP equivalentes aos da colecao "
        "Postman. Os testes de integracao sobem a aplicacao em porta aleatoria, autenticam com JWT real "
        "e exercitam os endpoints.",
    )
    report.table(
        "Cenarios da colecao Postman",
        ["ID", "Cenario", "Resultado esperado"],
        [
            ["T01", "Login do cliente", "200 e accessToken"],
            ["T02", "Login do admin", "200 e perfil ADMIN"],
            ["T03", "Listar promocoes", "200 com campanhas"],
            ["T04", "Listar unidades", "200 com unidade semeada"],
            ["T05", "Consultar cardapio", "200 com itens"],
            ["T06", "Criar pedido com promocao", "201 com desconto"],
            ["T07", "Pagar com mock aprovado", "201, APPROVED e RECEIVED"],
            ["T08", "Consultar pedido", "200 com itens"],
            ["T09", "Consultar fidelidade", "200 com pontos"],
            ["T10", "Consultar extrato", "200 com EARNED"],
            ["T11", "Resgatar pontos", "200 com desconto"],
            ["T12", "Consultar auditoria", "200 com ORDER_CREATED"],
            ["T13", "Acesso sem token", "401 NAO_AUTENTICADO"],
            ["T14", "Cliente listando pedidos", "403 ACESSO_NEGADO"],
            ["T15", "Pedido sem canal", "400 DADOS_INVALIDOS"],
            ["T16", "Estoque insuficiente", "409 ESTOQUE_INSUFICIENTE"],
            ["T17", "Promocao expirada", "409 PROMOCAO_NAO_APLICAVEL"],
            ["T18", "Resgate sem consentimento", "409 CONSENTIMENTO_NECESSARIO"],
            ["T19", "Query param invalido", "400 PARAMETRO_INVALIDO"],
            ["T20", "Replay de pagamento", "200 sem duplicar"],
        ],
        widths=[0.08, 0.42, 0.50],
    )
    report.p(
        "Alem dos cenarios HTTP, existem testes unitarios de dominio e casos de uso, o SchemaDdlTest "
        "para V1 a V5 e o OpenApiDocsTest para /v3/api-docs e /swagger-ui.html.",
    )

    report.h1("8 CONCLUSAO")
    report.p(
        "A solucao entrega o Fluxo A completo e testavel: pedido multicanal com estoque e promocao, "
        "pagamento mock idempotente, atualizacao de status, fidelidade com consentimento e auditoria. "
        "O DER, os contratos e os endpoints estao coerentes com o codigo, e a suite automatizada cobre "
        "cenarios positivos e negativos.",
    )
    report.p(
        "Ficaram como proposta o Fluxo B de estoque administrativo, refresh token/logout, CRUD "
        "administrativo de catalogo e automacao de retencao da auditoria. Essas escolhas priorizaram a "
        "qualidade do fluxo critico e a reprodutibilidade da entrega.",
    )

    report.h1("9 REFERENCIAS")
    report.references([
        "ASSOCIACAO BRASILEIRA DE NORMAS TECNICAS. NBR 14724: informacao e documentacao - trabalhos "
        "academicos - apresentacao. Rio de Janeiro: ABNT, 2011.",
        "ASSOCIACAO BRASILEIRA DE NORMAS TECNICAS. NBR 6023: referencias - elaboracao. Rio de Janeiro: "
        "ABNT, 2018.",
        "BRASIL. Lei no 13.709, de 14 de agosto de 2018. Lei Geral de Protecao de Dados Pessoais (LGPD). "
        "Brasilia, DF: Presidencia da Republica, 2018.",
        "OWASP FOUNDATION. Password storage cheat sheet. Disponivel em: "
        "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html. "
        "Acesso em: 16 nov. 2026.",
        "RED GATE. Flyway documentation. Disponivel em: https://documentation.red-gate.com/flyway. "
        "Acesso em: 16 nov. 2026.",
        "SPRING. Spring Boot reference documentation. Disponivel em: https://docs.spring.io/spring-boot/. "
        "Acesso em: 16 nov. 2026.",
    ])

    report.h1("10 DECLARACAO DE USO DE IA")
    report.p(
        "Ferramenta utilizada: assistente de IA integrado ao ambiente de desenvolvimento (Zoo Code, "
        "modelo DeepSeek), empregado como apoio de planejamento, implementacao e revisao.",
    )
    report.p(
        "Prompts principais: solicitacoes para planejar as etapas 5 e 6, estruturar migrations, casos de "
        "uso, testes, README, colecao Postman e relatorios ABNT; pedidos de revisao de regras de "
        "fidelidade, idempotencia e auditoria.",
    )
    report.p(
        "Trechos aproveitados: estrutura de camadas, esbocos de classes, migrations, testes e textos "
        "tecnicos. Todo o material foi revisado, ajustado e executado no projeto; os resultados dos "
        "testes e as limitacoes declaradas sao reais.",
    )

    report.h1("ANEXO A - CREDENCIAIS DE DESENVOLVIMENTO")
    report.table(
        "Usuarios semeados na V3 (somente desenvolvimento)",
        ["E-mail", "Senha", "Perfil"],
        [
            ["admin@rdn.local", "Admin@123", "ADMIN"],
            ["staff@rdn.local", "Staff@123", "STAFF"],
            ["kitchen@rdn.local", "Kitchen@123", "KITCHEN"],
            ["customer@rdn.local", "Customer@123", "CUSTOMER"],
        ],
        widths=[0.40, 0.30, 0.30],
    )
    report.p(
        "A entrega final deve ser renomeada para o padrao solicitado no roteiro: "
        "RU_Projeto_Back_End.PDF.",
    )

    return report.save()


if __name__ == "__main__":
    path = build()
    print("PDF gerado em %s" % path)
