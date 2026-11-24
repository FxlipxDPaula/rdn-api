#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera o relatorio da Etapa 2 em PDF seguindo a NBR 14724 (ABNT).

Formatacao aplicada:
- A4 (210 x 297 mm), margens 3 cm (esquerda/superior) e 2 cm (direita/inferior)
- Times New Roman 12 pt, espacamento 1,5, texto justificado
- Recuo de primeira linha 1,25 cm
- Capa, sumario com numero de paginas, secoes numeradas e referencias
- Numero de pagina no canto superior direito (suprimido na capa)
- Tabelas e codigo em espacamento simples

Uso:
    python tools/abnt_pdf.py
"""

from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    KeepTogether,
    LongTable,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents

PAGE_W, PAGE_H = A4
MARGIN_LEFT = 3 * cm
MARGIN_RIGHT = 2 * cm
MARGIN_TOP = 3 * cm
MARGIN_BOTTOM = 2 * cm
TEXT_WIDTH = PAGE_W - MARGIN_LEFT - MARGIN_RIGHT

OUTPUT = "documentacao/docs/etapa-2-seguranca-abnt.pdf"
AUTHOR = "Felipe D'Paula"
YEAR = "2026"

# --------------------------------------------------------------------- estilos

BODY = ParagraphStyle(
    "body",
    fontName="Times-Roman",
    fontSize=12,
    leading=18,
    alignment=TA_JUSTIFY,
    firstLineIndent=1.25 * cm,
    spaceBefore=0,
    spaceAfter=0,
)

SECTION = ParagraphStyle(
    "section",
    fontName="Times-Bold",
    fontSize=12,
    leading=18,
    alignment=TA_LEFT,
    spaceBefore=18,
    spaceAfter=6,
    keepWithNext=1,
)

SUBSECTION = ParagraphStyle(
    "subsection",
    fontName="Times-Bold",
    fontSize=12,
    leading=18,
    alignment=TA_LEFT,
    spaceBefore=12,
    spaceAfter=3,
    keepWithNext=1,
)

LIST_ITEM = ParagraphStyle(
    "list_item",
    parent=BODY,
    firstLineIndent=0,
    leftIndent=1.25 * cm,
    spaceAfter=6,
)

CODE = ParagraphStyle(
    "code",
    fontName="Courier",
    fontSize=8.5,
    leading=10.5,
    alignment=TA_LEFT,
    leftIndent=0.5 * cm,
    spaceBefore=6,
    spaceAfter=8,
    backColor=colors.HexColor("#f3f3f3"),
    borderPadding=4,
)

TABLE_HEAD = ParagraphStyle(
    "table_head",
    fontName="Times-Bold",
    fontSize=9.5,
    leading=11.5,
    alignment=TA_CENTER,
)

TABLE_CELL = ParagraphStyle(
    "table_cell",
    fontName="Times-Roman",
    fontSize=9.5,
    leading=11.5,
    alignment=TA_LEFT,
)

CAPTION = ParagraphStyle(
    "caption",
    fontName="Times-Roman",
    fontSize=10,
    leading=12,
    alignment=TA_LEFT,
    spaceBefore=8,
    spaceAfter=2,
    keepWithNext=1,
)

SOURCE = ParagraphStyle(
    "source",
    fontName="Times-Roman",
    fontSize=10,
    leading=12,
    alignment=TA_LEFT,
    spaceAfter=12,
)

REFERENCE = ParagraphStyle(
    "reference",
    fontName="Times-Roman",
    fontSize=12,
    leading=12,
    alignment=TA_LEFT,
    spaceAfter=12,
)

COVER_TOP = ParagraphStyle(
    "cover_top",
    fontName="Times-Bold",
    fontSize=12,
    leading=18,
    alignment=TA_CENTER,
)

COVER_AUTHOR = ParagraphStyle(
    "cover_author",
    fontName="Times-Roman",
    fontSize=12,
    leading=18,
    alignment=TA_CENTER,
    spaceBefore=36,
)

COVER_TITLE = ParagraphStyle(
    "cover_title",
    fontName="Times-Bold",
    fontSize=16,
    leading=22,
    alignment=TA_CENTER,
)

COVER_SUBTITLE = ParagraphStyle(
    "cover_subtitle",
    fontName="Times-Roman",
    fontSize=12,
    leading=18,
    alignment=TA_CENTER,
    spaceBefore=12,
)

COVER_BOTTOM = ParagraphStyle(
    "cover_bottom",
    fontName="Times-Roman",
    fontSize=12,
    leading=18,
    alignment=TA_CENTER,
)

TOC_TITLE = ParagraphStyle(
    "toc_title",
    fontName="Times-Bold",
    fontSize=12,
    leading=18,
    alignment=TA_CENTER,
    spaceAfter=18,
)

REFERENCE_TITLE = ParagraphStyle(
    "reference_title",
    parent=SECTION,
    alignment=TA_CENTER,
)


# --------------------------------------------------------------------- helpers

def mono(text):
    """Trecho de codigo inline em Courier."""
    return '<font name="Courier" size="10">%s</font>' % escape(text)


def p(text, style=BODY):
    return Paragraph(text, style)


def item(text, marker="\u2013"):
    return Paragraph(text, LIST_ITEM, bulletText=marker)


def section(number, title):
    text = "%s %s" % (number, title.upper()) if number else title.upper()
    para = Paragraph(text, SECTION)
    para._toc_level = 0
    para._toc_text = text
    return para


def subsection(number, title):
    text = "%s %s" % (number, title)
    para = Paragraph(text, SUBSECTION)
    para._toc_level = 1
    para._toc_text = text
    return para


def code(block):
    return KeepTogether(Preformatted(block.strip("\n"), CODE))


def centered_section(title):
    text = title.upper()
    para = Paragraph(text, REFERENCE_TITLE)
    para._toc_level = 0
    para._toc_text = text
    return para


def table(number, title, headers, rows, widths):
    data = [[Paragraph("<b>%s</b>" % escape(h), TABLE_HEAD) for h in headers]]
    for row in rows:
        data.append([Paragraph(escape(str(cell)), TABLE_CELL) for cell in row])

    grid = LongTable(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    grid.setStyle(
        TableStyle(
            [
                ("GRID", (0, 0), (-1, -1), 0.5, colors.black),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 3),
                ("RIGHTPADDING", (0, 0), (-1, -1), 3),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
            ]
        )
    )

    return [
        Paragraph("Tabela %s \u2014 %s" % (number, escape(title)), CAPTION),
        grid,
        Paragraph("Fonte: elaborado pelo autor (%s)." % YEAR, SOURCE),
    ]


class AbntDoc(BaseDocTemplate):
    """Documento A4 com numeracao no canto superior direito e sumario."""

    def __init__(self, filename,
                 title="RDN-API: seguranca e autenticacao na API de pedidos",
                 subject="Relatorio tecnico da Etapa 2 (ABNT)"):
        super().__init__(
            filename,
            pagesize=A4,
            leftMargin=MARGIN_LEFT,
            rightMargin=MARGIN_RIGHT,
            topMargin=MARGIN_TOP,
            bottomMargin=MARGIN_BOTTOM,
            title=title,
            author=AUTHOR,
            subject=subject,
        )
        frame = Frame(
            MARGIN_LEFT,
            MARGIN_BOTTOM,
            TEXT_WIDTH,
            PAGE_H - MARGIN_TOP - MARGIN_BOTTOM,
            id="body",
        )
        self.addPageTemplates([PageTemplate(id="body", frames=[frame], onPage=self._page_number)])

    def _page_number(self, canvas, doc):
        if doc.page == 1:
            return
        canvas.saveState()
        canvas.setFont("Times-Roman", 10)
        canvas.drawRightString(PAGE_W - MARGIN_RIGHT, PAGE_H - 1.6 * cm, str(doc.page))
        canvas.restoreState()

    def afterFlowable(self, flowable):
        level = getattr(flowable, "_toc_level", None)
        if level is not None:
            self.notify("TOCEntry", (level, flowable._toc_text, self.page))


# ---------------------------------------------------------------------- capa

def cover():
    return [
        Spacer(1, 0.4 * cm),
        p("PROJETO MULTIDISCIPLINAR", COVER_TOP),
        p("TRILHA BACK-END", COVER_TOP),
        p(AUTHOR, COVER_AUTHOR),
        Spacer(1, 4.6 * cm),
        p("RDN-API: SEGURAN\u00c7A E AUTENTICA\u00c7\u00c3O NA API DE PEDIDOS", COVER_TITLE),
        p("Relat\u00f3rio t\u00e9cnico da Etapa 2: BCrypt, JWT e camada de aplica\u00e7\u00e3o", COVER_SUBTITLE),
        Spacer(1, 7.2 * cm),
        p("Caxias do Sul - RS", COVER_BOTTOM),
        p(YEAR, COVER_BOTTOM),
        PageBreak(),
    ]


def summary():
    toc = TableOfContents()
    toc.dotsMinLevel = 0
    toc.levelStyles = [
        ParagraphStyle(
            "toc_level_0",
            fontName="Times-Bold",
            fontSize=12,
            leading=18,
            leftIndent=0,
            firstLineIndent=0,
        ),
        ParagraphStyle(
            "toc_level_1",
            fontName="Times-Roman",
            fontSize=12,
            leading=18,
            leftIndent=0.8 * cm,
            firstLineIndent=0,
        ),
    ]

    return [p("SUM\u00c1RIO", TOC_TITLE), toc, PageBreak()]


# -------------------------------------------------------------------- conteudo

def introduction():
    return [
        section("1", "Introdu\u00e7\u00e3o"),
        p(
            "O projeto rdn-api implementa o back-end de uma rede de lanchonetes em expans\u00e3o, "
            "com atendimento em m\u00faltiplos canais: aplicativo, totem, balc\u00e3o, pickup e web. "
            "O sistema precisa registrar a origem de cada pedido, controlar estoque por unidade, "
            "aplicar o programa de fideliza\u00e7\u00e3o e simular a integra\u00e7\u00e3o com um servi\u00e7o "
            "externo de pagamento. A Etapa 1 do trabalho entregou a base persistente (Flyway, JPA "
            "e testes de integra\u00e7\u00e3o); esta Etapa 2 entrega a camada de seguran\u00e7a e os "
            "primeiros endpoints da API."
        ),
        p(
            "A autentica\u00e7\u00e3o foi priorizada porque nenhuma regra de acesso pode existir sem "
            "identidade. Sem saber quem chama a API, n\u00e3o h\u00e1 como restringir a cozinha a "
            "altera\u00e7\u00f5es de status, impedir que um cliente consulte pedidos de outro ou "
            "proteger dados pessoais. A Lei Geral de Prote\u00e7\u00e3o de Dados (LGPD) refor\u00e7a "
            "essa necessidade ao exigir finalidade, minimiza\u00e7\u00e3o e seguran\u00e7a no tratamento "
            "de dados (BRASIL, 2018)."
        ),
        p(
            "O texto est\u00e1 organizado em tr\u00eas blocos. A Se\u00e7\u00e3o 2 descreve a arquitetura "
            "em camadas e os artefatos produzidos. A Se\u00e7\u00e3o 3 detalha a implementa\u00e7\u00e3o, "
            "arquivo por arquivo, incluindo trechos de c\u00f3digo e o racioc\u00ednio de cada decis\u00e3o. "
            "As Se\u00e7\u00f5es 4 a 9 consolidam decis\u00f5es, conceitos, erros comuns, exerc\u00edcios e o "
            "encaminhamento para a Etapa 3."
        ),
    ]


def architecture():
    return [
        section("2", "Arquitetura em camadas do projeto"),
        p(
            "A aplica\u00e7\u00e3o segue uma varia\u00e7\u00e3o de Ports and Adapters sobre Spring Boot: o "
            "dom\u00ednio concentra entidades e invariantes, a camada de aplica\u00e7\u00e3o orquestra "
            "casos de uso por meio de portas (interfaces), a infraestrutura implementa essas "
            "portas e a camada de API traduz HTTP em comandos. A API segue o estilo REST "
            "(FIELDING, 2000), com recursos no plural, identifica\u00e7\u00e3o no caminho e "
            "c\u00f3digos de status coerentes."
        ),
        *table(
            "1",
            "Camadas, responsabilidades e artefatos da Etapa 2",
            ["Camada", "Responsabilidade", "Artefatos desta etapa"],
            [
                [
                    "Domain",
                    "Entidades, enums e invariantes, sem anota\u00e7\u00e3o de persist\u00eancia",
                    "Remo\u00e7\u00e3o de User.authenticate; User.isActive usado no login",
                ],
                [
                    "Application",
                    "Casos de uso e portas; n\u00e3o conhece JPA nem JWT",
                    "LoginService, LoginCommand, LoginResult, AuthenticatedUser, AuthUserGateway, TokenIssuer",
                ],
                [
                    "Infrastructure",
                    "Persist\u00eancia, migra\u00e7\u00f5es, JWT, BCrypt e adaptadores",
                    "SecurityConfig, JwtTokenIssuer, JwtAuthenticationFilter, AuthUserGatewayAdapter, V3__seed_users.sql",
                ],
                [
                    "API",
                    "Contratos HTTP, valida\u00e7\u00e3o de entrada e erros",
                    "AuthController, LoginRequest, LoginResponse, ApiError, GlobalExceptionHandler",
                ],
            ],
            [2.6 * cm, 5.4 * cm, 8.0 * cm],
        ),
        p(
            "A integra\u00e7\u00e3o entre camadas acontece por interfaces pr\u00f3prias. O caso de uso "
            "declara " + mono("AuthUserGateway") + " e " + mono("TokenIssuer") + "; a "
            "infraestrutura fornece as implementa\u00e7\u00f5es. Assim, trocar a biblioteca de token "
            "n\u00e3o altera a regra de neg\u00f3cio, e o dom\u00ednio permanece livre de depend\u00eancias de "
            "framework."
        ),
        *table(
            "2",
            "Resumo quantitativo da entrega",
            ["Item", "Quantidade"],
            [
                ["Classes Java novas", "22"],
                ["Migra\u00e7\u00e3o Flyway nova", "1 (V3, quatro usu\u00e1rios)"],
                ["Endpoints expostos", "2 (POST /auth/login e GET /auth/me)"],
                ["Testes automatizados (total do projeto)", "20, todos verdes"],
            ],
            [11.0 * cm, 5.0 * cm],
        ),
    ]


def security_overview():
    return [
        section("3", "Implementa\u00e7\u00e3o da seguran\u00e7a"),
        subsection("3.1", "Vis\u00e3o geral do fluxo de autentica\u00e7\u00e3o"),
        p(
            "O fluxo foi dividido em dois momentos. No login, o cliente envia e-mail e senha; "
            "a aplica\u00e7\u00e3o localiza o usu\u00e1rio, compara a senha com o hash BCrypt armazenado "
            "e emite um token JWT assinado com HMAC-SHA256. Nas demais requisi\u00e7\u00f5es, o token "
            "vai no cabe\u00e7alho " + mono("Authorization: Bearer <token>") + " e um filtro o valida "
            "antes de qualquer controller."
        ),
        item("O cliente envia POST /auth/login com e-mail e senha."),
        item("O LoginService normaliza o e-mail e busca o usu\u00e1rio no reposit\u00f3rio."),
        item("O PasswordEncoder compara a senha informada com o hash BCrypt do banco."),
        item("Credenciais v\u00e1lidas e usu\u00e1rio ativo: o JwtTokenIssuer monta as claims e assina o token."),
        item("A resposta devolve o token e um resumo p\u00fablico do usu\u00e1rio, sem o hash."),
        item("Nas chamadas seguintes, o JwtAuthenticationFilter decodifica o token e popula o contexto."),
        item("Regras de autoriza\u00e7\u00e3o por perfil podem usar as authorities ROLE_* j\u00e1 carregadas."),
        *table(
            "3",
            "Endpoints entregues na Etapa 2",
            ["M\u00e9todo/Rota", "Autentica\u00e7\u00e3o", "Sucesso", "Falhas previstas"],
            [
                [
                    "POST /auth/login",
                    "P\u00fablica",
                    "200 com accessToken, tokenType, expiresIn e user",
                    "400 valida\u00e7\u00e3o; 401 credenciais; 403 usu\u00e1rio inativo",
                ],
                [
                    "GET /auth/me",
                    "JWT (Bearer)",
                    "200 com id, fullName, email e role",
                    "401 sem token, token inv\u00e1lido ou expirado",
                ],
            ],
            [3.2 * cm, 2.6 * cm, 5.2 * cm, 5.0 * cm],
        ),
    ]


def login_service():
    return [
        subsection("3.2", "Caso de uso de login (LoginService)"),
        p(
            "O " + mono("LoginService") + " \u00e9 o cora\u00e7\u00e3o da etapa. Ele recebe um "
            + mono("LoginCommand") + " imut\u00e1vel, busca o usu\u00e1rio pela porta "
            + mono("AuthUserGateway") + ", compara a senha usando " + mono("PasswordEncoder") + " e "
            "solicita o token \u00e0 porta " + mono("TokenIssuer") + ". Nenhuma dessas etapas conhece "
            "detalhes de HTTP, JPA ou da biblioteca de JWT."
        ),
        code(
            """
