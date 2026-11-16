# Etapa 7 — Finalização e entrega do Projeto Multidisciplinar

> Plano de arquitetura para fechar as lacunas entre o [roteiro](../docs/Roteiro%20Atividade%20Pratica.pdf:1)
> e o projeto **rdn-api**. O código de negócio já atende ao MVP (Fluxo A). O escopo desta etapa é
> **publicação, diagramas, coleção de testes e relatório ABNT** — sem refatorar domínio, casos de uso
> ou endpoints.

## 1. Resultado esperado

Ao final desta etapa:

- O repositório público contém o estado final do projeto e o README aponta para ele.
- O endpoint `POST /usuarios` (ADMIN) está implementado, testado e documentado.
- A coleção Postman tem **25 cenários (T01–T25)**, incluindo pagamento recusado, 404 e cadastro de usuário.
- O PDF final tem **figuras reais** (casos de uso, DER, classes e sequência), contratos representativos,
  plano de testes no formato do roteiro, capa ABNT completa e numeração correta.
- A suíte automatizada está verde e o número de testes do relatório confere.
- O arquivo de submissão é `<RU>_Projeto_Back_End.PDF`.

## 2. Decisões arquiteturais

| # | Decisão | Justificativa | Alternativa descartada |
|---|---|---|---|
| D1 | **Alterar o código apenas para o cadastro de usuários** | É o único RF ainda ausente; Fluxo A, multicanalidade, segurança, erro padrão e auditoria já estão implementados e testados | Refatorar domínio, pagamento ou estoque nesta etapa |
| D2 | **Implementar `POST /usuarios` restrito a ADMIN** | Fecha o RF de cadastro sem abrir auto-cadastro público e reaproveita BCrypt, JWT, auditoria e o padrão `ApiError` | Manter o cadastro documentado como parcial |
| D3 | **Diagramas em PlantUML com PNG versionado** | Fontes versionadas e renderização reproduzível; o PDF consome PNG e não depende de ferramenta externa na geração | Desenhar no PDF por código, Mermaid sem renderizador local |
| D4 | **DER em duas figuras** | 14 tabelas em uma única imagem ficam ilegíveis em A4 retrato | Uma figura única reduzida |
| D5 | **PDF gerado por `tools/abnt_pdf_final.py` com suporte a figuras** | Mantém a fonte de verdade do relatório no repositório e permite regenerar após qualquer ajuste | Editar o PDF manualmente |
| D6 | **Numeração automática de seções preservada** | O helper já numera `h1/h2` em [`abnt_doc.py`](../tools/abnt_doc.py:176); o gerador final passa a enviar títulos sem número | Remover a numeração do helper e renumerar todos os scripts de etapa |
| D7 | **Arquivo `<RU>_Projeto_Back_End.PDF` não versionado** | O RU é dado pessoal; o repositório mantém o nome neutro `projeto-back-end-final-abnt.pdf` e a cópia renomeada é gerada só para submissão | Commitar o arquivo com RU |
| D8 | **Fluxo B permanece fora do MVP** | O roteiro exige **um** fluxo completo; o Fluxo A está fechado e declarado no README | Implementar endpoints administrativos de estoque |

### 2.1 Fora de escopo

- Endpoints administrativos de estoque (Fluxo B).
- Refresh token, logout/revogação de JWT.
- CRUD administrativo de filial, cardápio e promoções.
- Automação de retenção/anonimização de `audit_logs`.
- Listagem, edição e desativação de usuários; o escopo é apenas o cadastro.
- Alterações em migrations, entidades de domínio e mappers.

## 3. Mapa de mudanças

| Arquivo | Ação |
|---|---|
| [`README.md`](../README.md:1) | URL do repositório, contagem de cenários, pasta `08-Erros`, limitação de cadastro, seção de regeneração de artefatos |
| [`postman/rdn-api.postman_collection.json`](../postman/rdn-api.postman_collection.json:1) | Nova pasta `08-Erros` com `T21a`, `T21` e `T22` |
| [`postman/rdn-api.postman_environment.json`](../postman/rdn-api.postman_environment.json:1) | Variáveis `pedidoRecusaId` e `idempotencyKeyRecusa` |
| `docs/diagrams/*.puml` | **Novos** fontes PlantUML dos 5 diagramas |
| `docs/diagrams/*.png` | **Novos** PNGs renderizados e versionados |
| `tools/render_diagrams.py` | **Novo** script de renderização (Java + PlantUML) |
| [`tools/abnt_doc.py`](../tools/abnt_doc.py:1) | `figure()`, tabela compacta, fonte de tabela, `h1/h2` com flag `numbered`, `references()` no sumário, capa com instituição/curso/RU |
| [`tools/abnt_pdf_final.py`](../tools/abnt_pdf_final.py:1) | Títulos sem número, figuras, RF corrigidos, contratos, tabelas de teste em 6 colunas, URL real |
| [`docs/projeto-back-end-final-abnt.pdf`](../docs/projeto-back-end-final-abnt.pdf:1) | Regenerado |
| [`.gitignore`](../.gitignore:1) | Ignorar `tools/plantuml.jar` |
| [`plans/07-finalizacao-entrega.md`](07-finalizacao-entrega.md:1) | Este plano |

