#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Gera os diagramas do relatorio final em PNG usando Pillow.

Uso:
    python tools/render_diagrams.py

Os arquivos sao gravados em docs/diagrams/. O script nao depende de PlantUML,
Graphviz, Node ou JDK: apenas Pillow, que ja e usado pelo ReportLab.
"""

import math
import os

from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = os.path.join("docs", "diagrams")

BG = "#FFFFFF"
INK = "#1F2937"
MUTED = "#4B5563"
ACCENT = "#1D4ED8"
ACCENT_SOFT = "#EFF6FF"
EDGE = "#93C5FD"
AMBER_FILL = "#FEF3C7"
AMBER_EDGE = "#D97706"
GREEN_FILL = "#ECFDF5"
GREEN_EDGE = "#10B981"
GRAY_FILL = "#F9FAFB"
GRAY_EDGE = "#D1D5DB"

_FONT_CACHE = {}


def load_font(size, bold=False):
    key = (size, bold)
    if key in _FONT_CACHE:
        return _FONT_CACHE[key]

    candidates = [
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold
        else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/Library/Fonts/Arial Bold.ttf" if bold else "/Library/Fonts/Arial.ttf",
    ]
    for path in candidates:
        if os.path.exists(path):
            font = ImageFont.truetype(path, size)
            _FONT_CACHE[key] = font
            return font

    try:
        font = ImageFont.load_default(size=size)
    except TypeError:
        font = ImageFont.load_default()

    _FONT_CACHE[key] = font
    return font


class Canvas:
    def __init__(self, width, height, title, subtitle=None):
        self.width = width
        self.height = height
        self.image = Image.new("RGB", (width, height), BG)
        self.d = ImageDraw.Draw(self.image)
        self.d.text((60, 36), title, font=load_font(42, True), fill=ACCENT, anchor="la")
        if subtitle:
            self.d.text((60, 98), subtitle, font=load_font(23), fill=MUTED, anchor="la")

    # ---------------------------------------------------------------- primitivas
    def wrap(self, text, font, max_width):
        words = text.split()
        lines = []
        current = ""
        for word in words:
            candidate = (current + " " + word).strip()
            if self.d.textlength(candidate, font=font) <= max_width:
                current = candidate
            else:
                if current:
                    lines.append(current)
                current = word
        if current:
            lines.append(current)
        return lines

    def line(self, p1, p2, color=INK, width=3, dashed=False):
        if not dashed:
            self.d.line([p1, p2], fill=color, width=width)
            return

        dx = p2[0] - p1[0]
        dy = p2[1] - p1[1]
        distance = math.hypot(dx, dy)
        if distance == 0:
            return

        ux, uy = dx / distance, dy / distance
        step, dash = 26, 15
        position = 0
        while position < distance:
            end = min(position + dash, distance)
            self.d.line(
                [(p1[0] + ux * position, p1[1] + uy * position),
                 (p1[0] + ux * end, p1[1] + uy * end)],
                fill=color, width=width,
            )
            position += step

    def label(self, x, y, text, size=19, bg=BG, fill=INK, bold=False):
        font = load_font(size, bold)
        box = self.d.textbbox((x, y), text, font=font, anchor="mm")
        self.d.rectangle([box[0] - 4, box[1] - 2, box[2] + 4, box[3] + 2], fill=bg)
        self.d.text((x, y), text, font=font, fill=fill, anchor="mm")

    def arrow(self, p1, p2, text=None, dashed=False, color=INK, width=3, label_dy=-18, size=19):
        self.line(p1, p2, color=color, width=width, dashed=dashed)

        angle = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
        length, half = 20, 9
        x, y = p2
        head = [
            (x, y),
            (x - length * math.cos(angle) + half * math.sin(angle),
             y - length * math.sin(angle) - half * math.cos(angle)),
            (x - length * math.cos(angle) - half * math.sin(angle),
             y - length * math.sin(angle) + half * math.cos(angle)),
        ]
        self.d.polygon(head, fill=color)

        if text:
            mx = (p1[0] + p2[0]) / 2
            my = (p1[1] + p2[1]) / 2 + label_dy
            self.label(mx, my, text, size=size)

    def polyline(self, points, color=INK, width=3, dashed=False):
        for start, end in zip(points, points[1:]):
            self.line(start, end, color=color, width=width, dashed=dashed)

    def box(self, x, y, width, title, lines, header=ACCENT, fill=ACCENT_SOFT, edge=EDGE,
            line_size=21, header_size=26):
        height = 78 + len(lines) * 31 + 14
        self.d.rounded_rectangle([x, y, x + width, y + height], radius=14, fill=fill,
                                 outline=edge, width=3)
        self.d.rounded_rectangle([x, y, x + width, y + 58], radius=14, fill=header, outline=header)
        self.d.rectangle([x, y + 38, x + width, y + 58], fill=header)
        self.d.text((x + width / 2, y + 29), title, font=load_font(header_size, True),
                    fill="#FFFFFF", anchor="mm")

        ty = y + 76
        for line in lines:
            self.d.text((x + 16, ty), line, font=load_font(line_size), fill=INK, anchor="la")
            ty += 31
        return height

    def ellipse(self, cx, cy, width, height, text, fill=AMBER_FILL, edge=AMBER_EDGE, size=22):
        self.d.ellipse([cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2],
                       fill=fill, outline=edge, width=3)
        self.d.multiline_text((cx, cy), text, font=load_font(size), fill=INK, anchor="mm",
                              align="center", spacing=4)

    def actor(self, cx, cy, label):
        self.d.ellipse([cx - 18, cy - 74, cx + 18, cy - 38], outline=INK, width=4)
        self.d.line([(cx, cy - 38), (cx, cy + 8)], fill=INK, width=4)
        self.d.line([(cx - 42, cy - 16), (cx + 42, cy - 16)], fill=INK, width=4)
        self.d.line([(cx, cy + 8), (cx - 32, cy + 58)], fill=INK, width=4)
        self.d.line([(cx, cy + 8), (cx + 32, cy + 58)], fill=INK, width=4)
        self.d.text((cx, cy + 92), label, font=load_font(24, True), fill=INK, anchor="mm")

    def note(self, x, y, width, text, fill=GRAY_FILL, edge=GRAY_EDGE, size=20):
        font = load_font(size)
        lines = self.wrap(text, font, width - 30)
        height = 24 + len(lines) * 29
        self.d.rounded_rectangle([x, y, x + width, y + height], radius=10, fill=fill,
                                 outline=edge, width=2)
        ty = y + 14
        for line in lines:
            self.d.text((x + 15, ty), line, font=font, fill=MUTED, anchor="la")
            ty += 29

    def save(self, filename):
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        path = os.path.join(OUTPUT_DIR, filename)
        self.image.save(path, "PNG", optimize=True)
        print("gerado: %s" % path)


# ---------------------------------------------------------------------- figura 1
def use_cases():
    c = Canvas(2400, 1850, "Figura 1 - Casos de uso",
               "Atores e funcionalidades principais do RDN-API")

    c.d.rounded_rectangle([520, 190, 1880, 1720], radius=34, outline="#9CA3AF", width=4)
    c.d.text((1200, 225), "RDN-API", font=load_font(28, True), fill="#6B7280", anchor="ma")

    cases = {
        "autenticar": (780, 370, "Autenticar"),
        "cardapio": (780, 570, "Consultar cardapio"),
        "promocoes": (780, 770, "Consultar promocoes"),
        "pedido": (1200, 470, "Realizar pedido"),
        "status": (1200, 710, "Acompanhar/\natualizar status"),
        "cancelar": (1200, 950, "Cancelar pedido"),
        "pagamento": (1620, 470, "Solicitar pagamento"),
        "consentimento": (1620, 710, "Consentir fidelidade"),
        "resgate": (1620, 950, "Resgatar pontos"),
        "auditoria": (1200, 1240, "Consultar auditoria"),
        "cadastro": (1620, 1240, "Cadastrar usuario"),
    }

    actors = {
        "cliente": (240, 530, "Cliente"),
        "atendente": (240, 1000, "Atendente"),
        "cozinha": (240, 1430, "Cozinha"),
        "admin": (2140, 530, "Administrador"),
        "gateway": (2140, 1120, "Gateway"),
    }

    links = [
        ("cliente", "autenticar"), ("cliente", "cardapio"), ("cliente", "promocoes"),
        ("cliente", "pedido"), ("cliente", "status"), ("cliente", "cancelar"),
        ("cliente", "pagamento"), ("cliente", "consentimento"), ("cliente", "resgate"),
        ("atendente", "autenticar"), ("atendente", "pedido"), ("atendente", "status"),
        ("atendente", "cancelar"), ("atendente", "promocoes"),
        ("cozinha", "autenticar"), ("cozinha", "status"),
        ("admin", "autenticar"), ("admin", "cadastro"), ("admin", "auditoria"),
        ("admin", "promocoes"),
        ("gateway", "pagamento"),
    ]

    for actor, case in links:
        ax, ay, _ = actors[actor]
        cx, cy, _ = cases[case]
        c.line((ax, ay), (cx, cy), color="#CBD5E1", width=2)

    for cx, cy, text in cases.values():
        c.ellipse(cx, cy, 360, 130, text)

    for ax, ay, label in actors.values():
        c.actor(ax, ay, label)

    c.note(560, 1520, 820,
           "Realizar pedido inclui validar estoque e aplicar promocao. "
           "Solicitar pagamento usa o Gateway mock e exige Idempotency-Key.")
    c.note(1420, 1520, 420,
           "Cadastro de usuario e restrito ao perfil ADMIN.")

    c.save("01-casos-de-uso.png")


# ---------------------------------------------------------------------- figura 2
def der_core():
    c = Canvas(2400, 1400, "Figura 2 - DER: nucleo de pedidos e pagamentos",
               "Chaves, cardinalidades e restricoes principais")

    users_x, users_y, w = 100, 250, 540
    orders_x, orders_y = 900, 250
    items_x, items_y = 1700, 250
    payments_x, payments_y = 900, 800
    promo_x, promo_y = 1700, 800

    c.arrow((users_x + w, 400), (orders_x, 400), "1 : N", label_dy=-18)
    c.arrow((orders_x + 560, 400), (items_x, 400), "1 : N", label_dy=-18)
    c.arrow((orders_x + 280, 660), (payments_x + 280, 800), "1 : N", label_dy=-18)
    c.arrow((promo_x, 960), (orders_x + 560, 620), "0..1 : N", label_dy=-18)

    c.box(users_x, users_y, w, "users", [
        "id UUID PK",
        "full_name VARCHAR(120)",
        "email VARCHAR(160) UK",
        "password_hash VARCHAR(100)",
        "role VARCHAR(20)",
        "is_active BOOLEAN",
    ])

    c.box(orders_x, orders_y, 560, "orders", [
        "id UUID PK",
        "customer_id UUID FK",
        "branch_id UUID FK",
        "promotion_id UUID FK",
        "origin_channel VARCHAR(20)",
        "status VARCHAR(30)",
        "total_amount NUMERIC",
        "discount_amount NUMERIC",
        "idempotency_key VARCHAR UK",
        "created_at TIMESTAMP",
    ])

    c.box(items_x, items_y, 560, "order_items", [
        "id UUID PK",
        "order_id UUID FK",
        "menu_item_id UUID FK",
        "quantity INTEGER",
        "unit_price NUMERIC",
        "subtotal NUMERIC",
    ])

    c.box(payments_x, payments_y, 560, "payments", [
        "id UUID PK",
        "order_id UUID FK",
        "amount NUMERIC",
        "method VARCHAR(20)",
        "status VARCHAR(20)",
        "gateway_transaction_id VARCHAR",
        "idempotency_key VARCHAR UK",
        "requested_at TIMESTAMP",
        "processed_at TIMESTAMP",
    ])

    c.box(promo_x, promo_y, 560, "promotions", [
        "id UUID PK",
        "title VARCHAR(120)",
        "discount_type VARCHAR(20)",
        "discount_value NUMERIC",
        "start_date TIMESTAMP",
        "end_date TIMESTAMP",
        "min_order_value NUMERIC",
        "is_active BOOLEAN",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)

    c.note(100, 760, 540,
           "Pedido guarda o preco unitario copiado e a promocao aplicada. "
           "Cada tentativa de pagamento e uma linha em payments.")
    c.note(100, 1050, 540,
           "menu_items, branches e inventory aparecem na Figura 3. "
           "O canal do pedido e o enum SalesChannel.")

    c.save("02-der-nucleo.png")


# ---------------------------------------------------------------------- figura 3
def der_support():
    c = Canvas(2520, 1900, "Figura 3 - DER: catalogo, estoque, fidelidade e auditoria",
               "Entidades de apoio e chaves estrangeiras")

    branches = (100, 250, 520)
    menus = (700, 250, 520)
    menu_items = (1300, 250, 520)
    inventory = (1900, 250, 520)
    users = (100, 800, 520)
    accounts = (700, 800, 520)
    transactions = (1300, 800, 560)
    movements = (1900, 800, 520)
    sessions = (700, 1350, 520)
    audit = (1300, 1350, 560)

    c.arrow((branches[0] + 520, 350), (menus[0], 350), "1 : 0..1")
    c.arrow((menus[0] + 520, 350), (menu_items[0], 350), "1 : N")
    c.arrow((menu_items[0] + 520, 350), (inventory[0], 350), "1 : N")
    c.polyline([(branches[0] + 260, 480), (branches[0] + 260, 700), (inventory[0] + 260, 700),
                (inventory[0] + 260, 500)], color=INK, width=3)
    c.label(inventory[0] - 120, 690, "1 : N")
    c.arrow((inventory[0] + 260, 500), (movements[0] + 260, 800), "1 : N", label_dy=18)
    c.arrow((users[0] + 520, 900), (accounts[0], 900), "1 : 0..1")
    c.arrow((accounts[0] + 520, 900), (transactions[0], 900), "1 : N")
    c.polyline([(users[0] + 260, 1020), (users[0] + 260, 1220), (sessions[0] + 260, 1220),
                (sessions[0] + 260, 1350)], color=INK, width=3)
    c.label(sessions[0] + 180, 1210, "1 : N")

    c.box(*branches, "branches", [
        "id UUID PK",
        "name VARCHAR(120)",
        "is_open BOOLEAN",
        "city / state / zip_code",
    ])
    c.box(*menus, "menus", [
        "id UUID PK",
        "branch_id UUID FK / UK",
        "title VARCHAR(120)",
        "is_active BOOLEAN",
    ])
    c.box(*menu_items, "menu_items", [
        "id UUID PK",
        "menu_id UUID FK",
        "name VARCHAR(120)",
        "price NUMERIC",
        "is_available BOOLEAN",
    ])
    c.box(*inventory, "inventory_items", [
        "id UUID PK",
        "branch_id UUID FK",
        "menu_item_id UUID FK",
        "quantity_on_hand NUMERIC",
        "minimum_threshold NUMERIC",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(*users, "users", [
        "id UUID PK",
        "email VARCHAR(160) UK",
        "role VARCHAR(20)",
        "is_active BOOLEAN",
    ])
    c.box(*accounts, "loyalty_accounts", [
        "id UUID PK",
        "customer_id UUID FK",
        "points_balance INTEGER",
        "consent_given BOOLEAN",
        "consent_timestamp TIMESTAMP",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(*transactions, "loyalty_transactions", [
        "id UUID PK",
        "loyalty_account_id UUID FK",
        "order_id UUID FK",
        "points INTEGER",
        "type VARCHAR(20)",
        "created_at TIMESTAMP",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(*movements, "inventory_movements", [
        "id UUID PK",
        "inventory_item_id UUID FK",
        "type VARCHAR(20)",
        "quantity NUMERIC",
        "occurred_at TIMESTAMP",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(*sessions, "auth_sessions", [
        "id UUID PK",
        "user_id UUID FK",
        "token_hash VARCHAR UK",
        "issued_at TIMESTAMP",
        "expires_at TIMESTAMP",
    ])
    c.box(*audit, "audit_logs", [
        "id UUID PK",
        "occurred_at TIMESTAMP",
        "actor_id UUID",
        "actor_email VARCHAR(160)",
        "actor_role VARCHAR(20)",
        "action VARCHAR(60)",
        "resource VARCHAR(40)",
        "resource_id UUID",
        "details / ip_address",
    ], header="#6B7280", fill=GRAY_FILL, edge=GRAY_EDGE)

    c.note(100, 1400, 520,
           "audit_logs guarda o ator como snapshot e nao tem FK para users. "
           "O saldo de fidelidade e conferido pela soma do extrato.")
    c.note(1300, 1720, 560,
           "Pontos assinados: EARNED positivo, REDEEMED/EXPIRED negativo.")

    c.save("03-der-apoio.png")


# ---------------------------------------------------------------------- figura 4
def class_diagram():
    c = Canvas(2520, 2050, "Figura 4 - Diagrama de classes do dominio",
               "Entidades ricas, value objects e enums (sem anotacoes JPA)")

    w = 520
    xs = [100, 720, 1340, 1960]
    ys = [240, 700, 1160, 1620]

    c.line((xs[0] + w, ys[0] + 110), (xs[1], ys[0] + 110), color="#CBD5E1")
    c.line((xs[1] + w, ys[0] + 110), (xs[2], ys[0] + 110), color="#CBD5E1")
    c.line((xs[0] + 260, ys[0] + 230), (xs[0] + 260, ys[1]), color="#CBD5E1")
    c.line((xs[1] + 260, ys[1] + 230), (xs[1] + 260, ys[0] + 260), color="#CBD5E1")
    c.line((xs[0] + w, ys[1] + 110), (xs[1], ys[1] + 110), color="#CBD5E1")
    c.line((xs[0] + w, ys[1] + 150), (xs[2], ys[1] + 150), color="#CBD5E1")
    c.line((xs[0] + 260, ys[2] + 210), (xs[0] + 260, ys[1] + 230), color="#CBD5E1")
    c.line((xs[2] + w, ys[2] + 110), (xs[3], ys[2] + 110), color="#CBD5E1")
    c.polyline([(xs[0] + 260, ys[3]), (xs[0] + 260, ys[3] - 140),
                (xs[2] + 260, ys[3] - 140), (xs[2] + 260, ys[2] + 210)],
               color="#CBD5E1", width=3, dashed=True)

    c.label(xs[1] + 50, ys[0] + 80, "1 : N")
    c.label(xs[2] + 50, ys[0] + 80, "1 : N")
    c.label(xs[0] + 300, ys[0] + 420, "1 : N")
    c.label(xs[0] + w + 50, ys[1] + 80, "1 : N")
    c.label(xs[2] - 30, ys[1] + 120, "1 : N")
    c.label(xs[1] + 300, ys[1] - 80, "N : 1")
    c.label(xs[1] + 50, ys[2] + 80, "1 : N")
    c.label(xs[3] - 50, ys[2] + 80, "1 : N")

    c.box(xs[0], ys[0], w, "User", [
        "- id : UUID",
        "- fullName : String",
        "- email : String",
        "- passwordHash : String",
        "- role : UserRole",
        "- isActive : Boolean",
        "+ isActive() : boolean",
    ])
    c.box(xs[1], ys[0], w, "Branch", [
        "- id : UUID",
        "- name : String",
        "- isOpen : Boolean",
        "- address : Address",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(xs[2], ys[0], w, "Menu", [
        "- id : UUID",
        "- title : String",
        "- isActive : Boolean",
        "+ isOpen() : boolean",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(xs[3], ys[0], w, "MenuItem", [
        "- id : UUID",
        "- name : String",
        "- price : BigDecimal",
        "- isAvailable : Boolean",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)

    c.box(xs[0], ys[1], w, "Order", [
        "- id : UUID",
        "- status : OrderStatus",
        "- originChannel : SalesChannel",
        "- totalAmount : BigDecimal",
        "- discountAmount : BigDecimal",
        "+ addItem() / calculateTotal()",
        "+ awaitPayment() / changeStatus()",
    ], header="#7C3AED", fill="#F5F3FF", edge="#C4B5FD")
    c.box(xs[1], ys[1], w, "OrderItem", [
        "- id : UUID",
        "- quantity : Integer",
        "- unitPrice : BigDecimal",
        "- subtotal : BigDecimal",
    ], header="#7C3AED", fill="#F5F3FF", edge="#C4B5FD")
    c.box(xs[2], ys[1], w, "Payment", [
        "- id : UUID",
        "- method : PaymentMethod",
        "- status : PaymentStatus",
        "- amount : BigDecimal",
        "- gatewayTransactionId : String",
        "+ approve() / decline() / fail()",
    ], header="#7C3AED", fill="#F5F3FF", edge="#C4B5FD")
    c.box(xs[3], ys[1], w, "Promotion", [
        "- id : UUID",
        "- title : String",
        "- discountType : DiscountType",
        "- discountValue : BigDecimal",
        "- startDate / endDate",
        "+ isValidFor() / applyDiscount()",
    ], header="#7C3AED", fill="#F5F3FF", edge="#C4B5FD")

    c.box(xs[0], ys[2], w, "InventoryItem", [
        "- id : UUID",
        "- quantityOnHand : BigDecimal",
        "- minimumThreshold : BigDecimal",
        "+ isBelowMinimum() : boolean",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(xs[1], ys[2], w, "InventoryMovement", [
        "- id : UUID",
        "- type : MovementType",
        "- quantity : BigDecimal",
        "- occurredAt : LocalDateTime",
    ], header=GREEN_EDGE, fill=GREEN_FILL, edge=GREEN_EDGE)
    c.box(xs[2], ys[2], w, "LoyaltyAccount", [
        "- id : UUID",
        "- pointsBalance : Integer",
        "- consentGiven : Boolean",
        "+ grantConsent() / revokeConsent()",
        "+ earn() / redeem()",
    ])
    c.box(xs[3], ys[2], w, "LoyaltyTransaction", [
        "- id : UUID",
        "- points : Integer",
        "- type : TransactionType",
        "- createdAt : LocalDateTime",
    ])

    c.box(xs[0], ys[3], w, "LoyaltyPolicy", [
        "+ pointsFor(amount) : int",
        "+ discountFor(points) : BigDecimal",
    ], header="#6B7280", fill=GRAY_FILL, edge=GRAY_EDGE)
    c.box(xs[1], ys[3], 1240, "Enums do dominio", [
        "SalesChannel: APP, TOTEM, BALCAO, PICKUP, WEB",
        "OrderStatus: AGUARDANDO_PAGAMENTO, RECEIVED, PREPARING, READY, DELIVERED, CANCELLED",
        "PaymentStatus / PaymentMethod / DiscountType / MovementType / TransactionType / UserRole",
    ], header="#6B7280", fill=GRAY_FILL, edge=GRAY_EDGE)

    c.note(100, 1880, 2200,
           "Visao resumida. Multiplicidades e chaves estrangeiras completas estao no DER "
           "(Figuras 2 e 3). Order referencia Branch, User e Promotion; User possui "
           "LoyaltyAccount. Address e um value object embutido em Branch.")

    c.save("04-classes-dominio.png")


# ---------------------------------------------------------------------- figura 5
def sequence():
    c = Canvas(2400, 1650, "Figura 5 - Sequencia do Fluxo A",
               "Pedido, pagamento mock e atualizacao de status")

    participants = [
        (180, "Cliente"),
        (450, "OrderController"),
        (750, "CreateOrderService"),
        (1050, "Catalog/\nInventory"),
        (1300, "OrderGateway"),
        (1550, "PaymentController"),
        (1800, "RequestPayment/\nProvider"),
        (2150, "AuditGateway"),
    ]

    for x, name in participants:
        lines = name.split("\n")
        height = 46 + len(lines) * 26
        c.d.rounded_rectangle([x - 120, 190, x + 120, 190 + height], radius=10,
                              fill=ACCENT_SOFT, outline=EDGE, width=3)
        c.d.multiline_text((x, 190 + height / 2), name, font=load_font(21, True),
                           fill=INK, anchor="mm", align="center", spacing=3)
        c.line((x, 190 + height), (x, 1450), color="#CBD5E1", width=2, dashed=True)

    messages = [
        (330, 180, 450, "POST /pedidos (canalPedido)", False),
        (390, 450, 750, "create(command)", False),
        (450, 750, 1050, "findBranch / findMenuItem", False),
        (510, 750, 1050, "reserve (lock)", False),
        (570, 750, 1300, "save(order)", False),
        (630, 750, 2150, "ORDER_CREATED", False),
        (690, 450, 180, "201 pedidoId", True),
        (780, 180, 1550, "POST /pagamentos + Idempotency-Key", False),
        (840, 1550, 1800, "request(...)", False),
        (900, 1800, 1800, "charge(...)", False),
        (960, 1800, 1800, "APPROVED / DECLINED / FAILED", True),
        (1020, 1800, 1300, "update status", False),
        (1080, 1800, 2150, "PAYMENT_*", False),
        (1140, 1550, 180, "201 / 200 replay", True),
    ]

    c.d.rounded_rectangle([1460, 880, 2320, 1120], radius=12, fill="#FFFBEB",
                          outline=AMBER_EDGE, width=2)
    c.d.text((1480, 895), "alt [APROVADO | RECUSADO | FALHA]", font=load_font(20, True),
             fill="#92400E", anchor="la")

    for y, x1, x2, text, dashed in messages:
        if x1 == x2:
            continue
        c.arrow((x1, y), (x2, y), text=text, dashed=dashed, label_dy=-16, size=18)

    c.d.text((1800, 920), "charge()", font=load_font(18), fill=INK, anchor="mm")
    c.d.text((1800, 990), "resultado", font=load_font(18), fill=INK, anchor="mm")

    c.note(100, 1330, 1250,
           "Aprovado: pedido vai para RECEIVED e acumula pontos se houver consentimento. "
           "Recusado: CANCELLED. Falha: permanece AGUARDANDO_PAGAMENTO para nova tentativa.")
    c.note(1400, 1330, 900,
           "Replay com a mesma Idempotency-Key devolve 200 com a tentativa original, sem nova cobranca.")

    c.save("05-sequencia-fluxo-a.png")


def main():
    use_cases()
    der_core()
    der_support()
    class_diagram()
    sequence()


if __name__ == "__main__":
    main()