public LoginResult login(LoginCommand command) {
    String email = normalizeEmail(command.email());

    User user = userGateway.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException(
                    "E-mail ou senha invalidos."));

    if (!passwordMatches(command.password(), user.getPasswordHash())) {
        throw new InvalidCredentialsException("E-mail ou senha invalidos.");
    }

    if (!user.isActive()) {
        throw new InactiveUserException("Usuario inativo.");
    }

    TokenIssuer.IssuedToken token = tokenIssuer.issue(user);
    return new LoginResult(token.accessToken(), "Bearer",
            token.expiresInSeconds(), AuthenticatedUser.from(user));
}
"""
        ),
        item("O comando isola a aplica\u00e7\u00e3o do DTO HTTP; a API pode mudar sem alterar a regra."),
        item(
            "A normaliza\u00e7\u00e3o (trim e min\u00fasculas com Locale.ROOT) evita falhas de login por "
            "espa\u00e7os ou diferen\u00e7a de caixa no e-mail."
        ),
        item(
            "A mesma exce\u00e7\u00e3o e a mesma mensagem s\u00e3o usadas para e-mail inexistente e senha "
            "incorreta, impedindo a enumera\u00e7\u00e3o de contas."
        ),
        item(
            "A verifica\u00e7\u00e3o de senha ocorre antes da checagem de inatividade: quem n\u00e3o sabe a "
            "senha recebe 401, n\u00e3o 403, e n\u00e3o descobre o status da conta."
        ),
        item(
            "O m\u00e9todo auxiliar " + mono("passwordMatches") + " trata senha nula ou em branco e hash "
            "nulo antes de chamar o encoder, porque o BCrypt lan\u00e7aria exce\u00e7\u00e3o e a resposta "
            "viraria erro 500."
        ),
        item(
            "A sa\u00edda usa " + mono("AuthenticatedUser") + ", um recorte que n\u00e3o carrega "
            + mono("passwordHash") + " nem qualquer dado sens\u00edvel."
        ),
        p(
            "O dom\u00ednio deixou de oferecer " + mono("User.authenticate(String)") + ", que comparava "
            "senhas em texto puro. Qualquer c\u00f3digo futuro que tente repetir esse padr\u00e3o n\u00e3o "
            "compila, o que documenta a decis\u00e3o diretamente no tipo."
        ),
    ]


def security_config():
    return [
        subsection("3.3", "Configura\u00e7\u00e3o de seguran\u00e7a (SecurityConfig)"),
        p(
            "O " + mono("SecurityConfig") + " define a pol\u00edtica de acesso, os beans de criptografia "
            "e a cadeia de filtros. O modelo \u00e9 stateless: n\u00e3o h\u00e1 sess\u00e3o HTTP, e a \u00fanica "
            "credencial \u00e9 o token. A configura\u00e7\u00e3o \u00e9 fechada por padr\u00e3o "
            + mono("anyRequest().authenticated()") + ", de modo que endpoints criados nas "
            "pr\u00f3ximas etapas j\u00e1 nas\u00e7am protegidos."
        ),
        code(
            """
