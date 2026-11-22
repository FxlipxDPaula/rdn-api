#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio da Etapa 1 (persistencia do Fluxo A) em PDF ABNT.

Reutiliza estilos, helpers e a classe AbntDoc de tools/abnt_pdf.py, garantindo
que a Etapa 1 e a Etapa 2 tenham exatamente o mesmo padrao visual.

Uso:
    python tools/abnt_pdf_etapa1.py
"""

from abnt_pdf import (
    AUTHOR,
    YEAR,
    AbntDoc,
    COVER_AUTHOR,
    COVER_BOTTOM,
    COVER_SUBTITLE,
    COVER_TITLE,
    COVER_TOP,
    REFERENCE,
    centered_section,
    code,
    item,
    mono,
    p,
    section,
    subsection,
    summary,
    table,
)
from reportlab.lib.units import cm
from reportlab.platypus import PageBreak, Spacer

OUTPUT = "documentacao/docs/etapa-1-persistencia-abnt.pdf"


# ---------------------------------------------------------------------- capa

def cover():
    return [
        Spacer(1, 0.4 * cm),
        p("PROJETO MULTIDISCIPLINAR", COVER_TOP),
        p("TRILHA BACK-END", COVER_TOP),
        p(AUTHOR, COVER_AUTHOR),
        Spacer(1, 4.6 * cm),
        p("RDN-API: PERSIST\u00caNCIA DO FLUXO DE PEDIDO", COVER_TITLE),
        p("Relat\u00f3rio t\u00e9cnico da Etapa 1: dom\u00ednio, JPA, Flyway e testes de integra\u00e7\u00e3o",
          COVER_SUBTITLE),
        Spacer(1, 7.2 * cm),
        p("Caxias do Sul - RS", COVER_BOTTOM),
        p(YEAR, COVER_BOTTOM),
        PageBreak(),
    ]


# -------------------------------------------------------------------- conteudo

def introduction():
    return [
        section("1", "Introdu\u00e7\u00e3o"),
        p(
            "O projeto rdn-api \u00e9 o back-end de uma rede de lanchonetes com atendimento em "
            "m\u00faltiplos canais: aplicativo, totem, balc\u00e3o, pickup e web. A primeira etapa "
            "concentrou-se em transformar o modelo de dom\u00ednio em um banco de dados real, "
            "versionado e verific\u00e1vel: schema criado por migra\u00e7\u00f5es Flyway, entidades JPA "
            "separadas do dom\u00ednio, reposit\u00f3rios Spring Data e um teste de integra\u00e7\u00e3o que "
            "grava e rel\u00ea um pedido completo."
        ),
        p(
            "A prioridade da etapa foi a persist\u00eancia do Fluxo A (pedido, pagamento simulado e "
            "atualiza\u00e7\u00e3o de status), recomendado pelo roteiro da disciplina. Sem banco "
            "consistente n\u00e3o h\u00e1 como implementar casos de uso ou endpoints confi\u00e1veis; por "
            "isso, ao final da Etapa 1 o sistema persistia dados, mas ainda n\u00e3o expunha API. As "
            "camadas de aplica\u00e7\u00e3o e de interface foram deliberadamente adiadas."
        ),
        p(
            "O relat\u00f3rio descreve a arquitetura adotada (Se\u00e7\u00e3o 2), detalha os principais "
            "artefatos com trechos de c\u00f3digo (Se\u00e7\u00e3o 3), consolida decis\u00f5es e trade-offs "
            "(Se\u00e7\u00e3o 4) e encerra com conceitos, erros comuns, exerc\u00edcios, checklist e o "
            "encaminhamento para a etapa seguinte (Se\u00e7\u00f5es 5 a 9)."
        ),
    ]


def architecture():
    return [
        section("2", "Arquitetura e escopo da Etapa 1"),
        p(
            "O dom\u00ednio permaneceu puro: as classes de " + mono("domain") + " n\u00e3o possuem "
            "anota\u00e7\u00f5es de persist\u00eancia e n\u00e3o importam " + mono("jakarta.persistence") + ". "
            "O mapeamento relacional vive em " + mono("infrastructure/persistence/entity") + ", "
            "com convers\u00e3o feita por mappers e acesso por reposit\u00f3rios Spring Data. A "
            "consequ\u00eancia aceita \u00e9 manter duas representa\u00e7\u00f5es (dom\u00ednio e entidade) e mais "
            "classes; o benef\u00edcio \u00e9 preservar invariantes como " + mono("Address") + " imut\u00e1vel."
        ),
        *table(
            "1",
            "Situa\u00e7\u00e3o das camadas ao final da Etapa 1",
            ["Camada", "Situa\u00e7\u00e3o"],
            [
                [
                    "Domain",
                    "Pronta: POJOs com regras de pedido, pagamento, estoque, fidelidade e promo\u00e7\u00e3o",
                ],
                [
                    "Infrastructure",
                    "Pronta: 13 entidades JPA, 13 mappers, 13 reposit\u00f3rios, migrations V1 e V2 e DER",
                ],
                [
                    "Application",
                    "Ausente: nenhum caso de uso (criar pedido, pagar, mudar status) implementado",
                ],
                [
                    "API",
                    "Ausente: nenhum controller, DTO ou tratamento HTTP",
                ],
            ],
            [4.0 * cm, 12.0 * cm],
        ),
        p(
            "O schema \u00e9 criado exclusivamente pelo Flyway. O Hibernate opera com "
            + mono("ddl-auto=validate") + ", ou seja, apenas confere se o mapeamento bate com o "
            "DDL e falha na inicializa\u00e7\u00e3o em caso de diverg\u00eancia. As migrations "
            + mono("V1__schema.sql") + " e " + mono("V2__seed.sql") + " s\u00e3o imut\u00e1veis depois de "
            "aplicadas, o que preserva o checksum e a reprodutibilidade do banco."
        ),
        *table(
            "2",
            "Artefatos produzidos na Etapa 1",
            ["Artefato", "Quantidade e observa\u00e7\u00e3o"],
            [
                ["Entidades JPA", "13, uma por tabela, mais AddressEmbeddable"],
                ["Mappers", "13, com m\u00e9todos toDomain e copyScalarsToEntity"],
                ["Reposit\u00f3rios", "13 interfaces Spring Data, com consultas derivadas e @EntityGraph"],
                ["Migrations", "2: V1 cria o schema; V2 insere filial, card\u00e1pio, 3 itens e estoque"],
                ["Testes", "7 casos: 5 de persist\u00eancia, 1 de contexto e 1 de DDL/seed"],
                ["Documenta\u00e7\u00e3o", "DER em Mermaid e plano de execu\u00e7\u00e3o da etapa"],
            ],
            [5.0 * cm, 11.0 * cm],
        ),
    ]


def order_domain():
    return [
        section("3", "Modelagem e implementa\u00e7\u00e3o"),
        subsection("3.1", "Order: o agregado do pedido"),
        p(
            "A classe " + mono("Order") + " \u00e9 a raiz do agregado: quem manipula itens, total, "
            "status e cancelamento passa por ela. N\u00e3o conhece JPA, HTTP nem banco; depende "
            "apenas de " + mono("OrderItem") + ", " + mono("MenuItem") + ", " + mono("User") + ", "
            + mono("Branch") + " e dos enums de status e canal. As regras ficam na entidade, e n\u00e3o "
            "em um service, para que nenhum fluxo futuro as esque\u00e7a (EVANS, 2003)."
        ),
        code(
            """
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS =
        Map.of(
                OrderStatus.RECEIVED,
                Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
                OrderStatus.PREPARING,
                Set.of(OrderStatus.READY, OrderStatus.CANCELLED),
                OrderStatus.READY,
                Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
                OrderStatus.DELIVERED, Set.of(),
                OrderStatus.CANCELLED, Set.of());