Arquivos adicionados na execução: `api/user/*`, `application/user/*`,
`infrastructure/persistence/user/UserRegistrationGatewayAdapter.java` e os testes
`RegisterUserServiceTest` e `UserRegistrationIntegrationTest`. Também foram alterados
[`AuditAction`](../src/main/java/com/rdn/rdn_api/application/audit/AuditAction.java:9),
[`SecurityConfig`](../src/main/java/com/rdn/rdn_api/infrastructure/security/SecurityConfig.java:70) e
[`GlobalExceptionHandler`](../src/main/java/com/rdn/rdn_api/api/error/GlobalExceptionHandler.java:53).

## 4. Etapas de execução

### Etapa 0 — Baseline e branch

1. `git fetch origin` e `git status -sb` para descobrir a divergência real com `origin/main`.
2. Confirmar que a `main` local está em `2ba705b` ([`refs/heads/main`](../.git/refs/heads/main:1)) e que o
   ref remoto está defasado ([`packed-refs`](../.git/packed-refs:2)).
3. Criar a branch de trabalho `entrega-final`.
4. Rodar `.\mvnw.cmd test` para registrar o baseline antes de qualquer mudança.

**Aceite:** suíte verde e branch criada sem alterações pendentes.

### Etapa 1 — Publicação do repositório (P0)

1. Preencher a URL pública `https://github.com/FxlipxDPaula/rdn-api` no lugar do placeholder em
   [`README.md`](../README.md:236).
2. Fazer o mesmo no gerador do PDF, criando uma constante `REPO_URL` e usando-a na Tabela 6 de
   [`abnt_pdf_final.py`](../tools/abnt_pdf_final.py:219).
3. Ao final das etapas (item 9), `git push origin entrega-final`, abrir PR/fast-forward para `main` e
   `git push origin main`.
4. Verificar o repositório em janela anônima.

**Aceite:** o repositório abre sem login e contém o commit com este plano, o README atualizado e a
coleção com 22 cenários.

### Etapa 2 — Coleção Postman (P0)

> Atualização: a coleção final tem 25 cenários. Os cenários T23–T25 cobrem o cadastro de usuário e
> foram adicionados à mesma pasta `08-Erros`.

Adicionar a pasta `08-Erros` **depois** de `07-Auditoria` em
[`rdn-api.postman_collection.json`](../postman/rdn-api.postman_collection.json:594), sem mover os
cenários negativos existentes (T13–T19 continuam nas pastas de recurso para manter contexto).

| ID | Nome | Método/Rota | Pré-condição | Entrada | Esperado |
|---|---|---|---|---|---|
| T21a | Criar pedido para recusa | `POST /pedidos` | token do cliente; seed aplicado | `canalPedido=TOTEM`, `unidadeId={{unidadeId}}`, `formaPagamento=MOCK`, 1 item `{{produtoId}}` | `201`; salvar `pedidoRecusaId` |
| T21 | Pagamento recusado | `POST /pedidos/{{pedidoRecusaId}}/pagamentos` | T21a executado | header `Idempotency-Key` com GUID; `{ "formaPagamento":"MOCK", "simular":"RECUSADO" }` | `201`, `status=DECLINED`, `pedidoStatus=CANCELLED`, `transacaoGateway=null` |
| T22 | Pedido com produto inexistente | `POST /pedidos` | token do cliente | `canalPedido=WEB`, `unidadeId={{unidadeId}}`, item com `produtoId={{$guid}}` e quantidade 1 | `404`, `error=RECURSO_NAO_ENCONTRADO` |
| T23 | Admin cria usuário | `POST /usuarios` | token do admin | `nome`, `email` único, `senha`, `perfil` | `201` sem `passwordHash`; salvar `novoUsuarioEmail` |
| T24 | Cliente tenta cadastrar usuário | `POST /usuarios` | token do cliente | mesmo corpo de T23 | `403 ACESSO_NEGADO` |
| T25 | E-mail duplicado | `POST /usuarios` | T23 executado; token do admin | mesmo e-mail de T23 | `409 EMAIL_JA_CADASTRADO` |