http
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
        .exceptionHandling(handling -> handling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);
"""
        ),
        item(
            "CSRF \u00e9 desnecess\u00e1rio em API stateless sem cookie de sess\u00e3o; mant\u00ea-lo ativo "
            "bloquearia POSTs leg\u00edtimos de clientes que n\u00e3o enviam formul\u00e1rio."
        ),
        item(
            "HTTP Basic e login por formul\u00e1rio s\u00e3o desligados para que as credenciais circulem "
            "apenas pelo JSON de login, sobre HTTPS."
        ),
        item(
            "Somente o login e o endpoint interno de erro s\u00e3o p\u00fablicos; todo o restante exige "
            "token v\u00e1lido."
        ),
        item(
            "O filtro JWT \u00e9 inserido antes do filtro padr\u00e3o de usu\u00e1rio e senha, garantindo que "
            "o contexto esteja preenchido quando a autoriza\u00e7\u00e3o for avaliada."
        ),
        item(
            "O filtro \u00e9 instanciado no pr\u00f3prio m\u00e9todo, e n\u00e3o anotado como componente, para o "
            "Spring Boot n\u00e3o registr\u00e1-lo duas vezes no container de servlets."
        ),
        code(
            """
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}

@Bean
JwtEncoder jwtEncoder(JwtProperties properties) {
    return new NimbusJwtEncoder(
            new ImmutableSecret<SecurityContext>(hmacKey(properties)));
}