"""
        ),
        item("A tabela \u00e9 " + mono("static final") + ": a regra \u00e9 a mesma para todo pedido, n\u00e3o \u00e9 estado da inst\u00e2ncia."),
        item(
            mono("Map.of") + " e " + mono("Set.of") + " criam cole\u00e7\u00f5es imut\u00e1veis; qualquer "
            "tentativa de altera\u00e7\u00e3o em runtime lan\u00e7a exce\u00e7\u00e3o, protegendo a regra."
        ),
        item(
            mono("Set.of()") + " vazio significa estado final: " + mono("DELIVERED") + " e "
            + mono("CANCELLED") + " n\u00e3o t\u00eam sa\u00edda."
        ),
        code(
            """
public BigDecimal calculateTotal() {
    BigDecimal sum = BigDecimal.ZERO;
    for (OrderItem orderItem : items) {
        sum = sum.add(orderItem.calculateSubtotal());
    }

    BigDecimal discount = discountAmount == null
            ? BigDecimal.ZERO : discountAmount;
    if (discount.signum() < 0) {
        discount = BigDecimal.ZERO;
    }
    if (discount.compareTo(sum) > 0) {
        discount = sum;
    }

    totalAmount = sum.subtract(discount);
    return totalAmount;
}
"""
        ),
        item(
            mono("BigDecimal") + " \u00e9 imut\u00e1vel: " + mono("sum.add(...)") + " devolve um novo "
            "objeto, por isso a reatribui\u00e7\u00e3o \u00e9 obrigat\u00f3ria."
        ),
        item(
            "A guarda contra " + mono("null") + " existe porque o campo \u00e9 um wrapper; "
            + mono("double") + " n\u00e3o aceitaria nulo, mas tamb\u00e9m n\u00e3o serve para dinheiro."
        ),
        item(
            "A compara\u00e7\u00e3o usa " + mono("compareTo") + " e n\u00e3o operador: "
            + mono("BigDecimal") + " com escalas diferentes pode ser diferente em "
            + mono("equals") + ". Limitar o desconto \u00e0 soma garante total n\u00e3o negativo, o que "
            "tamb\u00e9m protege a constraint " + mono("ck_orders_total") + "."
        ),
        code(
            """