Regras:

- `T21` precisa de script de pré-requisição para gerar `idempotencyKeyRecusa` (GUID), como em
  [`T07 Pagar aprovado`](../postman/rdn-api.postman_collection.json:366).
- `T21a` deve rodar em banco recém-semeado; a recusa cancela o pedido e não devolve estoque, então não
  reaproveitar o pedido de T06/T07.
- `T22` usa `{{$guid}}` no corpo; não depende de estado.
- Declarar `pedidoRecusaId`, `idempotencyKeyRecusa`, `novoUsuarioEmail` e `novoUsuarioId` no environment.
- `T23` gera o e-mail único no pre-request; `T25` reutiliza o mesmo valor e depende de T23.

**Aceite:** a coleção importa no Postman sem erro, executa na ordem e T21–T25 passam; README atualizado
para 25 cenários e ordem `01` a `08`.

### Etapa 3 — Diagramas (P0)

> Atualização: os diagramas foram implementados em `tools/render_diagrams.py` com Pillow (já usado pelo
> ReportLab). Não há dependência de PlantUML, Graphviz, Node ou JDK. Os cinco PNGs já estão em
> `docs/diagrams/` e são gerados por `python tools/render_diagrams.py`.

Criar `docs/diagrams/` com as fontes e os PNGs:

| Figura | Fonte | Conteúdo |
|---|---|---|
| 1 — Casos de uso | `01-casos-de-uso.puml` | Atores Cliente, Atendente, Cozinha, Gerente/Administrador e Gateway de Pagamento; casos autenticar, consultar cardápio, criar pedido (inclui validar estoque e aplicar promoção), solicitar pagamento (inclui gateway mock), acompanhar/atualizar status, cancelar, consentir fidelidade, resgatar pontos, consultar campanhas e auditoria |
| 2 — DER núcleo | `02-der-nucleo.puml` | `users`, `orders`, `order_items`, `payments`, `promotions` com PK/FK, cardinalidades e restrições principais |
| 3 — DER apoio | `03-der-apoio.puml` | `branches`, `menus`, `menu_items`, `inventory_items`, `inventory_movements`, `loyalty_accounts`, `loyalty_transactions`, `auth_sessions`, `audit_logs` |
| 4 — Classes de domínio | `04-classes-dominio.puml` | `Order`, `OrderItem`, `Payment`, `Promotion`, `LoyaltyAccount`, `LoyaltyTransaction`, `LoyaltyPolicy`, `InventoryItem`, `InventoryMovement`, `Menu`, `MenuItem`, `Branch`, `User`, `Address` e enums; anotar que o domínio não importa JPA |
| 5 — Sequência Fluxo A | `05-sequencia-fluxo-a.puml` | `OrderController → CreateOrderService → CatalogGateway/InventoryGateway/OrderGateway`, depois `PaymentController → RequestPaymentService → PaymentProvider → Order/Payment → AuditGateway`; blocos `alt` para `APPROVED`, `DECLINED`, `FAILED` e `opt` para replay idempotente |

Renderização:

- `tools/render_diagrams.py` localiza `tools/plantuml.jar` (ou `PLANTUML_JAR`), valida `java -version`,
  executa `java -jar <jar> -tpng docs/diagrams/*.puml` e falha com mensagem clara se o jar não existir.
- Baixar o jar uma vez via Maven (`mvn dependency:get -Dartifact=net.sourceforge.plantuml:plantuml:<versao-estavel>`)
  e ignorar `tools/plantuml.jar` no git.
- Commitar os PNGs para a geração do PDF não depender do PlantUML.
- Atualizar [`docs/DER.md`](../docs/DER.md:8) com uma nota de que as Figuras 2 e 3 são a renderização
  do mesmo modelo; mudança de schema exige regenerar as imagens.

**Aceite:** cinco PNGs legíveis em A4, fontes versionadas e script reproduzível documentado no README.

### Etapa 4 — Evolução do gerador ABNT (`tools/abnt_doc.py`)

1. **Figuras**: importar `Image` e `ImageReader`; adicionar contador `self._figure`; método
   `figure(path, caption, source="Fonte: elaborado pelo autor (2026).")` que:
   - lança `FileNotFoundError` se o PNG não existir;
   - calcula largura `TEXT_WIDTH` e altura proporcional, com limite de 20 cm;
   - escreve a legenda "Figura N — ..." acima da imagem e a fonte abaixo;
   - centraliza a imagem (`hAlign="CENTER"`).