@Bean
JwtDecoder jwtDecoder(JwtProperties properties) {
    return NimbusJwtDecoder.withSecretKey(hmacKey(properties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
}
"""
        ),
        p(
            "O m\u00e9todo " + mono("hmacKey") + " recusa segredos com menos de 32 bytes e derruba a "
            "inicializa\u00e7\u00e3o da aplica\u00e7\u00e3o. A escolha \u00e9 intencional: HS256 usa chave de 256 "
            "bits, e um segredo curto enfraqueceria a assinatura sem qualquer aviso em tempo de "
            "execu\u00e7\u00e3o."
        ),
    ]


def token_issuer():
    return [
        subsection("3.4", "Emiss\u00e3o do token (JwtTokenIssuer)"),
        p(
            "O " + mono("JwtTokenIssuer") + " implementa a porta " + mono("TokenIssuer") + " e "
            "converte o usu\u00e1rio em claims. O token \u00e9 assinado, n\u00e3o criptografado: o conte\u00fado "
            "\u00e9 leg\u00edvel por qualquer portador, portanto apenas dados n\u00e3o sens\u00edveis entram no "
            "payload (JONES; BRADLEY; SAKIMURA, 2015)."
        ),
        code(
            """
Instant issuedAt = Instant.now();
Instant expiresAt = issuedAt.plusSeconds(properties.getExpirationSeconds());

JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("rdn-api")
        .subject(user.getId().toString())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim("email", user.getEmail())
        .claim("name", user.getFullName())
        .claim("roles", List.of(user.getRole().name()))
        .build();

JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
String token = jwtEncoder
        .encode(JwtEncoderParameters.from(header, claims))
        .getTokenValue();
"""
        ),
        item(
            "O claim " + mono("sub") + " guarda o identificador do usu\u00e1rio e \u00e9 o valor usado para "
            "reconstruir a identidade no filtro."
        ),
        item(
            "Os claims " + mono("iat") + " e " + mono("exp") + " definem a janela de validade; o "
            "decoder rejeita tokens vencidos sem c\u00f3digo adicional."
        ),
        item(
            "Os claims " + mono("email") + " e " + mono("name") + " evitam uma consulta ao banco por "
            "requisi\u00e7\u00e3o, ao custo de ficarem desatualizados at\u00e9 a expira\u00e7\u00e3o."
        ),
        item(
            "O claim " + mono("roles") + " \u00e9 uma lista com o perfil do usu\u00e1rio, pronta para virar "
            "authority no filtro."
        ),
        p(
            "Durante os testes, a primeira vers\u00e3o falhou com a mensagem "
            + mono("Failed to select a JWK signing key") + ". A causa era a aus\u00eancia do cabe\u00e7alho "
            "JWS: sem ele, o encoder assume RS256, n\u00e3o encontra chave RSA e aborta. A corre\u00e7\u00e3o "
            "foi declarar " + mono("JwsHeader.with(MacAlgorithm.HS256)") + " antes de codificar."
        ),
    ]


def auth_filter():
    return [
        subsection("3.5", "Filtro de autentica\u00e7\u00e3o (JwtAuthenticationFilter)"),
        p(
            "O filtro l\u00ea o cabe\u00e7alho de autoriza\u00e7\u00e3o em cada requisi\u00e7\u00e3o. Quando o token "
            "\u00e9 v\u00e1lido, monta um " + mono("AuthenticatedUser") + " e registra a autentica\u00e7\u00e3o "
            "no " + mono("SecurityContextHolder") + "; quando \u00e9 inv\u00e1lido, limpa o contexto e "
            "segue. Ele nunca responde por conta pr\u00f3pria, para que 401 e 403 tenham um \u00fanico "
            "formato de corpo."
        ),
        code(
            """
String header = request.getHeader(HttpHeaders.AUTHORIZATION);
if (header != null && header.startsWith("Bearer ")) {
    authenticate(header.substring("Bearer ".length()).trim());
}
filterChain.doFilter(request, response);
"""
        ),
        code(
            """
Jwt jwt = jwtDecoder.decode(token);
UserRole role = firstRole(jwt.getClaimAsStringList("roles"));
if (role == null) {
    return;
}

AuthenticatedUser principal = new AuthenticatedUser(
        UUID.fromString(jwt.getSubject()),
        jwt.getClaimAsString("name"),
        jwt.getClaimAsString("email"),
        role);

List<GrantedAuthority> authorities =
        List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
                principal, null, authorities));
