# Etapa 2 — Segurança base: BCrypt, JWT e `/auth/login`

## Objetivo

Fechar a porta de entrada da API: usuários semeados no banco, senha com hash BCrypt,
login emitindo JWT e um filtro que protege rotas por perfil. Ao final, `POST /auth/login`
e `GET /auth/me` funcionam de ponta a ponta e o padrão de erro JSON já nasce aqui,
porque `401` e `403` são parte desta entrega.

## Contexto de partida (Etapa 1 concluída)

- Persistência pronta: 13 tabelas em `V1__schema.sql`, seed de filial/cardápio/estoque em `V2__seed.sql`,
  entidades JPA + mappers + repositórios em `infrastructure/persistence` e `infrastructure/repository`.
- `mvnw test` verde: 7 testes (5 de persistência, 1 de contexto, 1 de DDL/seed).
- Domínio com invariantes: `Order.changeStatus`, `InventoryItem.deductStock`, total limitado pelo desconto.
- **Nenhum usuário no seed**: `V2__seed.sql` adia de propósito porque BCrypt ainda não está no classpath.
- **Camadas `application/` e `api/` ainda não existem** — esta etapa cria a `application/` e a `api/` mínima de auth.
- `User.authenticate(String)` hoje compara senha em texto puro ([`User.java`](../src/main/java/com/rdn/rdn_api/domain/User.java:40)) e precisa sair de cena.

## Critério de pronto (DoD)

- `POST /auth/login` responde `200` com `accessToken` (JWT), `tokenType=Bearer`, `expiresIn` e `user` sem `passwordHash`.
- Credenciais inválidas → `401` com JSON de erro padrão; usuário inativo → `403`.
- Body inválido (email vazio, senha vazia) → `400` com `details` por campo.
- `GET /auth/me` sem token → `401`; com token válido → `200` com o usuário autenticado.
- `V3__seed.sql` cria 4 usuários (um por perfil) com hash BCrypt válido.
- `User.authenticate` deixa de existir ou de ser usado; a verificação passa a usar `PasswordEncoder`.
- `mvnw test` verde incluindo testes de login (unitário + MockMvc) e `SchemaDdlTest` atualizado para V3.
- Nenhum endpoint de pedido/pagamento nesta etapa (fica para a Etapa 3).

## Decisões arquiteturais

1. **JWT HMAC-SHA256 com Nimbus** (`spring-security-oauth2-jose`), sem biblioteca de terceiros.
   Assinatura e verificação via `NimbusJwtEncoder`/`NimbusJwtDecoder` do Spring Security.
2. **`BCryptPasswordEncoder` força 10** (default do Spring). Testes mais rápidos; força 12 fica como
   evolução documentada.
3. **Stateless**: sem refresh token e sem logout nesta etapa. A tabela `auth_sessions` continua no schema,
   mas não é usada (trade-off registrado: sessão persistida só se houver refresh/blacklist).
4. **Authorities** `ROLE_ADMIN`, `ROLE_STAFF`, `ROLE_KITCHEN`, `ROLE_CUSTOMER` derivadas de `UserRole`,
   gravadas no claim `roles` do token e convertidas em `GrantedAuthority` no filtro.
5. **Segredo e expiração por ambiente**: `JWT_SECRET` (mínimo 32 bytes) e `JWT_EXPIRATION_SECONDS`
   (default `3600`). Valor default só para desenvolvimento, com aviso no README.
6. **Padrão de erro desde já**: `@RestControllerAdvice` + `ApiError` (`error`, `message`, `details`,
   `timestamp`, `path`), cobrindo `400/401/403/404/409/500`. Os endpoints da Etapa 3 já herdam o formato.
7. **Ports na `application/`**: `UserGateway` (busca por email) e `TokenIssuer` (emite token) como
   interfaces; adaptadores em `infrastructure/security`. Mantém `application/` sem depender de JPA.

## Pacotes e classes novas

```
application/auth/
  LoginCommand            record(email, senha)
  LoginResult             record(accessToken, tokenType, expiresIn, AuthenticatedUser)
  AuthenticatedUser       record(id, fullName, email, role)
  LoginService            orquestra: busca usuário -> PasswordEncoder.matches -> emite token
  AuthUserGateway         port: Optional<User> findByEmail(String email)
  TokenIssuer             port: Optional<Token> issue(User user)

api/auth/
  AuthController          POST /auth/login, GET /auth/me
  LoginRequest            record com @Email @NotBlank
  LoginResponse           record de resposta
api/error/
  ApiError, ApiErrorFactory, GlobalExceptionHandler

infrastructure/security/
  SecurityConfig          SecurityFilterChain + PasswordEncoder bean
  JwtProperties           secret / expiration (via @ConfigurationProperties)
  JwtTokenIssuer          implements TokenIssuer (Nimbus)
  JwtAuthenticationFilter OncePerRequestFilter: valida token, popula SecurityContext
  AuthUserGatewayAdapter  implements AuthUserGateway (UserJpaRepository + UserMapper)
```