2. **Tabela compacta**: adicionar parâmetro `compact=False` em `table()`; quando verdadeiro, usar
   `TABLE_TEXT_SMALL` e `TABLE_HEAD_SMALL` (8.5 pt / 11 leading) e reduzir paddings.
3. **Fonte de tabela**: adicionar parâmetro `source` em `table()` e imprimir a fonte abaixo da tabela
   com estilo pequeno.
4. **Numeração**: `h1(text, numbered=True)` e `h2(text, numbered=True)`; quando `numbered=False`,
   renderizar o texto sem número e **não** incrementar o contador. Usar isso no ANEXO A.
5. **Referências**: refatorar `references()` para chamar `self.h1("REFERÊNCIAS", numbered=False)` e
   marcar `_toc_level = 1`, eliminando o cabeçalho duplicado.
6. **Capa**: adicionar constantes `INSTITUTION`, `COURSE`, `RU` e `PROFESSOR` (opcional); reorganizar
   `cover()` na ordem ABNT: instituição, curso, autor, RU, título, subtítulo, cidade, ano.

**Aceite:** os scripts das etapas 1–5 continuam gerando PDF (numeração antiga preservada) e o script
final usa as novas opções sem duplicar títulos.

### Etapa 5 — Conteúdo do PDF final (`tools/abnt_pdf_final.py`)

1. **Títulos sem número**: remover os prefixos "1 ", "2 ", "2.1 " etc. das chamadas `h1/h2`; a
   numeração continua automática. Chamar o ANEXO A com `numbered=False`.
2. **Figuras**: inserir
   - Figura 1 após a descrição da feature em 3.1;
   - Figuras 2 e 3 após a Tabela 3 em 3.2;
   - Figura 4 após o texto de classes em 3.3;
   - Figura 5 após o texto de sequência em 3.3.
3. **RF corrigidos** na Tabela 1:
   - "Cadastro de usuários" → "POST /usuarios restrito a ADMIN, com senha BCrypt" → **Implementado**;
   - "Autenticação com perfis" → **Implementado**;
   - adicionar "Gestão de unidades" → "GET /unidades; CRUD administrativo não implementado" → **Parcial**;
   - adicionar "Cancelamento de pedido" → "PATCH /pedidos/{id}/status para CANCELLED" → **Implementado**.
4. **Contratos representativos** em nova subseção 4.1, capturados de uma execução real (Postman/Swagger),
   não copiados do template:
   - `POST /auth/login` request e response `200` (campos reais: `accessToken`, `tokenType`, `expiresIn`,
     `user.role`);
   - `POST /usuarios` request e response `201` sem `passwordHash`;
   - `POST /pedidos` request com `canalPedido` e response `201`;
   - `GET /pedidos?canalPedido=TOTEM&status=AGUARDANDO_PAGAMENTO` response paginado;
   - `POST /pedidos/{id}/pagamentos` com header `Idempotency-Key` e response `201`/replay `200`;
   - erro `409 ESTOQUE_INSUFICIENTE` no formato `ApiError`.
   - Deixar explícito que o contrato completo está no Swagger.
5. **Plano de testes**:
   - Tabela 7 — positivos (T01–T12 e T23);
   - Tabela 8 — negativos (T13–T22, T24 e T25);
   - colunas: **ID, Endpoint + método, Pré-condição, Entrada, Esperado (status + trecho), Evidência (nome na coleção)**;
   - nota de rodapé para os helpers T02b, T11a, T18a e T21a;
   - substituir "20 cenários" por 25 e remover a contagem fixa de testes (usar o total real da suíte).
6. **Evidências** (Tabela 6): URL real do repositório, Swagger local, coleção, environment, DER,
   `docs/diagrams/`, PDF final; remover qualquer placeholder.
7. **Conclusão**: acrescentar o cadastro por ADMIN e a referência às figuras.
8. **Referências**: remover a chamada `h1("9 REFERENCIAS")`; usar apenas `report.references([...])`.

**Aceite:** PDF sem "1 1", sem "2.1 2.1", sem "REFERÊNCIAS" duplicado; figuras visíveis e legendadas;
tabelas de teste completas; URL do repositório presente.

### Etapa 6 — README

1. URL do repositório na seção 15.
2. Seção 12: 25 cenários (T01–T25), ordem `01-Auth` a `08-Erros`, helpers e aviso de seed limpo.
3. Seção 14: manter `POST /usuarios` como cadastro restrito a ADMIN e registrar que listagem/edição/
   desativação ficaram como proposta, junto com o Fluxo B.
4. Seção 16: acrescentar `docs/diagrams/` e `tools/render_diagrams.py`.
5. Nova subseção "Como regenerar os artefatos":
   - `python tools/render_diagrams.py`;
   - `python tools/abnt_pdf_final.py`;
   - cópia para `<RU>_Projeto_Back_End.PDF`.