public void changeStatus(OrderStatus newStatus) {
    if (newStatus == null) {
        throw new IllegalArgumentException("status e obrigatorio");
    }

    if (status == null) {
        if (OrderStatus.RECEIVED != newStatus) {
            throw new IllegalStateException("pedido deve iniciar em RECEIVED");
        }
        this.status = newStatus;
        return;
    }

    if (!ALLOWED_TRANSITIONS.getOrDefault(status, Set.of())
            .contains(newStatus)) {
        throw new IllegalStateException(
                "transicao de " + status + " para " + newStatus
                        + " nao permitida");
    }

    this.status = newStatus;
}
"""
        ),
        item(
            mono("IllegalArgumentException") + " indica entrada inv\u00e1lida do chamador; "
            + mono("IllegalStateException") + " indica estado atual incompat\u00edvel. A distin\u00e7\u00e3o "
            "permite \u00e0 API traduzir a primeira em 400/422 e a segunda em 409."
        ),
        item(
            "Pedido rec\u00e9m-criado s\u00f3 pode nascer em " + mono("RECEIVED") + ". Sem esse bloqueio, "
            "seria poss\u00edvel criar um pedido j\u00e1 entregue."
        ),
        item(
            mono("getOrDefault(status, Set.of())") + " evita " + mono("NullPointerException")
            + " caso apare\u00e7a um status sem regra cadastrada; nesse caso, a transi\u00e7\u00e3o \u00e9 negada."
        ),
        code(
            """
public void cancel(String reason) {
    changeStatus(OrderStatus.CANCELLED);
    this.cancellationReason = reason;
}
"""
        ),
        item("Delegar impede duplicar a regra: cancelar um pedido j\u00e1 entregue falha."),
        item("O motivo s\u00f3 \u00e9 gravado depois da valida\u00e7\u00e3o; se a transi\u00e7\u00e3o for negada, nada muda."),
        p(
            "Padr\u00f5es presentes: m\u00e1quina de estados declarativa, guard clauses, modelo de "
            "dom\u00ednio rico e encapsulamento. O ponto de aten\u00e7\u00e3o \u00e9 o "
            + mono("setStatus") + " p\u00fablico, mantido apenas para a reidrata\u00e7\u00e3o feita pelos "
            "mappers."
        ),
    ]


def payment_domain():
    return [
        subsection("3.2", "Payment: a tentativa de pagamento"),
        p(
            "A classe representa uma tentativa de cobran\u00e7a no provedor simulado, e n\u00e3o o "
            "pagamento definitivo do pedido. Isso permite hist\u00f3rico: um pedido pode ter uma "
            "tentativa recusada e outra aprovada. O estado inicial \u00e9 " + mono("PENDING") + ", e o "
            "objeto n\u00e3o conhece JSON, HTTP nem o gateway."
        ),
        code(
            """
public Payment(Order order, BigDecimal amount,
        PaymentMethod method, String idempotencyKey) {
    if (amount == null || amount.signum() <= 0) {
        throw new IllegalArgumentException("amount deve ser positivo");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        throw new IllegalArgumentException(
                "idempotencyKey e obrigatoria");
    }

    this.order = order;
    this.amount = amount;
    this.method = method;
    this.idempotencyKey = idempotencyKey.trim();
    this.status = PaymentStatus.PENDING;
    this.requestedAt = LocalDateTime.now();
}
"""
        ),
        item(
            mono("signum()") + " devolve -1, 0 ou 1: forma idiom\u00e1tica de testar o sinal sem "
            "aritm\u00e9tica."
        ),
        item(
            mono("isBlank()") + " cobre string vazia e apenas espa\u00e7os; "
            + mono("isEmpty()") + " aceitaria " + mono("\"   \"") + "."
        ),
        item(
            mono("trim()") + " normaliza a chave antes de gravar, porque a coluna \u00e9 "
            + mono("UNIQUE") + ": " + mono("\"abc\"") + " e " + mono("\"abc \"") + " precisam colidir, "
            "sen\u00e3o a idempot\u00eancia vaza."
        ),
        code(
            """
public void markApproved(String transactionId, String rawResponse) {
    requirePending();
    this.status = PaymentStatus.APPROVED;
    this.gatewayTransactionId = transactionId;
    this.gatewayRawResponse = rawResponse;
    this.processedAt = LocalDateTime.now();
}
"""
        ),
        item(
            "A guarda centralizada impede transformar " + mono("APPROVED") + " em "
            + mono("DECLINED") + " caso o webhook chegue duas vezes."
        ),
        item(
            "Guardar o payload bruto \u00e9 requisito de auditoria: sem ele, n\u00e3o h\u00e1 como investigar "
            "diverg\u00eancia de valor depois."
        ),
        item(
            mono("processedAt") + " s\u00f3 \u00e9 preenchido quando h\u00e1 resposta do provedor; a coluna "
            "\u00e9 nullable exatamente por isso."
        ),
        code(
            """