"""
        ),
        item(
            "Header ausente \u00e9 situa\u00e7\u00e3o normal em rotas p\u00fablicas e n\u00e3o gera erro no filtro."
        ),
        item(
            "O decode valida assinatura e expira\u00e7\u00e3o; token adulterado, vencido ou de outro "
            "emissor lan\u00e7a exce\u00e7\u00e3o e o contexto \u00e9 limpo."
        ),
        item(
            "Role ausente ou desconhecida invalida a autentica\u00e7\u00e3o: token sem perfil n\u00e3o serve "
            "para autorizar nada."
        ),
        item(
            "O prefixo ROLE_ \u00e9 a conven\u00e7\u00e3o do Spring Security; com ele, "
            + mono("hasRole(\"ADMIN\")") + " passa a funcionar nas pr\u00f3ximas etapas."
        ),
        item(
            "Limpar o contexto no catch evita que uma autentica\u00e7\u00e3o sobreviva entre requisi\u00e7\u00f5es "
            "que reutilizam a mesma thread do pool do servidor."
        ),
    ]


def exception_handler():
    return [
        subsection("3.6", "Padroniza\u00e7\u00e3o de erros (GlobalExceptionHandler)"),
        p(
            "Todas as falhas da API respondem no mesmo formato: " + mono("error") + " (c\u00f3digo "
            "est\u00e1vel), " + mono("message") + " (texto leg\u00edvel), " + mono("details") + " "
            "(problemas por campo), " + mono("timestamp") + " e " + mono("path") + ". O handler "
            "central evita que cada controller invente seu pr\u00f3prio JSON."
        ),
        code(
            """
