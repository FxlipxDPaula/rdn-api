#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio final do projeto em PDF seguindo a NBR 14724.

Uso:
    python tools/render_diagrams.py
    python tools/abnt_pdf_final.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from abnt_doc import AbntReport  # noqa: E402

OUTPUT = "docs/projeto-back-end-final-abnt.pdf"
REPO_URL = "https://github.com/FxlipxDPaula/rdn-api"
FIG_DIR = os.path.join("docs", "diagrams")


def fig(name):
    return os.path.join(FIG_DIR, name)


def build():
    report = AbntReport(
        OUTPUT,
        "RDN-API: BACK-END MULTICANAL PARA REDE DE LANCHONETES",
        "Relatorio final do Projeto Multidisciplinar - Trilha Back-End",
    )
    report.cover().summary()

    report.h1("INTRODUCAO E OBJETIVOS")
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
        "cardapio, promocoes, fidelidade, auditoria e cadastro de usuarios restrito a administracao.",
    )
    report.p(
        "A API foi construida com Java 25, Spring Boot 4.1.2, Spring Data JPA, Spring Security com JWT, "
        "Flyway e PostgreSQL 16 como banco alvo. Os testes automatizados usam H2 em modo PostgreSQL para "
        "serem reproduziveis sem Docker.",
    )

    report.h1("ANALISE E REQUISITOS")
    report.h2("Requisitos funcionais")
    report.table(
        "Requisitos funcionais e cobertura",
        ["Requisito", "Implementacao", "Situacao"],
        [
            ["Cadastro de usuarios", "POST /usuarios restrito a ADMIN, com senha BCrypt", "Implementado"],
            ["Autenticacao com perfis", "JWT HS256 e perfis ADMIN, STAFF, KITCHEN e CUSTOMER", "Implementado"],
            ["Consulta de cardapio por unidade", "GET /unidades e GET /unidades/{id}/cardapio", "Implementado"],
            ["Gestao de unidades", "GET /unidades; CRUD administrativo fora do MVP", "Parcial"],
            ["Realizacao de pedidos", "POST /pedidos com canal, itens, promocao e calculo de total", "Implementado"],
            ["Atualizacao de status", "PATCH /pedidos/{id}/status com maquina de estados", "Implementado"],
            ["Cancelamento de pedido", "PATCH /pedidos/{id}/status para CANCELLED", "Implementado"],
            ["Controle de estoque", "Baixa por unidade com lock e recusa de saldo insuficiente", "Implementado no Fluxo A"],
            ["Pagamento externo mock", "POST /pedidos/{id}/pagamentos com idempotencia", "Implementado"],
            ["Fidelizacao com consentimento", "Opt-in/out, saldo, extrato, acumulo e resgate", "Implementado"],
            ["Promocoes e campanhas", "GET /promocoes e promocao opcional no pedido", "Implementado"],
            ["Auditoria de acoes sensiveis", "audit_logs e GET /auditoria restrito a ADMIN", "Implementado"],
            ["Multicanalidade", "canalPedido obrigatorio e filtro por canal", "Implementado"],
        ],
        widths=[0.26, 0.52, 0.22],
    )

    report.h2("Requisitos nao funcionais")
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

    report.h2("Multicanalidade")
    report.p(
        "O canal de origem e um dado de dominio. O pedido nasce com canalPedido obrigatorio, aceitando "
        "APP, TOTEM, BALCAO, PICKUP e WEB. A listagem operacional permite filtrar por canal e por status, "
        "o que sustenta a consolidacao por canal exigida pelo roteiro.",
    )

    report.h2("Priorizacao")
    report.p(
        "A prioridade foi fechar o Fluxo A ponta a ponta antes de ampliar o escopo. O Fluxo B, com "
        "endpoints administrativos de entrada e saida de estoque, nao foi implementado; a restricao de "
        "venda por saldo existe e e aplicada no pedido. Login social, refresh token, CRUD administrativo "
        "de cardapio e listagem/edicao de usuarios tambem ficaram fora do MVP.",
    )

    report.h1("DIAGRAMAS E MODELAGEM")
    report.h2("Casos de uso")
    report.p(
        "Atores: Cliente, Atendente, Cozinha, Administrador e Gateway de Pagamento. Os casos principais "
        "sao autenticar, consultar cardapio, criar pedido, solicitar pagamento, acompanhar status, "
        "cancelar pedido, consentir com fidelidade, resgatar pontos, consultar campanhas, cadastrar "
        "usuario e consultar auditoria.",
    )
    report.p(
        "Feature critica: Realizar pedido e solicitar pagamento. Pre-condicao: usuario autenticado e "
        "unidade aberta. Fluxo principal: cria o pedido, valida catalogo, aplica promocao opcional, baixa "
        "estoque, solicita o pagamento mock com chave de idempotencia e atualiza o status conforme o "
        "resultado. Excecoes: estoque insuficiente, produto inexistente, promocao invalida, chave "
        "reutilizada e pagamento recusado.",
    )
    report.figure(fig("01-casos-de-uso.png"),
                  "Casos de uso e atores do RDN-API",
                  "Fonte: elaborado pelo autor com base no roteiro (2026).")

    report.h2("DER e modelo de dados")
    report.p(
        "O modelo completo esta descrito em docs/DER.md. As entidades principais sao users, branches, "
        "menus, menu_items, inventory_items, inventory_movements, orders, order_items, payments, "
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
    report.figure(fig("02-der-nucleo.png"),
                  "DER: nucleo de pedidos, itens e pagamentos",
                  "Fonte: elaborado pelo autor a partir de V1 a V5 (2026).")
    report.figure(fig("03-der-apoio.png"),
                  "DER: catalogo, estoque, fidelidade e auditoria",
                  "Fonte: elaborado pelo autor a partir de V1 a V5 (2026).")

    report.h2("Classes e sequencia")
    report.p(
        "No dominio, Order concentra itens, total, desconto e transicoes de status. Promotion calcula "
        "vigencia e desconto. LoyaltyAccount controla consentimento e saldo. LoyaltyPolicy converte reais "
        "em pontos. Payment guarda a tentativa e o resultado do gateway. O dominio nao importa JPA.",
    )
    report.figure(fig("04-classes-dominio.png"),
                  "Classes de dominio, value objects e enums",
                  "Fonte: elaborado pelo autor a partir do codigo (2026).")
    report.p(
        "A sequencia do fluxo critico e: controller recebe o request, caso de uso valida catalogo e "
        "estoque, dominio calcula o total, OrderGateway persiste, PaymentController solicita a cobranca, "
        "RequestPaymentService aplica o resultado, o pedido muda de status e a auditoria registra o "
        "evento. A aprovacao ainda acumula pontos quando o cliente consentiu.",
    )
    report.figure(fig("05-sequencia-fluxo-a.png"),
                  "Sequencia do Fluxo A: pedido, pagamento mock e status",
                  "Fonte: elaborado pelo autor a partir do codigo (2026).")

    report.h1("API E ENDPOINTS")
    report.table(
        "Contratos principais",
        ["Metodo/Rota", "Perfis", "Sucesso", "Erros previstos"],
        [
            ["POST /auth/login", "publico", "200 com accessToken", "400, 401, 403"],
            ["GET /auth/me", "autenticado", "200 com usuario", "401"],
            ["POST /usuarios", "ADMIN", "201 com usuario", "400, 401, 403, 409"],
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
        "de regra de negocio. O contrato completo, com todos os schemas, esta no Swagger.",
    )

    report.h2("Contratos representativos")
    report.p("Login: POST /auth/login (publico).")
    report.code(
        """{
  "email": "customer@rdn.local",
  "senha": "Customer@123"
}

200 OK
{
  "accessToken": "jwt...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "00000000-0000-0000-0000-00000000000f",
    "fullName": "Cliente RDN",
    "email": "customer@rdn.local",
    "role": "CUSTOMER"
  }
}"""
    )

    report.p("Cadastro de usuario: POST /usuarios (header Authorization com token ADMIN).")
    report.code(
        """{
  "nome": "Maria Silva",
  "email": "maria@rdn.local",
  "senha": "Senha@123",
  "perfil": "CUSTOMER"
}

201 Created
{
  "usuarioId": "...",
  "nome": "Maria Silva",
  "email": "maria@rdn.local",
  "perfil": "CUSTOMER",
  "ativo": true
}"""
    )

    report.p("Criacao de pedido: POST /pedidos (JWT; CUSTOMER, STAFF ou ADMIN).")
    report.code(
        """{
  "canalPedido": "TOTEM",
  "unidadeId": "00000000-0000-0000-0000-000000000001",
  "formaPagamento": "MOCK",
  "promocaoId": "00000000-0000-0000-0000-0000000000c1",
  "itens": [
    { "produtoId": "00000000-0000-0000-0000-000000000003", "quantidade": 2 }
  ]
}

201 Created
{
  "pedidoId": "...",
  "canalPedido": "TOTEM",
  "status": "AGUARDANDO_PAGAMENTO",
  "total": 46.62,
  "desconto": 5.18,
  "itens": [
    { "produtoId": "...", "quantidade": 2, "precoUnitario": 25.90, "subtotal": 51.80 }
  ],
  "criadoEm": "2026-11-16T12:00:00"
}"""
    )

    report.p(
        "Filtro por canal e status: "
        "GET /pedidos?canalPedido=TOTEM&status=AGUARDANDO_PAGAMENTO&page=0&limit=10"
    )
    report.code(
        """200 OK
{
  "content": [
    { "pedidoId": "...", "canalPedido": "TOTEM", "status": "AGUARDANDO_PAGAMENTO", "total": 46.62 }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}"""
    )

    report.p(
        "Pagamento mock: POST /pedidos/{id}/pagamentos com header Idempotency-Key. "
        "A mesma chave devolve 200 com a tentativa original."
    )
    report.code(
        """{ "formaPagamento": "MOCK", "simular": "APROVADO" }

201 Created
{
  "pagamentoId": "...",
  "pedidoId": "...",
  "status": "APPROVED",
  "pedidoStatus": "RECEIVED",
  "transacaoGateway": "MOCK-..."
}"""
    )

    report.p("Erro padrao 409 para estoque insuficiente.")
    report.code(
        """{
  "error": "ESTOQUE_INSUFICIENTE",
  "message": "Nao ha quantidade suficiente para um ou mais itens.",
  "details": [
    { "field": "itens[produtoId=...].quantidade", "issue": "Disponivel: 1; solicitado: 3" }
  ],
  "timestamp": "2026-11-16T12:00:00Z",
  "path": "/pedidos"
}"""
    )

    report.h1("LGPD, PRIVACIDADE E SEGURANCA")
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
            ["Cadastro restrito", "POST /usuarios exige perfil ADMIN"],
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

    report.h1("ENTREGA TECNICA E DOCUMENTACAO")
    report.p(
        "O projeto esta organizado em domain, application, infrastructure e api. O README da raiz "
        "explica requisitos, variaveis de ambiente, docker-compose, migrations, execucao, Swagger, "
        "usuarios de teste, testes automatizados, colecao Postman e como regenerar diagramas e PDF.",
    )
    report.table(
        "Evidencias de entrega",
        ["Evidencia", "Local"],
        [
            ["Repositorio publico", REPO_URL],
            ["Swagger UI", "http://localhost:8080/swagger-ui.html"],
            ["OpenAPI JSON", "http://localhost:8080/v3/api-docs"],
            ["Colecao Postman", "postman/rdn-api.postman_collection.json"],
            ["Environment Postman", "postman/rdn-api.postman_environment.json"],
            ["DER", "docs/DER.md"],
            ["Diagramas", "docs/diagrams/*.png e tools/render_diagrams.py"],
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

    report.h1("PLANO DE TESTES")
    report.p(
        "A suite automatizada cobre dominio, casos de uso e HTTP real com JWT. A colecao Postman possui "
        "25 cenarios (T01 a T25), dos quais 13 sao positivos e 12 sao negativos. Os helpers T02b, T11a, "
        "T18a e T21a preparam estado e nao entram na contagem.",
    )
    report.table(
        "Cenarios positivos da colecao Postman",
        ["ID", "Endpoint", "Pre-condicao", "Entrada", "Esperado", "Evidencia"],
        [
            ["T01", "POST /auth/login", "cliente semeado", "email e senha", "200 e accessToken", "01-Auth/T01"],
            ["T02", "POST /auth/login", "admin semeado", "email e senha", "200 e role ADMIN", "01-Auth/T02"],
            ["T03", "GET /promocoes", "token", "-", "200 com campanhas", "06-Promocoes/T03"],
            ["T04", "GET /unidades", "token", "-", "200 com unidade", "02-Catalogo/T04"],
            ["T05", "GET /unidades/{id}/cardapio", "token e unidade", "path id", "200 com itens", "02-Catalogo/T05"],
            ["T06", "POST /pedidos", "token e seed", "canal, itens e promocao", "201 com desconto", "03-Pedidos/T06"],
            ["T07", "POST /pedidos/{id}/pagamentos", "T06 e Idempotency-Key", "MOCK e APROVADO", "201 APPROVED e RECEIVED", "04-Pagamento/T07"],
            ["T08", "GET /pedidos/{id}", "T06", "path id", "200 com itens", "03-Pedidos/T08"],
            ["T09", "GET /fidelidade", "token e T07", "-", "200 com saldo", "05-Fidelidade/T09"],
            ["T10", "GET /fidelidade/transacoes", "T07", "-", "200 com EARNED", "05-Fidelidade/T10"],
            ["T11", "POST /fidelidade/resgates", "T11a e consentimento", "pedidoId e pontos", "200 com desconto", "05-Fidelidade/T11"],
            ["T12", "GET /auditoria", "token ADMIN", "acao=ORDER_CREATED", "200 com ORDER_CREATED", "07-Auditoria/T12"],
            ["T23", "POST /usuarios", "token ADMIN", "nome, email, senha e perfil", "201 sem passwordHash", "08-Erros/T23"],
        ],
        widths=[0.06, 0.20, 0.18, 0.18, 0.19, 0.19],
        compact=True,
        source="Fonte: colecao Postman do projeto (2026).",
    )
    report.table(
        "Cenarios negativos da colecao Postman",
        ["ID", "Endpoint", "Pre-condicao", "Entrada", "Esperado", "Evidencia"],
        [
            ["T13", "GET /pedidos", "sem token", "-", "401 NAO_AUTENTICADO", "01-Auth/T13"],
            ["T14", "GET /pedidos", "token CUSTOMER", "-", "403 ACESSO_NEGADO", "03-Pedidos/T14"],
            ["T15", "POST /pedidos", "token", "sem canalPedido", "400 DADOS_INVALIDOS", "03-Pedidos/T15"],
            ["T16", "POST /pedidos", "token", "quantidade acima do saldo", "409 ESTOQUE_INSUFICIENTE", "03-Pedidos/T16"],
            ["T17", "POST /pedidos", "token e promocao expirada", "promocaoId expirado", "409 PROMOCAO_NAO_APLICAVEL", "03-Pedidos/T17"],
            ["T18", "POST /fidelidade/resgates", "T18a com opt-out", "pedidoId e pontos", "409 CONSENTIMENTO_NECESSARIO", "05-Fidelidade/T18"],
            ["T19", "GET /pedidos?canalPedido=FOO", "token ADMIN", "query invalida", "400 PARAMETRO_INVALIDO", "03-Pedidos/T19"],
            ["T20", "POST /pedidos/{id}/pagamentos", "T07 e mesma chave", "MOCK e APROVADO", "200 sem duplicar", "04-Pagamento/T20"],
            ["T21", "POST /pedidos/{id}/pagamentos", "T21a e GUID", "MOCK e RECUSADO", "201 DECLINED e CANCELLED", "08-Erros/T21"],
            ["T22", "POST /pedidos", "token", "produtoId inexistente", "404 RECURSO_NAO_ENCONTRADO", "08-Erros/T22"],
            ["T24", "POST /usuarios", "token CUSTOMER", "corpo valido", "403 ACESSO_NEGADO", "08-Erros/T24"],
            ["T25", "POST /usuarios", "T23 e token ADMIN", "mesmo email de T23", "409 EMAIL_JA_CADASTRADO", "08-Erros/T25"],
        ],
        widths=[0.06, 0.20, 0.18, 0.18, 0.19, 0.19],
        compact=True,
        source="Fonte: colecao Postman do projeto (2026).",
    )
    report.p(
        "A suite automatizada inclui testes unitarios de dominio e casos de uso, o SchemaDdlTest para "
        "V1 a V5, o OpenApiDocsTest para /v3/api-docs e /swagger-ui.html e os testes HTTP de pedido, "
        "pagamento, fidelidade e cadastro de usuario.",
    )

    report.h1("CONCLUSAO")
    report.p(
        "A solucao entrega o Fluxo A completo e testavel: pedido multicanal com estoque e promocao, "
        "pagamento mock idempotente, atualizacao de status, fidelidade com consentimento, auditoria e "
        "cadastro de usuarios restrito a administracao. O DER, os diagramas, os contratos e os endpoints "
        "estao coerentes com o codigo, e a suite automatizada cobre cenarios positivos e negativos.",
    )
    report.p(
        "Ficaram como proposta o Fluxo B de estoque administrativo, refresh token/logout, CRUD "
        "administrativo de catalogo, listagem/edicao/desativacao de usuarios e automacao de retencao da "
        "auditoria. Essas escolhas priorizaram a qualidade do fluxo critico e a reprodutibilidade da "
        "entrega.",
    )

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

    report.h1("DECLARACAO DE USO DE IA")
    report.p(
        "Ferramenta utilizada: assistente de IA integrado ao ambiente de desenvolvimento (Zoo Code, "
        "modelo DeepSeek), empregado como apoio de planejamento, implementacao e revisao.",
    )
    report.p(
        "Prompts principais: solicitacoes para planejar as etapas 5 e 6, estruturar migrations, casos de "
        "uso, testes, README, colecao Postman, diagramas e relatorios ABNT; pedidos de revisao de "
        "fidelidade, idempotencia, auditoria e do endpoint de cadastro de usuarios.",
    )
    report.p(
        "Trechos aproveitados: estrutura de camadas, esbocos de classes, migrations, testes, diagramas e "
        "textos tecnicos. Todo o material foi revisado, ajustado e executado no projeto; os resultados "
        "dos testes e as limitacoes declaradas sao reais.",
    )

    report.h1("ANEXO A - CREDENCIAIS DE DESENVOLVIMENTO", numbered=False)
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
        "Novos usuarios podem ser criados por POST /usuarios com o token do ADMIN. "
        "A entrega final deve ser renomeada para o padrao solicitado no roteiro: "
        "RU_Projeto_Back_End.PDF.",
    )

    return report.save()


if __name__ == "__main__":
    path = build()
    print("PDF gerado em %s" % path)