private void requirePending() {
    if (status != null && PaymentStatus.PENDING != status) {
        throw new IllegalStateException(
                "pagamento ja finalizado com status " + status
                        + " e nao pode ser alterado");
    }
}
"""
        ),
        p(
            "A checagem " + mono("status != null") + " protege contra objeto reidratado sem status "
            "(mapeador ou banco legado). Nesse caso, permitir a transi\u00e7\u00e3o \u00e9 mais seguro do "
            "que estourar " + mono("NullPointerException") + ". Os padr\u00f5es aplicados s\u00e3o "
            "construtor com valida\u00e7\u00e3o, guard clause compartilhada e estado expl\u00edcito."
        ),
    ]


def jpa_entity():
    return [
        subsection("3.3", "OrderJpaEntity: a tradu\u00e7\u00e3o relacional"),
        p(
            "A entidade existe porque " + mono("Order") + " n\u00e3o tem anota\u00e7\u00e3o JPA. \u00c9 ela que "
            "declara " + mono("@Entity") + ", " + mono("@Table") + ", " + mono("@Column") + " e as "
            "associa\u00e7\u00f5es; quem depende dela \u00e9 o reposit\u00f3rio e o mapper."
        ),
        *table(
            "3",
            "Anota\u00e7\u00f5es JPA e seus efeitos",
            ["Anota\u00e7\u00e3o", "O que faz", "Por que est\u00e1 aqui"],
            [
                [
                    "@Entity e @Table(name = \"orders\")",
                    "Liga a classe \u00e0 tabela",
                    "Nome expl\u00edcito evita depender da conven\u00e7\u00e3o",
                ],
                [
                    "@GeneratedValue(strategy = UUID)",
                    "Gera o identificador no Java",
                    "Funciona em PostgreSQL e H2, sem sequence nem fun\u00e7\u00e3o do banco",
                ],
                [
                    "@Enumerated(EnumType.STRING)",
                    "Salva o nome do enum",
                    "ORDINAL gravaria 0,1,2 e quebraria se a ordem do enum mudasse",
                ],
                [
                    "@ManyToOne(fetch = LAZY)",
                    "Carrega a associa\u00e7\u00e3o sob demanda",
                    "Evita buscar cliente e filial em toda listagem",
                ],
                [
                    "@OneToMany(mappedBy, cascade, orphanRemoval)",
                    "Um-para-muitos pelo lado inverso",
                    "A FK est\u00e1 em order_items; a cascata salva os itens junto",
                ],
                [
                    "precision = 10, scale = 2",
                    "Declara NUMERIC(10,2)",
                    "Sem isso, ddl-auto=validate acusa diverg\u00eancia",
                ],
            ],
            [5.2 * cm, 4.4 * cm, 6.4 * cm],
        ),
        code(
            """
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")
private UserJpaEntity customer;

@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "branch_id", nullable = false)
private BranchJpaEntity branch;
"""
        ),
        item("Sem " + mono("optional = false") + ", o cliente \u00e9 opcional: pedido de totem pode n\u00e3o ter usu\u00e1rio identificado."),
        item("A filial \u00e9 obrigat\u00f3ria; pedido sem unidade n\u00e3o existe no dom\u00ednio da rede."),
        code(
            """
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
        orphanRemoval = true, fetch = FetchType.LAZY)
private List<OrderItemJpaEntity> items = new ArrayList<>();
"""
        ),
        p(
            "Tr\u00eas decis\u00f5es em uma declara\u00e7\u00e3o: inicializar com " + mono("new ArrayList<>()")
            + " evita " + mono("NullPointerException") + " no " + mono("add") + " antes do "
            "carregamento; " + mono("cascade = ALL") + " faz " + mono("save(order)") + " salvar os "
            "itens; " + mono("orphanRemoval = true") + " remove do banco o item retirado da lista. "
            "Sem a \u00faltima, " + mono("order.getItems().remove(x)") + " n\u00e3o geraria "
            + mono("DELETE") + "."
        ),
    ]


def schema():
    return [
        subsection("3.4", "V1__schema.sql: o contrato do banco"),
        p(
            "A migration \u00e9 a fonte de verdade do schema: o Hibernate apenas valida, nunca cria. "
            "Como " + mono("V1") + " roda uma \u00fanica vez no hist\u00f3rico do Flyway, o arquivo n\u00e3o "
            "pode ser editado depois de aplicado."
        ),
        code(
            """
CONSTRAINT pk_orders PRIMARY KEY (id),
CONSTRAINT uk_orders_idempotency_key UNIQUE (idempotency_key),
CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id)
        REFERENCES users (id),
CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id),
CONSTRAINT ck_orders_channel CHECK (origin_channel IN
        ('APP', 'TOTEM', 'BALCAO', 'PICKUP', 'WEB')),