6. Confirmar que todos os links relativos abrem no GitHub.

**Aceite:** README sem placeholders, com contagens coerentes e comandos reproduzíveis.

### Etapa 7 — Validação

1. `.\mvnw.cmd test` — suíte verde; anotar o total de testes.
2. Subir a API com seed limpo e executar a coleção completa no Postman (ou Newman, se disponível).
3. Conferir T21 (recusado), T22 (404) e T23–T25 (cadastro) e o restante da coleção.
4. Se houver Docker: `docker compose up -d`, subir a API, executar o fluxo rápido do README e guardar
   log/print como evidência. Se não houver, manter a limitação declarada no README e no PDF.
5. Regerar o PDF e revisar visualmente página por página.
6. Validar o JSON da coleção (importar no Postman) e o environment.

**Aceite:** testes verdes, coleção 25/25, PDF revisado, evidências registradas.

### Etapa 8 — Geração e renomeação do PDF

1. `python tools/abnt_pdf_final.py` gera `docs/projeto-back-end-final-abnt.pdf`.
2. Copiar para `<RU>_Projeto_Back_End.PDF` (ou `.PDF` conforme o roteiro).
3. Não commitar a cópia com RU; manter apenas o PDF neutro no repositório.
4. Conferir capa, sumário, figuras, tabelas, referências e declaração de IA.

**Aceite:** arquivo único, com capa/sumário/seções/referências e nome exigido.

### Etapa 9 — Commits, push e entrega

Sequência de commits sugerida:

1. `feat(usuarios): adiciona cadastro POST /usuarios restrito a ADMIN`
2. `test(usuarios): cobre cadastro, duplicidade, 403 e 400`
3. `docs(repo): preenche URL pública e atualiza contagens do README`
4. `test(postman): adiciona T21 recusa, T22 404 e T23–T25 cadastro`
5. `docs(diagramas): adiciona PNGs de casos de uso, DER, classes e sequência`
6. `feat(pdf): suporta figuras, tabelas compactas e capa ABNT completa`
7. `docs(pdf): corrige numeração, contratos e plano de testes com 25 cenários`
8. `chore(entrega): regenera PDF final e atualiza evidências`

Depois:

- `git push origin entrega-final`;
- fast-forward/merge em `main` e `git push origin main`;
- `git tag -a v1.0-entrega -m "Entrega final"` e `git push origin v1.0-entrega`;
- verificar links públicos e o README na interface do GitHub.

**Aceite:** `git log origin/main..HEAD` vazio após o push; repositório público contém tudo.

## 5. Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Pillow indisponível | Sem figuras no PDF | `render_diagrams.py` falha com mensagem clara; os PNGs já estão versionados |
| Estoque do seed insuficiente para T21a | T21 falha | Rodar a coleção em banco limpo e usar quantidade 1 |
| PDF com tabela de 6 colunas ilegível | Perda de clareza | `table(compact=True)` + split em tabela de positivos e negativos |
| RU/instituição ausentes | Capa e nome do arquivo incompletos | Coletar antes da Etapa 5; usar placeholders explícitos até lá |
| Docker ausente | Sem smoke real em PostgreSQL | Manter a limitação declarada, como já feito em [`README.md`](../README.md:228) |
| Repositório remoto divergente | Conflito no push | `git fetch` na Etapa 0 e rebase controlado antes de começar |

## 6. Critérios de pronto (DoD)

- [ ] Repositório público com o commit final e a tag.
- [ ] README sem placeholders.
- [ ] `POST /usuarios` implementado, testado e documentado.
- [ ] Coleção Postman com 25 cenários (T01–T25) e pasta `08-Erros`.
- [ ] T21 recusado, T22 404 e T23–T25 cadastro executando com asserts.
- [ ] Cinco figuras embutidas no PDF, com legenda e fonte.
- [ ] Tabelas de teste com ID, endpoint, pré-condição, entrada, esperado e evidência.
- [ ] PDF sem numeração duplicada e sem referências duplicadas.
- [ ] Capa com instituição, curso e RU.
- [ ] `.\mvnw.cmd test` verde e número de testes do PDF conferido.
- [ ] PDF final renomeado para `<RU>_Projeto_Back_End.PDF`.

## 7. Pendências de informação

- **RU** do aluno (capa e nome do arquivo).
- **Instituição e curso** para a capa ABNT.
- **Docker/PostgreSQL** disponível nesta máquina? (define se a Etapa 7 terá smoke real ou limitação declarada).