@ExceptionHandler(InvalidCredentialsException.class)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public ApiError handleInvalidCredentials(
        InvalidCredentialsException ex, HttpServletRequest request) {
    return ApiError.of("CREDENCIAIS_INVALIDAS",
            ex.getMessage(), request.getRequestURI());
}

@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiError handleValidation(
        MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiErrorDetail> details = ex.getBindingResult()
            .getFieldErrors().stream()
            .map(error -> new ApiErrorDetail(
                    error.getField(), error.getDefaultMessage()))
            .toList();
    return ApiError.of("DADOS_INVALIDOS",
            "Um ou mais campos estao invalidos.",
            details, request.getRequestURI());
}
"""
        ),
        *table(
            "4",
            "Erros padronizados e seus gatilhos",
            ["Status", "C\u00f3digo do erro", "Quando acontece"],
            [
                ["400", "DADOS_INVALIDOS", "Bean Validation encontra campo obrigat\u00f3rio vazio ou e-mail inv\u00e1lido"],
                ["400", "CORPO_INVALIDO", "JSON ausente ou malformado"],
                ["401", "CREDENCIAIS_INVALIDAS", "E-mail inexistente ou senha incorreta"],
                ["401", "NAO_AUTENTICADO", "Requisi\u00e7\u00e3o protegida sem token v\u00e1lido"],
                ["403", "USUARIO_INATIVO", "Credenciais corretas, mas conta desativada"],
                ["403", "ACESSO_NEGADO", "Autenticado sem o perfil exigido"],
                ["404", "RECURSO_NAO_ENCONTRADO", "Rota inexistente"],
                ["500", "ERRO_INTERNO", "Falha n\u00e3o prevista; stack trace fica no log do servidor"],
            ],
            [1.5 * cm, 5.2 * cm, 9.3 * cm],
        ),
        p(
            "Os erros de autentica\u00e7\u00e3o que ocorrem antes do DispatcherServlet s\u00e3o escritos pelo "
            + mono("RestAuthenticationEntryPoint") + " e pelo " + mono("RestAccessDeniedHandler") + ", "
            "com o mesmo formato. O m\u00e9todo gen\u00e9rico registra o stack trace no log e devolve "
            "mensagem neutra ao cliente, sem expor detalhes internos."
        ),
    ]


def decisions():
    return [
        section("4", "Decis\u00f5es de projeto e trade-offs"),
        *table(
            "5",
            "Decis\u00f5es arquiteturais da Etapa 2",
            ["Decis\u00e3o", "Alternativa descartada", "Consequ\u00eancia aceita"],
            [
                [
                    "Dom\u00ednio puro com portas na aplica\u00e7\u00e3o",
                    "Aplica\u00e7\u00e3o acessando o reposit\u00f3rio JPA diretamente",
                    "Duas interfaces e um adaptador a mais para manter as camadas expl\u00edcitas",
                ],
                [
                    "JWT HMAC com Nimbus (spring-security-oauth2-jose)",
                    "Biblioteca externa como jjwt",
                    "Nenhuma depend\u00eancia de terceiros; API do pr\u00f3prio Spring Security",
                ],
                [
                    "Stateless, sem sess\u00e3o persistida",
                    "Refresh token e tabela auth_sessions em uso",
                    "Logout e revoga\u00e7\u00e3o de token ficam para etapa futura",
                ],
                [
                    "BCryptPasswordEncoder com for\u00e7a 10",
                    "DelegatingPasswordEncoder com prefixo {bcrypt}",
                    "Seed simples, por\u00e9m trocar de encoder no futuro exigir\u00e1 migrar os hashes",
                ],
                [
                    "JSON de erro manual no entry point",
                    "Injetar ObjectMapper nas respostas de seguran\u00e7a",
                    "Evita acoplamento \u00e0 vers\u00e3o de Jackson do Spring Boot 4",
                ],
                [
                    "Filtro JWT instanciado no SecurityConfig",
                    "Filtro anotado como @Component",
                    "Sem registro duplicado no container de servlets",
                ],
                [
                    "Remo\u00e7\u00e3o de User.authenticate",
                    "Manter o m\u00e9todo depreciado",
                    "Compara\u00e7\u00e3o de senha em texto puro deixa de compilar",
                ],
            ],
            [4.8 * cm, 5.0 * cm, 6.2 * cm],
        ),
        p(
            "As decis\u00f5es privilegiam clareza de camadas e seguran\u00e7a b\u00e1sica em vez de "
            "funcionalidades avan\u00e7adas. A aus\u00eancia de refresh token, por exemplo, simplifica o "
            "MVP sem impedir a evolu\u00e7\u00e3o: a tabela " + mono("auth_sessions") + " j\u00e1 existe no "
            "schema e pode ser ativada quando houver necessidade de revoga\u00e7\u00e3o."
        ),
    ]


def concepts():
    return [
        section("5", "Conceitos fundamentais"),
        item("<b>Caso de uso</b>: classe que orquestra um fluxo de neg\u00f3cio, como o login, sem conhecer detalhes de HTTP."),
        item("<b>Porta e adaptador</b>: a aplica\u00e7\u00e3o declara a interface; a infraestrutura fornece a implementa\u00e7\u00e3o."),
        item("<b>Record</b>: classe imut\u00e1vel com construtor e acessores gerados, usada nos comandos e nas respostas."),
        item("<b>BCrypt</b>: fun\u00e7\u00e3o de hash lenta e com salt embutido; a compara\u00e7\u00e3o \u00e9 feita por matches, nunca por equals (PROVOS; MAZI\u00c8RES, 1999)."),
        item("<b>JWT</b>: token no formato cabe\u00e7alho.payload.assinatura; o payload \u00e9 leg\u00edvel, logo n\u00e3o recebe dados sens\u00edveis."),
        item("<b>SecurityContextHolder</b>: armazena a autentica\u00e7\u00e3o da requisi\u00e7\u00e3o atual e alimenta o @AuthenticationPrincipal."),
        item("<b>AuthenticationEntryPoint e AccessDeniedHandler</b>: respondem 401 e 403 fora do fluxo normal do MVC."),
        item("<b>Migra\u00e7\u00e3o versionada</b>: V1 cria o schema, V2 insere dados de card\u00e1pio e estoque e V3 insere usu\u00e1rios; migra\u00e7\u00f5es aplicadas n\u00e3o s\u00e3o editadas."),
    ]


def common_errors():
    return [
        section("6", "Erros comuns e armadilhas"),
        item("Emitir JWT sem declarar o algoritmo no cabe\u00e7alho: o Nimbus tenta RS256 e falha com \u201cFailed to select a JWK signing key\u201d."),
        item("Anotar o filtro JWT como componente: ele passa a rodar duas vezes, uma no servlet e outra na cadeia de seguran\u00e7a."),
        item("Usar segredo com menos de 32 bytes em HS256: a chave fica fraca; aqui a aplica\u00e7\u00e3o falha na inicializa\u00e7\u00e3o."),
        item("Comparar senha com equals: exp\u00f5e o hash e ignora o salt; \u00e9 o padr\u00e3o que User.authenticate tinha antes."),
        item("Esquecer de liberar /auth/login: o pr\u00f3prio login responde 401 e nenhum token \u00e9 emitido."),
        item("Devolver a entidade com passwordHash na resposta: vaza dado sens\u00edvel; por isso existem AuthenticatedUser e UserSummary."),
        item("Misturar encoders: hash gerado pelo DelegatingPasswordEncoder tem prefixo {bcrypt} e n\u00e3o confere com BCryptPasswordEncoder puro."),
    ]


def exercises():
    return [
        section("7", "Exerc\u00edcios de fixa\u00e7\u00e3o"),
        p(
            "<b>Exerc\u00edcio 1.</b> Um usu\u00e1rio inativo envia a senha errada para o login. A resposta "
            "\u00e9 401 ou 403? Justifique com base na ordem das verifica\u00e7\u00f5es."
        ),
        p(
            "<i>Gabarito:</i> 401. A senha \u00e9 verificada antes da checagem de inatividade; assim, "
            "ningu\u00e9m descobre o status da conta sem conhecer a credencial. O 403 s\u00f3 aparece "
            "quando a senha est\u00e1 correta."
        ),
        p(
            "<b>Exerc\u00edcio 2.</b> Remova a anota\u00e7\u00e3o a seguir do SecurityConfig e explique "
            "o efeito ao iniciar a aplica\u00e7\u00e3o:"
        ),
        p(mono("@EnableConfigurationProperties(JwtProperties.class)"), LIST_ITEM),
        p(
            "<i>Gabarito:</i> as propriedades deixam de ser um bean; a inje\u00e7\u00e3o em "
            + mono("JwtTokenIssuer") + " e nos m\u00e9todos que criam encoder e decoder falha e o contexto "
            "n\u00e3o sobe. Se a classe fosse instanciada manualmente, o segredo seria nulo e o m\u00e9todo "
            + mono("hmacKey") + " lan\u00e7aria IllegalStateException."
        ),
        p(
            "<b>Exerc\u00edcio 3.</b> Como provar, com os testes existentes, que um token expirado "
            "recebe 401?"
        ),
        p(
            "<i>Gabarito:</i> configurar " + mono("security.jwt.expiration-seconds=0") + " nas "
            "propriedades do teste de integra\u00e7\u00e3o e chamar /auth/me com o token rec\u00e9m-emitido. O "
            "decoder valida o claim exp, o filtro limpa o contexto e o entry point responde 401."
        ),
    ]


def checklist():
    return [
        section("8", "Checklist de autoavalia\u00e7\u00e3o"),
        item("[   ] Sei explicar por que a mensagem de erro do login \u00e9 igual para e-mail inexistente e senha errada.", ""),
        item("[   ] Sei diferenciar AuthenticationEntryPoint (401) de AccessDeniedHandler (403).", ""),
        item("[   ] Sei dizer por que o cabe\u00e7alho JWS com HS256 \u00e9 obrigat\u00f3rio neste projeto.", ""),
        item("[   ] Sei explicar por que o filtro JWT n\u00e3o \u00e9 um @Component.", ""),
        item("[   ] Sei dizer o efeito de anyRequest().authenticated() em endpoints futuros.", ""),
        item("[   ] Sei explicar por que o hash do seed n\u00e3o pode ser editado manualmente.", ""),
        item("[   ] Sei dizer o que AuthenticatedUser protege e de onde ele vem.", ""),
        item("[   ] Sei explicar por que o handler gen\u00e9rico registra o stack trace apenas no log.", ""),
    ]


def conclusion():
    return [
        section("9", "Considera\u00e7\u00f5es finais e pr\u00f3ximos passos"),
        p(
            "A Etapa 2 fecha a porta de entrada da API com quatro controles concretos: senha "
            "armazenada apenas como hash BCrypt, token JWT assinado com validade curta, filtro que "
            "protege todas as rotas e erro padronizado em qualquer falha. Os 20 testes automatizados "
            "cobrem o fluxo positivo e os cen\u00e1rios negativos de credencial, valida\u00e7\u00e3o, "
            "inativa\u00e7\u00e3o e token adulterado."
        ),
        *table(
            "6",
            "Resultado da su\u00edte de testes",
            ["Classe de teste", "Casos", "O que comprova"],
            [
                ["LoginServiceTest", "5", "Regras de autentica\u00e7\u00e3o isoladas da infraestrutura"],
                ["AuthControllerIntegrationTest", "8", "Login e /auth/me sobre HTTP real, com filtro e erros"],
                ["OrderPersistenceIntegrationTest", "5", "Persist\u00eancia do fluxo de pedido (Etapa 1)"],
                ["SchemaDdlTest", "1", "Migrations V1, V2 e V3 aplic\u00e1veis em banco limpo"],
                ["RdnApiApplicationTests", "1", "Contexto Spring sobe com a configura\u00e7\u00e3o atual"],
            ],
            [6.4 * cm, 1.8 * cm, 7.8 * cm],
        ),
        p(
            "A pr\u00f3xima etapa recomendada \u00e9 a API de pedidos: cria\u00e7\u00e3o com valida\u00e7\u00e3o de "
            "estoque (conflito 409), canal de pedido obrigat\u00f3rio, listagem com filtros por canal e "
            "status, pagina\u00e7\u00e3o e atualiza\u00e7\u00e3o de status pela m\u00e1quina de estados j\u00e1 "
            "existente no dom\u00ednio. Depois dela, o pagamento simulado fecha o fluxo cr\u00edtico "
            "exigido pelo roteiro."
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
            "BRASIL. <b>Lei n\u00ba 13.709, de 14 de agosto de 2018</b>. Lei Geral de Prote\u00e7\u00e3o de "
            "Dados Pessoais (LGPD). Bras\u00edlia, DF: Presid\u00eancia da Rep\u00fablica, 2018.",
            REFERENCE,
        ),
        p(
            "FIELDING, Roy Thomas. <b>Architectural styles and the design of network-based "
            "software architectures</b>. 2000. Tese (Doutorado) \u2014 University of California, "
            "Irvine, 2000.",
            REFERENCE,
        ),
        p(
            "JONES, Michael; BRADLEY, John; SAKIMURA, Natasha. <b>JSON Web Token (JWT)</b>: RFC 7519. "
            "Internet Engineering Task Force, 2015. Dispon\u00edvel em: "
            "https://www.rfc-editor.org/rfc/rfc7519. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "OWASP FOUNDATION. <b>Password Storage Cheat Sheet</b>. Dispon\u00edvel em: "
            "https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html. "
            "Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "PROVOS, Niels; MAZI\u00c8RES, David. <b>A future-adaptable password scheme</b>. In: "
            "USENIX ANNUAL TECHNICAL CONFERENCE, 1999, Monterey. Proceedings [...]. Berkeley: "
            "USENIX Association, 1999.",
            REFERENCE,
        ),
        p(
            "SPRING. <b>Spring Boot Reference Documentation</b>. Dispon\u00edvel em: "
            "https://docs.spring.io/spring-boot/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
        p(
            "SPRING. <b>Spring Security Reference</b>. Dispon\u00edvel em: "
            "https://docs.spring.io/spring-security/reference/. Acesso em: 12 nov. 2026.",
            REFERENCE,
        ),
    ]


# ------------------------------------------------------------------------ main

def build():
    doc = AbntDoc(OUTPUT)
    story = []
    story += cover()
    story += summary()
    story += introduction()
    story += architecture()
    story += security_overview()
    story += login_service()
    story += security_config()
    story += token_issuer()
    story += auth_filter()
    story += exception_handler()
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