CONSTRAINT ck_orders_status CHECK (status IN
        ('RECEIVED', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')),
CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
CONSTRAINT ck_orders_discount CHECK (discount_amount >= 0)
"""
        ),
        item(
            mono("UNIQUE") + " em coluna nula \u00e9 permitido no SQL: v\u00e1rios pedidos podem ter "
            "chave nula, e a unicidade vale apenas para valores preenchidos."
        ),
        item(
            "Repetir o enum no " + mono("CHECK") + " \u00e9 redund\u00e2ncia intencional: protege "
            "escritas diretas no banco que n\u00e3o passaram pelo Java."
        ),
        item(
            "O " + mono("CHECK") + " de total \u00e9 a \u00faltima defesa do invariante que "
            + mono("calculateTotal()") + " j\u00e1 garante."
        ),
        code(
            """
CONSTRAINT fk_order_items_order FOREIGN KEY (order_id)
        REFERENCES orders (id) ON DELETE CASCADE,
CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
CONSTRAINT ck_order_items_unit_price CHECK (unit_price >= 0),
CONSTRAINT ck_order_items_subtotal CHECK (subtotal >= 0)
"""
        ),
        item(
            mono("ON DELETE CASCADE") + " no banco e " + mono("orphanRemoval") + " no JPA cobrem "
            "caminhos diferentes: o JPA apaga quando o objeto sai da lista; o banco apaga quando o "
            "pedido \u00e9 removido por SQL."
        ),
        item(
            mono("quantity > 0") + " espelha a valida\u00e7\u00e3o de " + mono("OrderItem") + ": item com "
            "quantidade zero n\u00e3o tem sentido cont\u00e1bil."
        ),
        code(
            """
CREATE TABLE payments (
    ...
    idempotency_key VARCHAR(80) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    CONSTRAINT uk_payments_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_payments_amount CHECK (amount > 0)
);
"""
        ),
        item(
            "Em " + mono("payments") + " a chave \u00e9 " + mono("NOT NULL") + ", porque toda tentativa de "
            "cobran\u00e7a precisa ser idempotente: repetir a chamada ao gateway n\u00e3o pode gerar duas "
            "cobran\u00e7as."
        ),
        item(
            mono("TIMESTAMP") + " sem timezone, e n\u00e3o " + mono("TIMESTAMPTZ") + ": o Hibernate mapeia "
            + mono("LocalDateTime") + " para " + mono("timestamp") + " por padr\u00e3o; usar "
            + mono("timestamptz") + " faria " + mono("ddl-auto=validate") + " falhar."
        ),
        p(
            "Os \u00edndices em " + mono("origin_channel") + ", " + mono("status") + ", "
            + mono("branch_id") + " e " + mono("created_at") + " existem porque s\u00e3o exatamente os "
            "filtros previstos no roteiro (canal, status, unidade e per\u00edodo). \u00cdndice sem consulta "
            "correspondente \u00e9 custo de escrita sem retorno."
        ),
    ]


def persistence_test():
    return [
        subsection("3.5", "OrderPersistenceIntegrationTest: a prova de que persiste"),
        p(
            "O teste exercita a camada de persist\u00eancia de verdade, sem mock de "
            + mono("EntityManager") + ". Usa H2 em modo PostgreSQL porque Docker n\u00e3o est\u00e1 "
            "dispon\u00edvel na m\u00e1quina; a validade do DDL real \u00e9 coberta em separado pelo "
            + mono("SchemaDdlTest") + "."
        ),
        *table(
            "4",
            "Recursos de teste e seus efeitos",
            ["Recurso", "O que faz", "Por que est\u00e1 aqui"],
            [
                [
                    "@SpringBootTest",
                    "Sobe o contexto completo",
                    "Testa JPA e reposit\u00f3rios juntos, n\u00e3o a classe isolada",
                ],
                [
                    "@Transactional",
                    "Abre transa\u00e7\u00e3o com rollback",
                    "Cada teste deixa o banco limpo sem TRUNCATE manual",
                ],
                [
                    "saveAndFlush",
                    "For\u00e7a o INSERT imediato",
                    "Somente save deixaria o SQL pendente no buffer",
                ],
                [
                    "entityManager.clear()",
                    "Descarta o cache de primeiro n\u00edvel",
                    "Sem isso, a releitura viria da mem\u00f3ria e o teste seria falso-positivo",
                ],
                [
                    "isEqualByComparingTo",
                    "Compara BigDecimal por valor",
                    "isEqualTo falharia por diferen\u00e7a de escala (51,8 e 51,80)",
                ],
            ],
            [4.6 * cm, 4.8 * cm, 6.6 * cm],
        ),
        code(
            """
orderRepository.saveAndFlush(order);

entityManager.clear();

OrderJpaEntity reloaded = orderRepository
        .findWithItemsById(order.getId()).orElseThrow();

assertThat(reloaded.getItems()).hasSize(1);
assertThat(reloaded.getTotalAmount())
        .isEqualByComparingTo("51.80");
"""
        ),
        item("O INSERT \u00e9 for\u00e7ado antes da limpeza, garantindo que os dados chegaram ao banco."),
        item("Limpar o cache L1 \u00e9 o que transforma a releitura em consulta real."),
        item(
            mono("findWithItemsById") + " usa " + mono("@EntityGraph") + " para trazer os itens no "
            "mesmo " + mono("SELECT") + ", evitando " + mono("LazyInitializationException") + "."
        ),
        item("O total 2 x 25,90 = 51,80 \u00e9 conferido no item e no pedido, al\u00e9m de canal e status."),
        code(
            """
var byChannelAndStatus = orderRepository
        .findByOriginChannelAndStatus(
                SalesChannel.TOTEM, OrderStatus.PREPARING,
                PageRequest.of(0, 10));
assertThat(byChannelAndStatus.getContent())
        .extracting(OrderJpaEntity::getId)
        .contains(order.getId());
"""
        ),
        p(
            "A assinatura \u00e9 derivada: Spring Data traduz "
            + mono("findByOriginChannelAndStatus") + " em " + mono("WHERE origin_channel = ? AND status = ?")
            + ". Se o campo da entidade for renomeado, o teste deixa de compilar ou o contexto falha, "
            "o que \u00e9 o comportamento desejado."
        ),
        *table(
            "5",
            "Resultado da su\u00edte ao final da Etapa 1",
            ["Classe de teste", "Casos", "O que comprova"],
            [
                ["OrderPersistenceIntegrationTest", "5", "Pedido com itens e pagamento; filtros; estoque; unicidade de e-mail e de idempot\u00eancia"],
                ["SchemaDdlTest", "1", "V1 e V2 aplic\u00e1veis em banco limpo, com as contagens do seed"],
                ["RdnApiApplicationTests", "1", "Contexto Spring sobe com a configura\u00e7\u00e3o atual"],
            ],
            [6.4 * cm, 1.8 * cm, 7.8 * cm],
        ),
    ]


def other_files():
    return [
        subsection("3.6", "Arquivos complementares"),
        *table(
            "6",
            "Arquivos relevantes e seus pap\u00e9is",
            ["Arquivo", "Papel"],
            [
                ["PaymentStatus, PaymentMethod", "Enums de estado e forma de pagamento"],
                ["OrderItem", "Valida quantidade positiva; subtotal = pre\u00e7o x quantidade"],
                ["InventoryItem", "deductStock recusa saldo negativo; base do futuro 409 de estoque"],
                ["Address", "Value Object final e imut\u00e1vel, validado no construtor"],
                ["AddressEmbeddable", "Colunas de endere\u00e7o embutidas em branches"],
                ["OrderMapper", "Converte entidade em dom\u00ednio; usa BranchMapper.toDomainShallow"],
                ["OrderJpaRepository", "Filtros por canal, canal e status, idempot\u00eancia e @EntityGraph"],
                ["application.properties", "Datasource por vari\u00e1vel de ambiente, validate e open-in-view=false"],
                ["application.properties (teste)", "Substitui o de produ\u00e7\u00e3o nos testes: H2 e Flyway desligado"],
                ["V2__seed.sql", "Filial, card\u00e1pio, 3 itens, estoque e 3 movimentos INBOUND"],
                ["documentacao/docs/DER.md e documentacao/plans/01", "DER em Mermaid e registro das decis\u00f5es da etapa"],
            ],
            [6.0 * cm, 10.0 * cm],
        ),
    ]


def decisions():
    return [
        section("4", "Decis\u00f5es e trade-offs"),
        *table(
            "7",
            "Decis\u00f5es arquiteturais da Etapa 1",
            ["Decis\u00e3o", "Alternativa descartada", "Consequ\u00eancia aceita"],
            [
                [
                    "Dom\u00ednio puro e entidades JPA separadas",
                    "@Entity no pr\u00f3prio dom\u00ednio",
                    "13 entidades e 13 mappers a manter; em troca, Address continua imut\u00e1vel",
                ],
                [
                    "changeStatus com tabela de transi\u00e7\u00f5es",
                    "Validar no service",
                    "Nenhum caso de uso pode esquecer a regra; mudar o fluxo exige editar a tabela",
                ],
                [
                    "@GeneratedValue(UUID) no Java",
                    "gen_random_uuid() no banco",
                    "Funciona igual em H2 e PostgreSQL; custo de o ID existir antes do INSERT",
                ],
                [
                    "VARCHAR com CHECK para enums",
                    "CREATE TYPE AS ENUM",
                    "Portabilidade para H2; alterar um enum exige nova migration",
                ],
                [
                    "TIMESTAMP sem timezone",
                    "TIMESTAMPTZ",
                    "validate passa sem configura\u00e7\u00e3o extra; a aplica\u00e7\u00e3o grava em UTC",
                ],
                [
                    "H2 em modo PostgreSQL nos testes",
                    "Testcontainers",
                    "Roda sem Docker; o dialeto real \u00e9 coberto pelo teste de DDL",
                ],
                [
                    "Usu\u00e1rios fora do seed inicial",
                    "Hash BCrypt inventado",
                    "Nenhum login quebrado depois; o seed fica incompleto at\u00e9 a etapa de seguran\u00e7a",
                ],
                [
                    "Payment com m\u00faltiplas tentativas",
                    "Um pagamento por pedido",
                    "Hist\u00f3rico completo de recusas; o caso de uso precisa escolher a aprovada",
                ],
            ],
            [4.8 * cm, 5.0 * cm, 6.2 * cm],
        ),
    ]


def concepts():
    return [
        section("5", "Conceitos fundamentais"),
        item("<b>Agregado</b>: conjunto de objetos tratados como unidade; Order comanda os OrderItem (EVANS, 2003)."),
        item("<b>Idempot\u00eancia</b>: repetir a mesma requisi\u00e7\u00e3o produz o mesmo efeito; garantida por idempotency_key e por requirePending()."),
        item("<b>@Enumerated(STRING)</b>: persiste o nome do enum; ORDINAL grava a posi\u00e7\u00e3o e quebra se a ordem mudar."),
        item("<b>M\u00e1quina de estados</b>: transi\u00e7\u00f5es v\u00e1lidas declaradas como dados em ALLOWED_TRANSITIONS."),
        item("<b>@Embeddable</b>: objeto sem tabela pr\u00f3pria, com colunas embutidas no dono (AddressEmbeddable)."),
        item("<b>Consulta derivada</b>: o nome do m\u00e9todo gera o SQL; findByOriginChannelAndStatus vira WHERE."),
        item("<b>ddl-auto=validate</b>: o Hibernate compara mapeamento e schema e falha se divergir, mas nunca altera."),
        item("<b>Cache de primeiro n\u00edvel</b>: o EntityManager devolve a inst\u00e2ncia em mem\u00f3ria; clear() \u00e9 necess\u00e1rio para provar persist\u00eancia."),
    ]


def common_errors():
    return [
        section("6", "Erros comuns e armadilhas"),
        item(
            "Usar @Enumerated sem EnumType.STRING: o banco grava 0,1,2 e, ao inserir um valor no "
            "meio do enum, todos os registros antigos passam a apontar para o status errado."
        ),
        item(
            "Chamar order.setStatus(...) em vez de changeStatus(...): a m\u00e1quina de estados \u00e9 "
            "ignorada e um pedido entregue pode voltar para preparo."
        ),
        item(
            "Remover entityManager.clear() do teste: o findById devolve o objeto do cache L1, o "
            "SELECT nunca executa e o teste passa mesmo com o mapeamento quebrado."
        ),
        item(
            "Declarar TIMESTAMPTZ para um campo LocalDateTime: a aplica\u00e7\u00e3o nem sobe, porque "
            "ddl-auto=validate compara os tipos."
        ),
        item(
            "Trocar BigDecimal por double: 2 x 25,90 pode virar 51,799999... e o teste e o CHECK "
            "de total falham; dinheiro exige decimal."
        ),
        item(
            "Esquecer orphanRemoval = true: remover um item da lista em mem\u00f3ria n\u00e3o gera DELETE e "
            "o pedido volta do banco com item fantasma."
        ),
    ]


def exercises():
    return [
        section("7", "Exerc\u00edcios de fixa\u00e7\u00e3o"),
        p(
            "<b>Exerc\u00edcio 1.</b> Qual \u00e9 o resultado de "
            + mono("order.changeStatus(OrderStatus.READY)") + " em um Order rec\u00e9m-criado, com status "
            "nulo? E de " + mono("changeStatus(OrderStatus.CANCELLED)") + " no mesmo estado?"
        ),
        p(
            "<i>Gabarito:</i> ambos lan\u00e7am IllegalStateException. Com status nulo, o \u00fanico destino "
            "aceito \u00e9 RECEIVED; a regra de cancelar a partir de qualquer estado n\u00e3o final pressup\u00f5e "
            "que j\u00e1 exista um estado atual."
        ),
        p(
            "<b>Exerc\u00edcio 2.</b> Escreva um teste que prove que "
            + mono("InventoryItem.deductStock") + " recusa retirar mais que o saldo e que "
            + mono("isAvailable()") + " fica falso quando o saldo cai ao limite m\u00ednimo."
        ),
        code(
            """
@Test
void recusaSaidaMaiorQueOSaldoEIndicaIndisponibilidade() {
    InventoryItem item = new InventoryItem();
    item.setQuantityOnHand(new BigDecimal("10.000"));
    item.setMinimumThreshold(new BigDecimal("5.000"));

    assertThat(item.deductStock(new BigDecimal("11.000"), "teste"))
            .isFalse();
    assertThat(item.getQuantityOnHand())
            .isEqualByComparingTo("10.000");

    assertThat(item.deductStock(new BigDecimal("5.000"), "teste"))
            .isTrue();
    assertThat(item.getQuantityOnHand())
            .isEqualByComparingTo("5.000");
    assertThat(item.isAvailable()).isFalse();
}
"""
        ),
        p(
            "<i>Gabarito:</i> o teste prova que a sa\u00edda acima do saldo devolve falso e n\u00e3o altera "
            "o valor; e que isAvailable() usa compara\u00e7\u00e3o estrita, ou seja, saldo igual ao "
            "m\u00ednimo j\u00e1 \u00e9 indispon\u00edvel. Trocar o operador quebra o teste."
        ),
        p(
            "<b>Exerc\u00edcio 3.</b> Suponha que a cozinha precise de um estado "
            + mono("PARTIALLY_READY") + " entre PREPARING e READY. Liste todos os pontos que "
            "precisam mudar, do dom\u00ednio ao banco."
        ),
        item("OrderStatus: adicionar PARTIALLY_READY."),
        item("ALLOWED_TRANSITIONS: PREPARING para PARTIALLY_READY; PARTIALLY_READY para READY ou CANCELLED."),
        item("Nova migration: n\u00e3o editar V1; \u00e9 preciso remover e recriar a constraint de status com a nova lista."),
        item("documentacao/docs/DER.md: atualizar o enum documentado."),
        item("Testes: um caso v\u00e1lido novo e um inv\u00e1lido (PARTIALLY_READY para DELIVERED)."),
        p(
            "<i>Gabarito:</i> a lista acima. O ponto cr\u00edtico \u00e9 que editar V1 depois de aplicada "
            "quebra o checksum do Flyway e a aplica\u00e7\u00e3o n\u00e3o sobe mais."
        ),
    ]


def checklist():
    return [
        section("8", "Checklist de autoavalia\u00e7\u00e3o"),
        item("[   ] Sei dizer por que Order n\u00e3o tem @Entity e onde est\u00e1 o mapeamento dela.", ""),
        item("[   ] Sei diferenciar IllegalArgumentException de IllegalStateException em changeStatus.", ""),
        item("[   ] Sei justificar por que payments.idempotency_key \u00e9 NOT NULL e orders.idempotency_key \u00e9 nullable.", ""),
        item("[   ] Sei explicar o que acontece se orphanRemoval for removido da lista de itens.", ""),
        item("[   ] Sei dizer por que o teste precisa de entityManager.clear().", ""),
        item("[   ] Sei explicar por que o DDL usa CHECK se o Java j\u00e1 valida o enum.", ""),
        item("[   ] Sei justificar TIMESTAMP em vez de TIMESTAMPTZ neste projeto.", ""),
        item("[   ] Sei explicar por que BranchMapper.toDomainShallow existe.", ""),
    ]


def conclusion():
    return [
        section("9", "Considera\u00e7\u00f5es finais e pr\u00f3ximo passo"),
        p(
            "A Etapa 1 entregou a base persistente do Fluxo A: 13 tabelas criadas por Flyway, seed "
            "de cat\u00e1logo e estoque, mapeamento JPA separado do dom\u00ednio e 7 testes verdes, "
            "incluindo a execu\u00e7\u00e3o do DDL real em banco limpo. O sistema grava e rel\u00ea um "
            "pedido com itens e pagamento, mas ainda n\u00e3o \u00e9 uma API."
        ),
        p("Riscos e d\u00edvidas t\u00e9cnicas registrados ao final da etapa:"),
        item("Order.setStatus p\u00fablico permite burlar a m\u00e1quina de estados; o ideal \u00e9 restringir o acesso."),
        item("Camadas application e api vazias: nenhum endpoint, DTO ou tratamento de erro padronizado."),
        item("Mappers sem consumidor: foram escritos para adaptadores de reposit\u00f3rio que ainda n\u00e3o existem."),
        item("Seed sem usu\u00e1rios: n\u00e3o h\u00e1 como autenticar at\u00e9 a etapa de seguran\u00e7a."),
        item("V1 nunca rodou em PostgreSQL real; validar com Docker antes da entrega."),
        item("AuthSession guarda apenas hash: a valida\u00e7\u00e3o futura ser\u00e1 por compara\u00e7\u00e3o de hash."),
        item("Promotion n\u00e3o participa do pedido: falta o v\u00ednculo que registra a promo\u00e7\u00e3o aplicada."),
        p(
            "O passo seguinte natural \u00e9 a camada de aplica\u00e7\u00e3o do Fluxo A (criar pedido, "
            "registrar pagamento simulado e atualizar status), ainda sem HTTP, com testes de caso de "
            "uso sobre os reposit\u00f3rios existentes. Na evolu\u00e7\u00e3o do projeto essa etapa foi "
            "precedida pela seguran\u00e7a, documentada em relat\u00f3rio separado (Etapa 2), porque "
            "nenhuma regra de acesso pode existir sem identidade."
        ),
    ]


def references():
    return [
        centered_section("Refer\u00eancias"),
        p(
            "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. <b>NBR 14724</b>: informa\u00e7\u00e3o e "
            "documenta\u00e7\u00e3o \u2014 trabalhos acad\u00eamicos \u2014 apresenta\u00e7\u00e3o. Rio de Janeiro: "
            "ABNT, 2011.",
            REFERENCE,
        ),
        p(
            "ASSOCIA\u00c7\u00c3O BRASILEIRA DE NORMAS T\u00c9CNICAS. <b>NBR 6023</b>: informa\u00e7\u00e3o e "
            "documenta\u00e7\u00e3o \u2014 refer\u00eancias \u2014 elabora\u00e7\u00e3o. Rio de Janeiro: ABNT, 2018.",
            REFERENCE,
        ),
        p(
            "EVANS, Eric. <b>Domain-driven design</b>: tackling complexity in the heart of software. "
            "Boston: Addison-Wesley, 2003.",
            REFERENCE,
        ),
        p(
            "FOWLER, Martin. <b>Patterns of enterprise application architecture</b>. Boston: "
            "Addison-Wesley, 2003.",
            REFERENCE,
        ),
        p(
            "HIBERNATE. <b>Hibernate ORM documentation</b>. Dispon\u00edvel em: "
            "https://hibernate.org/orm/documentation/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "POSTGRESQL GLOBAL DEVELOPMENT GROUP. <b>PostgreSQL 16 documentation</b>. Dispon\u00edvel "
            "em: https://www.postgresql.org/docs/16/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "RED GATE. <b>Flyway documentation</b>. Dispon\u00edvel em: "
            "https://documentation.red-gate.com/flyway. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "SPRING. <b>Spring Boot reference documentation</b>. Dispon\u00edvel em: "
            "https://docs.spring.io/spring-boot/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "SPRING. <b>Spring Data JPA reference documentation</b>. Dispon\u00edvel em: "
            "https://docs.spring.io/spring-data/jpa/reference/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
    ]


# ------------------------------------------------------------------------ main

def build():
    doc = AbntDoc(
        OUTPUT,
        title="RDN-API: persistencia do fluxo de pedido",
        subject="Relatorio tecnico da Etapa 1 (ABNT)",
    )
    story = []
    story += cover()
    story += summary()
    story += introduction()
    story += architecture()
    story += order_domain()
    story += payment_domain()
    story += jpa_entity()
    story += schema()
    story += persistence_test()
    story += other_files()
    story += decisions()
    story += concepts()
    story += common_errors()
    story += exercises()
    story += checklist()
    story += conclusion()
    story += references()

    doc.multiBuild(story)
    print("PDF gerado: %s" % OUTPUT)
    print("Paginas: %d" % doc.page)


if __name__ == "__main__":
    build()
