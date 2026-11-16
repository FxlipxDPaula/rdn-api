package com.rdn.rdn_api.api.loyalty;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdn.rdn_api.domain.DiscountType;
import com.rdn.rdn_api.domain.PaymentMethod;
import com.rdn.rdn_api.domain.SalesChannel;
import com.rdn.rdn_api.domain.UserRole;
import com.rdn.rdn_api.infrastructure.persistence.entity.AddressEmbeddable;
import com.rdn.rdn_api.infrastructure.persistence.entity.BranchJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.InventoryItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.LoyaltyAccountJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.MenuJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.PromotionJpaEntity;
import com.rdn.rdn_api.infrastructure.persistence.entity.UserJpaEntity;
import com.rdn.rdn_api.infrastructure.repository.AuditLogJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.BranchJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.InventoryItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.InventoryMovementJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.LoyaltyAccountJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.LoyaltyTransactionJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuItemJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.MenuJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.OrderJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.PaymentJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.PromotionJpaRepository;
import com.rdn.rdn_api.infrastructure.repository.UserJpaRepository;

/**
 * Teste HTTP do fluxo de promocao, fidelidade, catalogo e auditoria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:loyalty_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class LoyaltyPromotionIntegrationTest {

    private static final String PASSWORD = "Senha@123";

    private static final Pattern PEDIDO_ID = Pattern.compile("\"pedidoId\"\\s*:\\s*\"([^\"]+)\"");

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

    @Autowired
    private PromotionJpaRepository promotionRepository;

    @Autowired
    private LoyaltyAccountJpaRepository loyaltyAccountRepository;

    @Autowired
    private LoyaltyTransactionJpaRepository loyaltyTransactionRepository;

    @Autowired
    private AuditLogJpaRepository auditLogRepository;

    private BranchJpaEntity branch;

    private MenuItemJpaEntity burger;

    private UserJpaEntity customer;

    private UUID promoId;

    private UUID promoExpiredId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        loyaltyTransactionRepository.deleteAll();
        loyaltyAccountRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        movementRepository.deleteAll();
        inventoryRepository.deleteAll();
        menuItemRepository.deleteAll();
        menuRepository.deleteAll();
        branchRepository.deleteAll();
        promotionRepository.deleteAll();
        userRepository.deleteAll();

        customer = persistUser("cliente@test.local", UserRole.CUSTOMER);
        persistUser("atendente@test.local", UserRole.STAFF);
        persistUser("admin@test.local", UserRole.ADMIN);
        seedCatalog();
        seedPromotions();
    }

    @Test
    void pedidoComPromocaoAplicaDescontoDoDominio() throws Exception {
        String token = token("cliente@test.local");

        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(PaymentMethod.MOCK, 2, promoId));

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body())
                .contains("\"desconto\":5.18")
                .contains("\"total\":46.62")
                .contains("\"promocaoId\":\"" + promoId + "\"");
    }

    @Test
    void promocaoExpiradaRetorna409() throws Exception {
        String token = token("cliente@test.local");

        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(PaymentMethod.MOCK, 2, promoExpiredId));

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"error\":\"PROMOCAO_NAO_APLICAVEL\"");
    }

    @Test
    void listaCampanhasComIndicadorDeVigencia() throws Exception {
        HttpResponse<String> response = get("/promocoes", token("cliente@test.local"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"titulo\":\"Semana do Burger\"")
                .contains("\"vigente\":true")
                .contains("\"vigente\":false");
    }

    @Test
    void consentimentoAcumulaPontosNoPagamentoAprovado() throws Exception {
        String token = token("cliente@test.local");

        HttpResponse<String> consent = put("/fidelidade/consentimento", token,
                "{\"consentimento\":true}");
        assertThat(consent.statusCode()).isEqualTo(200);
        assertThat(consent.body()).contains("\"consentimento\":true");

        String pedidoId = createOrder(token, PaymentMethod.MOCK, null);
        HttpResponse<String> payment = post("/pedidos/" + pedidoId + "/pagamentos", token,
                "chave-pontos", "{\"formaPagamento\":\"MOCK\",\"simular\":\"APROVADO\"}");
        assertThat(payment.statusCode()).isEqualTo(201);

        HttpResponse<String> account = get("/fidelidade", token);
        assertThat(account.statusCode()).isEqualTo(200);
        assertThat(account.body()).contains("\"saldoPontos\":51");

        HttpResponse<String> statement = get("/fidelidade/transacoes", token);
        assertThat(statement.statusCode()).isEqualTo(200);
        assertThat(statement.body())
                .contains("\"tipo\":\"EARNED\"")
                .contains("\"pontos\":51");
    }

    @Test
    void resgateAplicaDescontoEDepoisRecusaRepeticao() throws Exception {
        seedLoyaltyAccount(100, true);
        String token = token("cliente@test.local");
        String pedidoId = createOrder(token, PaymentMethod.MOCK, null);

        String body = "{\"pedidoId\":\"" + pedidoId + "\",\"pontos\":100}";
        HttpResponse<String> first = post("/fidelidade/resgates", token, null, body);

        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body())
                .contains("\"descontoAplicado\":10.00")
                .contains("\"novoSaldo\":0")
                .contains("\"totalPedido\":41.80");

        HttpResponse<String> second = post("/fidelidade/resgates", token, null, body);

        assertThat(second.statusCode()).isEqualTo(409);
        assertThat(second.body()).contains("\"error\":\"RESGATE_JA_APLICADO\"");
    }

    @Test
    void resgateSemConsentimentoRetorna409() throws Exception {
        String token = token("cliente@test.local");
        String pedidoId = createOrder(token, PaymentMethod.MOCK, null);

        HttpResponse<String> response = post("/fidelidade/resgates", token, null,
                "{\"pedidoId\":\"" + pedidoId + "\",\"pontos\":10}");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("\"error\":\"CONSENTIMENTO_NECESSARIO\"");
    }

    @Test
    void staffNaoAcessaFidelidade() throws Exception {
        HttpResponse<String> response = get("/fidelidade", token("atendente@test.local"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).contains("\"error\":\"ACESSO_NEGADO\"");
    }

    @Test
    void consultaUnidadesECardapio() throws Exception {
        String token = token("cliente@test.local");

        HttpResponse<String> branches = get("/unidades", token);
        assertThat(branches.statusCode()).isEqualTo(200);
        assertThat(branches.body())
                .contains("\"nome\":\"RDN Teste\"")
                .contains("\"cidade\":\"Caxias do Sul\"");

        HttpResponse<String> menu = get("/unidades/" + branch.getId() + "/cardapio", token);
        assertThat(menu.statusCode()).isEqualTo(200);
        assertThat(menu.body())
                .contains("\"titulo\":\"Cardapio Teste\"")
                .contains("\"nome\":\"X-Burger\"");
    }

    @Test
    void auditoriaSomenteAdmin() throws Exception {
        String customer = token("cliente@test.local");
        createOrder(customer, PaymentMethod.CASH, null);

        HttpResponse<String> admin = get("/auditoria?acao=ORDER_CREATED", token("admin@test.local"));
        assertThat(admin.statusCode()).isEqualTo(200);
        assertThat(admin.body()).contains("\"acao\":\"ORDER_CREATED\"");

        HttpResponse<String> staff = get("/auditoria", token("atendente@test.local"));
        assertThat(staff.statusCode()).isEqualTo(403);

        HttpResponse<String> anonymous = get("/auditoria", null);
        assertThat(anonymous.statusCode()).isEqualTo(401);
    }

    @Test
    void queryParamInvalidoRetorna400() throws Exception {
        HttpResponse<String> response = get("/pedidos?canalPedido=XYZ",
                token("atendente@test.local"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"error\":\"PARAMETRO_INVALIDO\"");
    }

    // ---------- apoio ----------

    private UserJpaEntity persistUser(String email, UserRole role) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setFullName("Usuario " + role.name());
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode(PASSWORD));
        entity.setRole(role);
        entity.setIsActive(Boolean.TRUE);
        return userRepository.saveAndFlush(entity);
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
        stock.setQuantityOnHand(new BigDecimal("100.000"));
        stock.setMinimumThreshold(new BigDecimal("1.000"));
        inventoryRepository.saveAndFlush(stock);
    }

    private void seedPromotions() {
        PromotionJpaEntity active = new PromotionJpaEntity();
        active.setTitle("Semana do Burger");
        active.setDescription("10% acima de R$ 30,00");
        active.setDiscountType(DiscountType.PERCENTAGE);
        active.setDiscountValue(new BigDecimal("10.00"));
        active.setMinOrderValue(new BigDecimal("30.00"));
        active.setIsActive(Boolean.TRUE);
        promoId = promotionRepository.saveAndFlush(active).getId();

        PromotionJpaEntity expired = new PromotionJpaEntity();
        expired.setTitle("Campanha Encerrada");
        expired.setDiscountType(DiscountType.PERCENTAGE);
        expired.setDiscountValue(new BigDecimal("15.00"));
        expired.setStartDate(LocalDateTime.now().minusDays(30));
        expired.setEndDate(LocalDateTime.now().minusDays(1));
        expired.setIsActive(Boolean.TRUE);
        promoExpiredId = promotionRepository.saveAndFlush(expired).getId();
    }

    private void seedLoyaltyAccount(int points, boolean consent) {
        LoyaltyAccountJpaEntity account = new LoyaltyAccountJpaEntity();
        account.setCustomer(userRepository.getReferenceById(customer.getId()));
        account.setPointsBalance(points);
        account.setConsentGiven(consent);
        account.setConsentTimestamp(consent ? LocalDateTime.now() : null);
        loyaltyAccountRepository.saveAndFlush(account);
    }

    private String createOrder(String token, PaymentMethod method, UUID promotionId) throws Exception {
        HttpResponse<String> response = post("/pedidos", token, null,
                orderBody(method, 2, promotionId));

        assertThat(response.statusCode()).isEqualTo(201);
        return extract(response.body(), PEDIDO_ID);
    }

    private String orderBody(PaymentMethod method, int quantity, UUID promotionId) {
        String promotionPart = promotionId == null ? "" : ",\"promocaoId\":\"" + promotionId + "\"";

        return """
                {"canalPedido":"%s","unidadeId":"%s","formaPagamento":"%s"%s,
                 "itens":[{"produtoId":"%s","quantidade":%d,"observacao":"sem cebola"}]}
                """.formatted(SalesChannel.TOTEM, branch.getId(), method, promotionPart,
                burger.getId(), quantity);
    }

    private String token(String email) throws Exception {
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

    private HttpResponse<String> put(String path, String token, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    private static String extract(String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
