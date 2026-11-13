package com.rdn.rdn_api.api.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.UserRole;
import com.rdn.rdn_api.infrastructure.persistence.entity.AddressEmbeddable;
import com.rdn.rdn_api.infrastructure.persistence.entity.BranchJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;
import com.rdn.rdn_api.infrastructure.repository.BranchJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.InventoryItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.InventoryMovementJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.OrderJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.PaymentJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Teste ponta a ponta do fluxo Pedido -> Pagamento mock -> Status.
 *
 * <p>Sobe a aplicacao em porta aleatoria e usa HTTP real, como o teste de
 * autenticacao, para exercitar JWT, autorizacao por perfil, validacao, maquina
 * de estados e idempotencia na mesma cadeia.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:order_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class OrderFlowIntegrationTest {

    private static final String PASSWORD = "Senha@123";

    private static final Pattern ID = Pattern.compile("\"pedidoId\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern ACCESS_TOKEN = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Autowired
    private Environment environment;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private BranchJpaRepository branchRepository;

    @Autowired
    private MenuJpaRepository menuRepository;

    @Autowired
    private MenuItemJpaRepository menuItemRepository;

    @Autowired
    private InventoryItemJpaRepository inventoryRepository;

    @Autowired
    private InventoryMovementJpaRepository movementRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    private BranchJpaEntity branch;

    private MenuItemJpaEntity burger;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        movementRepository.deleteAll();
        inventoryRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuRepository.deleteAll();
        branchRepository.deleteAll();
        userRepository.deleteAll();

        persistUser("cliente@test.local", UserRole.CUSTOMER);
        persistUser("outro.cliente@test.local", UserRole.CUSTOMER);
        persistUser("atendente@test.local", UserRole.STAFF);
        persistUser("cozinha@test.local", UserRole.KITCHEN);

        seedCatalog();
    }

    @Test
    void criarPedidoSemTokenRetorna401() throws Exception {
        HttpResponse<String> response = post("/pedidos", null, null,
                orderBody(SalesChannel.TOTEM, PaymentMethod.MOCK, 2, burger.getId()));

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"error\":\"NAO_AUTENTICADO\"");
    }

    @Test
    void criarPedidoComPagamentoMockNasceAguardandoPagamento() throws Exception {
        String token = customerToken();

        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(SalesChannel.TOTEM, PaymentMethod.MOCK, 2, burger.getId()));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"status\":\"AGUARDANDO_PAGAMENTO\"")
                .contains("\"canalPedido\":\"TOTEM\"")
                .contains("\"total\":51.80");
    }

    @Test
    void criarPedidoSemCanalRetorna400ComDetalheDoCampo() throws Exception {
        String token = customerToken();
        String body = """
                {"unidadeId":"%s","formaPagamento":"MOCK","itens":[{"produtoId":"%s","quantidade":1}]}
                """.formatted(branch.getId(), burger.getId());

        HttpResponse<String> response = post("/pedidos", token, null, body);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body())
                .contains("\"error\":\"DADOS_INVALIDOS\"")
                .contains("\"field\":\"canalPedido\"");
    }

    @Test
    void criarPedidoComEstoqueInsuficienteRetorna409() throws Exception {
        String token = customerToken();

        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(SalesChannel.TOTEM, PaymentMethod.MOCK, 999, burger.getId()));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"error\":\"ESTOQUE_INSUFICIENTE\"");
    }

    @Test
    void criarPedidoComProdutoInexistenteRetorna404() throws Exception {
        String token = customerToken();

        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(SalesChannel.WEB, PaymentMethod.MOCK, 1, java.util.UUID.randomUUID()));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"error\":\"RECURSO_NAO_ENCONTRADO\"");
    }

    @Test
    void listarPedidosComoStaffFiltraPorCanal() throws Exception {
        post("/pedidos", customerToken(), null,
                orderBody(SalesChannel.TOTEM, PaymentMethod.MOCK, 1, burger.getId()));
        post("/pedidos", customerToken(), null,
                orderBody(SalesChannel.WEB, PaymentMethod.MOCK, 1, burger.getId()));

        HttpResponse<String> response = get("/pedidos?canalPedido=TOTEM&page=0&limit=10", staffToken());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"canalPedido\":\"TOTEM\"");
        assertThat(occurrences(response.body(), "\"pedidoId\"")).isEqualTo(1);
    }

    @Test
    void listarPedidosComoClienteRetorna403() throws Exception {
        HttpResponse<String> response = get("/pedidos", customerToken());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"error\":\"ACESSO_NEGADO\"");
    }

    @Test
    void pagamentoAprovadoLiberaPedidoParaCozinha() throws Exception {
        String customer = customerToken();
        String pedidoId = createOrder(customer, PaymentMethod.MOCK);

        HttpResponse<String> payment = post("/pedidos/" + pedidoId + "/pagamentos", customer, "chave-aprovada",
                "{\"formaPagamento\":\"MOCK\",\"simular\":\"APROVADO\"}");

        assertThat(payment.statusCode()).isEqualTo(201);
        assertThat(payment.body())
                .contains("\"status\":\"APPROVED\"")
                .contains("\"pedidoStatus\":\"RECEIVED\"");

        HttpResponse<String> order = get("/pedidos/" + pedidoId, customer);
        assertThat(order.body()).contains("\"status\":\"RECEIVED\"");
    }

    @Test
    void pagamentoRecusadoCancelaPedido() throws Exception {
        String customer = customerToken();
        String pedidoId = createOrder(customer, PaymentMethod.MOCK);

        HttpResponse<String> payment = post("/pedidos/" + pedidoId + "/pagamentos", customer, "chave-recusada",
                "{\"formaPagamento\":\"MOCK\",\"simular\":\"RECUSADO\"}");

        assertThat(payment.statusCode()).isEqualTo(201);
        assertThat(payment.body())
                .contains("\"status\":\"DECLINED\"")
                .contains("\"pedidoStatus\":\"CANCELLED\"");
    }

    @Test
    void replayDaMesmaChaveRetorna200SemDuplicarTentativa() throws Exception {
        String customer = customerToken();
        String pedidoId = createOrder(customer, PaymentMethod.MOCK);
        String body = "{\"formaPagamento\":\"MOCK\",\"simular\":\"APROVADO\"}";

        HttpResponse<String> first = post("/pedidos/" + pedidoId + "/pagamentos", customer, "chave-replay", body);
        HttpResponse<String> second = post("/pedidos/" + pedidoId + "/pagamentos", customer, "chave-replay", body);

        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(200);

        HttpResponse<String> history = get("/pedidos/" + pedidoId + "/pagamentos", customer);
        assertThat(history.statusCode()).isEqualTo(200);
        assertThat(occurrences(history.body(), "\"pagamentoId\"")).isEqualTo(1);
    }

    @Test
    void pagamentoSemIdempotencyKeyRetorna400() throws Exception {
        String customer = customerToken();
        String pedidoId = createOrder(customer, PaymentMethod.MOCK);

        HttpResponse<String> response = post("/pedidos/" + pedidoId + "/pagamentos", customer, null,
                "{\"formaPagamento\":\"MOCK\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\":\"CABECALHO_OBRIGATORIO\"");
    }

    @Test
    void pagamentoDePedidoCanceladoRetorna409() throws Exception {
        String customer = customerToken();
        String pedidoId = createOrder(customer, PaymentMethod.MOCK);

        patch("/pedidos/" + pedidoId + "/status", staffToken(), "{\"status\":\"CANCELLED\",\"motivo\":\"teste\"}");

        HttpResponse<String> response = post("/pedidos/" + pedidoId + "/pagamentos", customer, "chave-cancelado",
                "{\"formaPagamento\":\"MOCK\"}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"error\":\"PEDIDO_NAO_AGUARDA_PAGAMENTO\"");
    }

    @Test
    void cozinhaAvancaStatusETransicaoInvalidaRetorna409() throws Exception {
        String pedidoId = createOrder(customerToken(), PaymentMethod.CASH);
        String kitchen = staffToken("cozinha@test.local");

        HttpResponse<String> preparing = patch("/pedidos/" + pedidoId + "/status", kitchen,
                "{\"status\":\"PREPARING\"}");
        assertThat(preparing.statusCode()).isEqualTo(200);
        assertThat(preparing.body()).contains("\"status\":\"PREPARING\"");

        HttpResponse<String> invalid = patch("/pedidos/" + pedidoId + "/status", kitchen,
                "{\"status\":\"DELIVERED\"}");
        assertThat(invalid.statusCode()).isEqualTo(409);
        assertThat(invalid.body()).contains("\"error\":\"TRANSICAO_INVALIDA\"");
    }

    @Test
    void clienteNaoPagaPedidoDeOutroCliente() throws Exception {
        String outroCliente = staffToken("outro.cliente@test.local");
        String pedidoId = createOrder(outroCliente, PaymentMethod.MOCK);

        HttpResponse<String> response = post("/pedidos/" + pedidoId + "/pagamentos", customerToken(),
                "chave-alheia", "{\"formaPagamento\":\"MOCK\",\"simular\":\"APROVADO\"}");

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"error\":\"ACESSO_NEGADO\"");
    }

    // ---------- apoio ----------

    private void persistUser(String email, UserRole role) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setFullName("Usuario " + role.name());
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(PASSWORD));
        entity.setRole(role);
        entity.setIsActive(Boolean.TRUE);
        userRepository.saveAndFlush(entity);
    }

    private void seedCatalog() {
        AddressEmbeddable address = new AddressEmbeddable();
        address.setStreet("Rua de Teste");
        address.setNumber("100");
        address.setNeighborhood("Centro");
        address.setCity("Caxias do Sul");
        address.setState("RS");
        address.setZipCode("95000-000");

        branch = new BranchJpaEntity();
        branch.setName("RDN Teste");
        branch.setIsOpen(Boolean.TRUE);
        branch.setAddress(address);

        MenuJpaEntity menu = new MenuJpaEntity();
        menu.setBranch(branch);
        menu.setTitle("Cardapio Teste");
        menu.setIsActive(Boolean.TRUE);
        branch.setMenu(menu);

        branch = branchRepository.saveAndFlush(branch);

        burger = new MenuItemJpaEntity();
        burger.setMenu(menu);
        burger.setName("X-Burger");
        burger.setDescription("Pao, carne e queijo");
        burger.setPrice(new BigDecimal("25.90"));
        burger.setIsAvailable(Boolean.TRUE);
        burger = menuItemRepository.saveAndFlush(burger);

        InventoryItemJpaEntity stock = new InventoryItemJpaEntity();
        stock.setBranch(branch);
        stock.setMenuItem(burger);
        stock.setQuantityOnHand(new BigDecimal("10.000"));
        stock.setMinimumThreshold(new BigDecimal("1.000"));
        inventoryRepository.saveAndFlush(stock);
    }

    private String createOrder(String token, PaymentMethod method) throws Exception {
        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(SalesChannel.TOTEM, method, 2, burger.getId()));

        assertThat(response.statusCode()).isEqualTo(201);
        return extract(response.body(), ID);
    }

    private String orderBody(SalesChannel canal, PaymentMethod method, int quantity,
            java.util.UUID produtoId) {
        return """
                {"canalPedido":"%s","unidadeId":"%s","formaPagamento":"%s",
                 "itens":[{"produtoId":"%s","quantidade":%d,"observacao":"sem cebola"}]}
                """.formatted(canal, branch.getId(), method, produtoId, quantity);
    }

    private String customerToken() throws Exception {
        return staffToken("cliente@test.local");
    }

    private String staffToken() throws Exception {
        return staffToken("atendente@test.local");
    }

    private String staffToken(String email) throws Exception {
        String body = "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, PASSWORD);
        HttpResponse<String> response = post("/auth/login", null, null, body);

        assertThat(response.statusCode()).isEqualTo(200);
        return extract(response.body(), ACCESS_TOKEN);
    }

    private HttpResponse<String> post(String path, String token, String idempotencyKey, String body)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (idempotencyKey != null) {
            builder.header("Idempotency-Key", idempotencyKey);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> patch(String path, String token, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private static String extract(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static int occurrences(String body, String needle) {
        int count = 0;
        int index = body.indexOf(needle);
        while (index >= 0) {
            count++;
            index = body.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
