#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio da Etapa 3 (API de pedidos) em PDF seguindo a NBR 14724.

Uso:
    python tools/abnt_pdf_etapa3.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from abnt_doc import AbntReport  # noqa: E402

OUTPUT = "docs/etapa-3-pedidos-abnt.pdf"


def build():
    report = AbntReport(
        OUTPUT,
        "RDN-API: API DE PEDIDOS E CONTROLE DE ESTOQUE",
        "Relat\u00f3rio t\u00e9cnico da Etapa 3: cria\u00e7\u00e3o, consulta e mudan\u00e7a de status",
    )
    report.cover().summary()

    report.h1("INTRODU\u00c7\u00c3O")
    report.p(
        "A Etapa 1 entregou a base persistente do Fluxo A e a Etapa 2 fechou a porta de entrada da "
        "API com autentica\u00e7\u00e3o e autoriza\u00e7\u00e3o por perfis. Faltava o elo central: expor o "
        "pedido. Esta etapa cria os endpoints de cria\u00e7\u00e3o, consulta e mudan\u00e7a de status, "
        "fazendo o estoque por unidade participar da decis\u00e3o e respeitando a m\u00e1quina de estados "
        "j\u00e1 existente no dom\u00ednio.",
    )
    report.p(
        "O escopo foi deliberadamente fechado em quatro rotas. O objetivo do roteiro \u00e9 ter um fluxo "
        "cr\u00edtico completo e test\u00e1vel, n\u00e3o um cat\u00e1logo extenso de endpoints sem regra de "
        "neg\u00f3cio. Cada rota tem contrato documentado, c\u00f3digos de status coerentes e erro padronizado "
        "herdado da Etapa 2.",
    )

    report.h1("ARQUITETURA E ESCOPO DA ETAPA 3")
    report.p(
        "A aplica\u00e7\u00e3o mant\u00e9m uma varia\u00e7\u00e3o de Ports and Adapters: a camada de aplica\u00e7\u00e3o "
        "declara as portas, a infraestrutura implementa com Spring Data e a camada de API traduz HTTP em "
        "comando. O dom\u00ednio continuou intocado \u2014 nenhuma anota\u00e7\u00e3o de persist\u00eancia foi "
        "adicionada a Order, OrderItem ou InventoryItem.",
    )
    report.table(
        "Artefatos produzidos na Etapa 3",
        ["Camada", "Responsabilidade", "Artefatos desta etapa"],
        [
            ["Application", "Casos de uso e portas", "OrderGateway, CatalogGateway, CustomerGateway, "
             "InventoryGateway, CreateOrderService, ChangeOrderStatusService, QueryOrderService"],
            ["Infrastructure", "Persist\u00eancia e lock", "OrderGatewayAdapter, CatalogGatewayAdapter, "
             "CustomerGatewayAdapter, InventoryGatewayAdapter"],
            ["API", "Contratos HTTP e erros", "OrderController, CreateOrderRequest, "
             "OrderResponse, OrderSummaryResponse, PageResponse"],
            ["Domain", "Regras invariantes", "Nenhuma altera\u00e7\u00e3o: changeStatus, cancel, "
             "deductStock e calculateTotal j\u00e1 existiam"],
        ],
        widths=[0.16, 0.24, 0.60],
    )

    report.h1("IMPLEMENTA\u00c7\u00c3O")
    report.h2("Portas de sa\u00edda da aplica\u00e7\u00e3o")
    report.p(
        "Quatro interfaces isolam a aplica\u00e7\u00e3o de detalhes de banco. OrderGateway persiste e "
        "consulta pedidos; CatalogGateway resolve unidade e item de card\u00e1pio; CustomerGateway localiza "
        "o cliente; InventoryGateway executa a baixa de estoque. Nenhum caso de uso importa jakarta.persistence.",
    )
    report.code(
        """
public interface InventoryGateway {
    ReservationResult reserve(UUID branchId, UUID menuItemId,
            BigDecimal quantity, String notes);

    record ReservationResult(boolean reserved, BigDecimal available) { }
}
""",
    )
    report.p(
        "A reserva \u00e9 uma opera\u00e7\u00e3o \u00fanica de prop\u00f3sito. Ler o saldo, aplicar a baixa e "
        "registrar a movimenta\u00e7\u00e3o precisam ocorrer na mesma transa\u00e7\u00e3o e sob lock; separar em "
        "tr\u00eas chamadas abriria espa\u00e7o para duas comandas venderem o mesmo saldo.",
    )

    report.h2("Cria\u00e7\u00e3o do pedido")
    report.p(
        "O caso de uso executa quatro passos: valida o cat\u00e1logo, resolve o cliente a partir do token, "
        "baixa o estoque e persiste o agregado. A transa\u00e7\u00e3o \u00e9 \u00fanica: se o segundo item falhar, "
        "a baixa do primeiro \u00e9 desfeita pelo rollback.",
    )
    report.code(
        """
@Transactional
public Order create(CreateOrderCommand command, AuthenticatedUser principal) {
    Branch branch = catalogGateway.findBranch(command.unidadeId())
            .orElseThrow(() -> new ResourceNotFoundException("Unidade", command.unidadeId()));

    Order order = new Order();
    order.setBranch(branch);
    order.setOriginChannel(command.canalPedido());

    Map<UUID, Integer> quantities = mergeQuantities(command.itens());
    for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
        MenuItem menuItem = catalogGateway.findMenuItem(entry.getKey())
                .orElseThrow(() -> new ResourceNotFoundException("Produto", entry.getKey()));
        order.addItem(menuItem, entry.getValue());
    }
    order.calculateTotal();

    for (Map.Entry<UUID, Integer> entry : quantities.entrySet()) {
        ReservationResult result = inventoryGateway.reserve(command.unidadeId(),
                entry.getKey(), BigDecimal.valueOf(entry.getValue()), notes);
        if (!result.reserved()) {
            throw new InsufficientStockException(entry.getKey(),
                    result.available(), BigDecimal.valueOf(entry.getValue()));
        }
    }
    ...
}
""",
    )
    report.p(
        "Dois detalhes merecem destaque. Primeiro, o total \u00e9 calculado pelo dom\u00ednio: o cliente n\u00e3o "
        "envia valor, o que elimina a possibilidade de pagar menos do que o pedido vale. Segundo, itens "
        "repetidos s\u00e3o agrupados por produto antes da valida\u00e7\u00e3o; sem isso duas linhas do mesmo "
        "item passariam isoladamente e a soma poderia estourar o saldo.",
    )

    report.h2("Baixa de estoque com lock pessimista")
    report.p(
        "A entidade de estoque n\u00e3o possui controle otimista (@Version). Para evitar venda acima do saldo, "
        "a leitura usada na baixa trava a linha at\u00e9 o fim da transa\u00e7\u00e3o. A solu\u00e7\u00e3o n\u00e3o "
        "exige altera\u00e7\u00e3o de schema.",
    )
    report.code(
        """
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from InventoryItemJpaEntity i "
        + "where i.branch.id = :branchId and i.menuItem.id = :menuItemId")
Optional<InventoryItemJpaEntity> findForUpdate(@Param("branchId") UUID branchId,
        @Param("menuItemId") UUID menuItemId);
""",
    )
    report.p(
        "O adaptador converte a linha em InventoryItem de dom\u00ednio, chama deductStock e, apenas quando o "
        "dom\u00ednio aceita, grava o saldo e cria um InventoryMovement do tipo OUTBOUND. A regra continua no "
        "dom\u00ednio; o adaptador apenas orquestra persist\u00eancia e auditoria.",
    )

    report.h2("Mudan\u00e7a de status e regra de dono")
    report.p(
        "A m\u00e1quina de estados n\u00e3o foi duplicada. O servi\u00e7o carrega o pedido, aplica "
        "changeStatus ou cancel e traduz IllegalStateException em conflito de neg\u00f3cio. O perfil CUSTOMER "
        "s\u00f3 pode cancelar o pr\u00f3prio pedido; qualquer outra combina\u00e7\u00e3o recebe 403.",
    )
    report.code(
        """
try {
    if (OrderStatus.CANCELLED == command.status()) {
        order.cancel(normalizeReason(command.motivo()));
    } else {
        order.changeStatus(command.status());
    }
} catch (IllegalStateException ex) {
    throw new InvalidStatusTransitionException(pedidoId, anterior, command.status());
}
""",
    )

    report.h2("Listagem paginada sem N+1")
    report.p(
        "A listagem devolve resumo, e n\u00e3o o agregado inteiro. Com open-in-view desligado, tocar na cole\u00e7\u00e3o "
        "de itens fora da transa\u00e7\u00e3o estouraria LazyInitializationException; al\u00e9m disso, paginar "
        "cole\u00e7\u00e3o com join fetch faria o Hibernate paginar em mem\u00f3ria. O detalhe do pedido, com itens, "
        "fica em GET /pedidos/{id}, que usa @EntityGraph.",
    )
    report.table(
        "Endpoints entregues na Etapa 3",
        ["M\u00e9todo/Rota", "Perfis", "Sucesso", "Falhas previstas"],
        [
            ["POST /pedidos", "CUSTOMER, STAFF, ADMIN", "201 com pedido e total",
             "400 valida\u00e7\u00e3o; 401; 403; 404 unidade/produto; 409 estoque"],
            ["GET /pedidos", "ADMIN, STAFF", "200 paginado com filtros de canal e status",
             "401; 403"],
            ["GET /pedidos/{id}", "Autenticado", "200 com itens",
             "401; 403 pedido de outro cliente; 404"],
            ["PATCH /pedidos/{id}/status", "KITCHEN, STAFF, ADMIN", "200 com novo status",
             "400; 401; 403; 404; 409 transi\u00e7\u00e3o inv\u00e1lida"],
        ],
        widths=[0.24, 0.22, 0.26, 0.28],
    )

    report.h2("Erros padronizados")
    report.p(
        "O corpo de erro continua \u00fanico: error, message, details, timestamp e path. A Etapa 3 apenas "
        "acrescentou c\u00f3digos est\u00e1veis ao cat\u00e1logo existente.",
    )
    report.table(
        "Novos c\u00f3digos de erro",
        ["Status", "C\u00f3digo", "Quando acontece"],
        [
            ["404", "RECURSO_NAO_ENCONTRADO", "Unidade, produto, cliente ou pedido inexistente"],
            ["409", "ESTOQUE_INSUFICIENTE", "Saldo da unidade menor que a quantidade pedida"],
            ["409", "TRANSICAO_INVALIDA", "M\u00e1quina de estados recusou a mudan\u00e7a"],
            ["403", "ACESSO_NEGADO", "Cliente tentando agir sobre pedido de outro cliente"],
        ],
        widths=[0.12, 0.30, 0.58],
    )

    report.h1("DECIS\u00d5ES E TRADE-OFFS")
    report.table(
        "Decis\u00f5es arquiteturais da Etapa 3",
        ["Decis\u00e3o", "Alternativa descartada", "Consequ\u00eancia aceita"],
        [
            ["Portas na aplica\u00e7\u00e3o", "Servi\u00e7o usando reposit\u00f3rio Spring Data direto",
             "Quatro interfaces e adaptadores a manter; camadas expl\u00edcitas"],
            ["Listagem com resumo", "Trazer itens na listagem", "Duas consultas quando o cliente quer o detalhe"],
            ["Autoriza\u00e7\u00e3o no SecurityConfig", "@PreAuthorize nos controllers",
             "Regra centralizada em um arquivo; controller menos declarativo"],
            ["Lock pessimista na baixa", "@Version na entidade de estoque",
             "Sem migration nova; escrita serializada na mesma linha"],
            ["Cliente vindo do token", "Confiar no clienteId do corpo",
             "Colaboradores precisam informar o cliente quando agem por ele"],
            ["Itens agrupados por produto", "Validar linha a linha",
             "Comportamento previs\u00edvel quando o mesmo item aparece duas vezes"],
        ],
        widths=[0.28, 0.34, 0.38],
    )

    report.h1("CONCEITOS FUNDAMENTAIS")
    report.bullets([
        "Agregado: Order comanda itens, total e status; a altera\u00e7\u00e3o passa pela raiz.",
        "Porta e adaptador: a aplica\u00e7\u00e3o declara a interface e a infraestrutura implementa.",
        "Lock pessimista: a linha fica travada at\u00e9 o commit, serializando a baixa de estoque.",
        "Idempot\u00eancia: repetir a mesma requisi\u00e7\u00e3o n\u00e3o duplica efeito.",
        "Pagina\u00e7\u00e3o: consulta com limite e deslocamento, devolvendo total de elementos.",
        "DTO de resumo: proje\u00e7\u00e3o enxuta que evita carregar cole\u00e7\u00f5es na listagem.",
        "Bean Validation: @Valid no corpo converte campo inv\u00e1lido em 400 com details.",
        "Transa\u00e7\u00e3o \u00fanica: qualquer falha desfaz pedido e baixa de estoque juntos.",
    ])

    report.h1("ERROS COMUNS E ARMADILHAS")
    report.bullets([
        "Aceitar o total vindo do cliente permite pagar menos do que o pedido vale; o total deve sair de calculateTotal.",
        "Validar estoque linha a linha, sem agrupar por produto, permite que duas linhas do mesmo item passem e estourem o saldo.",
        "Ler e baixar o estoque em chamadas separadas, sem lock, permite venda acima do saldo em concorr\u00eancia.",
        "Paginar cole\u00e7\u00e3o com join fetch faz o Hibernate carregar tudo em mem\u00f3ria e cortar depois.",
        "Tocar em entity.getItems() fora da transa\u00e7\u00e3o estoura LazyInitializationException com open-in-view desligado.",
        "Devolver 500 para transi\u00e7\u00e3o inv\u00e1lida esconde uma regra de neg\u00f3cio: o correto \u00e9 409.",
    ])

    report.h1("EXERC\u00cdCIOS DE FIXA\u00c7\u00c3O")
    report.p(
        "Exerc\u00edcio 1. Um pedido envia o mesmo produto em duas linhas, com quantidades 2 e 3, e a unidade "
        "tem 4 unidades em estoque. O que acontece e por qu\u00ea?",
    )
    report.p(
        "Gabarito: a resposta \u00e9 409 ESTOQUE_INSUFICIENTE. As linhas s\u00e3o agrupadas antes da valida\u00e7\u00e3o, "
        "formando uma necessidade de 5 contra um saldo de 4.",
    )
    report.p(
        "Exerc\u00edcio 2. Por que a listagem devolve OrderSummaryResponse em vez de OrderResponse?",
    )
    report.p(
        "Gabarito: para n\u00e3o carregar a cole\u00e7\u00e3o de itens em uma consulta paginada. Al\u00e9m de evitar N+1, "
        "evita a pagina\u00e7\u00e3o em mem\u00f3ria que o join fetch de cole\u00e7\u00e3o provocaria.",
    )
    report.p(
        "Exerc\u00edcio 3. Onde a regra de transi\u00e7\u00e3o de status est\u00e1 implementada e por que n\u00e3o no servi\u00e7o?",
    )
    report.p(
        "Gabarito: em Order.changeStatus, com a tabela ALLOWED_TRANSITIONS. No dom\u00ednio, nenhum caso de uso "
        "futuro consegue esquecer a regra; o servi\u00e7o apenas traduz a exce\u00e7\u00e3o em 409.",
    )

    report.h1("CHECKLIST DE AUTOAVALIA\u00c7\u00c3O")
    report.bullets([
        "Sei explicar por que o total nunca vem do request.",
        "Sei justificar o lock pessimista mesmo sem @Version na entidade.",
        "Sei dizer por que a listagem n\u00e3o devolve itens.",
        "Sei diferenciar 404 de 409 nos casos de produto e estoque.",
        "Sei explicar o rollback quando o segundo item n\u00e3o tem saldo.",
        "Sei apontar onde a autoriza\u00e7\u00e3o por perfil \u00e9 aplicada.",
        "Sei descrever o que acontece se o estoque da unidade n\u00e3o estiver cadastrado.",
    ])

    report.h1("CONSIDERA\u00c7\u00d5ES FINAIS E PR\u00d3XIMO PASSO")
    report.p(
        "A Etapa 3 fecha a API de pedidos com quatro rotas, estoque por unidade participando da decis\u00e3o, "
        "pagina\u00e7\u00e3o com filtros e erro padronizado. Foram acrescentados tr\u00eas testes de caso de uso, "
        "quatro de provedor simulado e quatorze cen\u00e1rios HTTP, todos verdes.",
    )
    report.p(
        "O passo seguinte \u00e9 o pagamento simulado, que fecha o fluxo cr\u00edtico exigido pelo roteiro e est\u00e1 "
        "documentado em relat\u00f3rio separado (Etapa 4), incluindo idempot\u00eancia e efeito no status.",
    )

    report.references([
        "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. NBR 14724: informa\u00e7\u00e3o e documenta\u00e7\u00e3o "
        "\u2014 trabalhos acad\u00eamicos \u2014 apresenta\u00e7\u00e3o. Rio de Janeiro: ABNT, 2011.",
        "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. NBR 6023: refer\u00eancias \u2014 elabora\u00e7\u00e3o. "
        "Rio de Janeiro: ABNT, 2018.",
        "EVANS, Eric. Domain-driven design: tackling complexity in the heart of software. Boston: "
        "Addison-Wesley, 2003.",
        "FIELDING, Roy Thomas. Architectural styles and the design of network-based software "
        "architectures. 2000. Tese (Doutorado) \u2014 University of California, Irvine, 2000.",
        "SPRING. Spring Data JPA reference documentation. Dispon\u00edvel em: "
        "https://docs.spring.io/spring-data/jpa/reference/. Acesso em: 13 nov. 2026.",
        "SPRING. Spring Security reference. Dispon\u00edvel em: "
        "https://docs.spring.io/spring-security/reference/. Acesso em: 13 nov. 2026.",
    ])

    return report.save()


if __name__ == "__main__":
    path = build()
    print("PDF gerado em %s" % path)