## Domínio

- Remover `User.authenticate(String)` ([`User.java`](../src/main/java/com/rdn/rdn_api/domain/User.java:40)):
  comparação de senha em texto puro não pode permanecer. Verificação passa a ser responsabilidade
  do `LoginService` com `PasswordEncoder`, que é detalhe de infraestrutura.
- `User.isActive()` já existe e é usado para negar login de usuário desativado.
- Se algum teste usa `authenticate`, ajustar para o novo fluxo.

## Contratos desta etapa

### `POST /auth/login` (público)

Request:
```json
{ "email": "admin@rdn.local", "senha": "Admin@123" }
```

Response `200`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": "00000000-0000-0000-0000-00000000000C", "fullName": "Administrador RDN", "email": "admin@rdn.local", "role": "ADMIN" }
}
```

Status: `200`, `400` (validação), `401` (`CREDENCIAIS_INVALIDAS`), `403` (`USUARIO_INATIVO`), `500`.

### `GET /auth/me` (JWT)

Response `200`: mesmo objeto `user` do login. Status: `200`, `401`, `403`.

## Seed (V3)

| Usuário | Senha (dev) | Perfil |
|---|---|---|
| `admin@rdn.local` | `Admin@123` | ADMIN |
| `staff@rdn.local` | `Staff@123` | STAFF |
| `kitchen@rdn.local` | `Kitchen@123` | KITCHEN |
| `customer@rdn.local` | `Customer@123` | CUSTOMER |

- IDs fixos `00000000-0000-0000-0000-00000000000C` a `...0F`.
- Hashes BCrypt gerados com `BCryptPasswordEncoder` antes de escrever a migration (teste descartável
  que imprime `encode(...)`, ou `jshell` com o classpath do projeto).
- Hashes fixos exigem regeração se a senha mudar — anotar no comentário da migration.

## Configuração

`application.properties`:
- `security.jwt.secret=${JWT_SECRET:<valor-dev-32-bytes>}`
- `security.jwt.expiration-seconds=${JWT_EXPIRATION_SECONDS:3600}`

`pom.xml`: `spring-boot-starter-security` e `spring-security-oauth2-jose`;
teste com `spring-boot-starter-security-test` (MockMvc com usuário autenticado).

## Testes

1. `LoginServiceTest` (Mockito): login válido emite token; senha errada → `401`;
   email inexistente → `401` (mesma mensagem, sem enumerar usuários); usuário inativo → `403`.
2. `AuthControllerIntegrationTest` (`@SpringBootTest` + MockMvc + H2): cria usuário BCrypt no setup
   (seed V3 não roda nos testes, porque Flyway está desativado e o schema é gerado pelo Hibernate).
   Casos: login `200`, senha errada `401`, body inválido `400`, `/auth/me` sem token `401`,
   `/auth/me` com token `200`, JSON de erro no formato `ApiError`.
3. `SchemaDdlTest`: após V1+V2+V3, `users` tem 4 linhas e todos os hashes começam com `$2a$`/`$2b$`.

## Riscos

| Risco | Mitigação |
|---|---|
| Spring Boot 4 / Spring Security 7 mudaram nomes de starters ou APIs | Compilar e rodar os testes de auth logo após adicionar as dependências, antes de escrever controllers |
| Build CLI falha sem `JAVA_HOME` (shell aponta para JRE 8) | Documentar `JAVA_HOME` apontando para o JDK 25 e validar no início da etapa |
| Hash BCrypt fixo no seed fica inválido se a senha mudar | Comentário na migration + senhas registradas no README |
| Segredo default em `application.properties` vazar para produção | Aviso no README + tarefa de exigir `JWT_SECRET` em perfil não-dev |
| `@SpringBootTest` sem Flyway divergir do seed real | `SchemaDdlTest` cobre V1+V2+V3 em banco limpo |

## Depois desta etapa

- **Etapa 3 — API de pedidos**: criar pedido validando estoque (`409`), `canalPedido` obrigatório,
  filtros `?canalPedido=&status=` com paginação, consulta por id, atualização de status pela máquina de estados.
- **Etapa 4 — Pagamento mock**: solicitação de pagamento, idempotência, aprovação/recusa e atualização de status do pedido.
- **Etapa 5 — Fidelidade, promoções e auditoria**: consentimento, pontos, log de ações sensíveis.
- **Etapa 6 — Entrega**: OpenAPI/Swagger, README, coleção Postman (≥10 cenários), validação final em PostgreSQL real.
