#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Renderizador ABNT (NBR 14724) reutilizavel para os relatorios das etapas.

Formatacao aplicada:
- A4, margens 3 cm (esquerda/superior) e 2 cm (direita/inferior)
- Times New Roman 12 pt, espacamento 1,5, texto justificado
- Recuo de primeira linha 1,25 cm
- Capa, sumario com numero de paginas, secoes numeradas e referencias
- Numero de pagina no canto superior direito (suprimido na capa)

Este modulo concentra a formatacao; o conteudo de cada etapa fica nos
arquivos abnt_pdf_etapa3.py e abnt_pdf_etapa4.py.
"""

import os

from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.utils import ImageReader
from reportlab.platypus import (
    BaseDocTemplate,
    Frame,
    Image,
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

AUTHOR = "Felipe D'Paula"
YEAR = "2026"
CITY = "Caxias do Sul - RS"
PROJECT = "PROJETO MULTIDISCIPLINAR"
TRACK = "TRILHA BACK-END"

# Preencher antes de gerar o PDF final de submissao.
INSTITUTION = "[INSTITUICAO DE ENSINO]"
COURSE = "[CURSO]"
RU = "RU: [PREENCHER]"
PROFESSOR = "Prof. Me. Luciane Yanase Kanashiro"

BODY = ParagraphStyle(
    "body", fontName="Times-Roman", fontSize=12, leading=18,
    alignment=TA_JUSTIFY, firstLineIndent=1.25 * cm, spaceAfter=0,
)
SECTION = ParagraphStyle(
    "section", fontName="Times-Bold", fontSize=12, leading=18,
    alignment=TA_LEFT, spaceBefore=18, spaceAfter=6, keepWithNext=1,
)
SUBSECTION = ParagraphStyle(
    "subsection", fontName="Times-Bold", fontSize=12, leading=18,
    alignment=TA_LEFT, spaceBefore=12, spaceAfter=3, keepWithNext=1,
)
LIST_ITEM = ParagraphStyle(
    "list_item", parent=BODY, firstLineIndent=0, leftIndent=1.25 * cm, spaceAfter=6,
)
CODE = ParagraphStyle(
    "code", fontName="Courier", fontSize=8.5, leading=11,
    alignment=TA_LEFT, leftIndent=1.0 * cm, spaceBefore=6, spaceAfter=6,
    backColor=colors.HexColor("#F4F4F4"), borderPadding=4,
)
CAPTION = ParagraphStyle(
    "caption", fontName="Times-Roman", fontSize=10, leading=14,
    alignment=TA_LEFT, spaceBefore=8, spaceAfter=3,
)
TABLE_TEXT = ParagraphStyle(
    "table_text", fontName="Times-Roman", fontSize=10, leading=13, alignment=TA_LEFT,
)
TABLE_HEAD = ParagraphStyle(
    "table_head", fontName="Times-Bold", fontSize=10, leading=13, alignment=TA_LEFT,
)
TABLE_TEXT_SMALL = ParagraphStyle(
    "table_text_small", fontName="Times-Roman", fontSize=8.5, leading=11, alignment=TA_LEFT,
)
TABLE_HEAD_SMALL = ParagraphStyle(
    "table_head_small", fontName="Times-Bold", fontSize=8.5, leading=11, alignment=TA_LEFT,
)
TABLE_SOURCE = ParagraphStyle(
    "table_source", fontName="Times-Roman", fontSize=9, leading=12, alignment=TA_LEFT,
    spaceAfter=8,
)
COVER_LINE = ParagraphStyle(
    "cover_line", fontName="Times-Bold", fontSize=12, leading=18, alignment=TA_CENTER,
)
COVER_TITLE = ParagraphStyle(
    "cover_title", fontName="Times-Bold", fontSize=16, leading=22, alignment=TA_CENTER,
)
COVER_SUBTITLE = ParagraphStyle(
    "cover_subtitle", fontName="Times-Roman", fontSize=12, leading=18, alignment=TA_CENTER,
)
TOC_1 = ParagraphStyle(
    "toc1", fontName="Times-Bold", fontSize=12, leading=18, leftIndent=0,
)
TOC_2 = ParagraphStyle(
    "toc2", fontName="Times-Roman", fontSize=11, leading=16, leftIndent=1.0 * cm,
)


class _AbntDoc(BaseDocTemplate):
    """DocTemplate com numeracao de pagina e marcacao do sumario."""

    def __init__(self, filename, title):
        super().__init__(
            filename, pagesize=A4,
            leftMargin=MARGIN_LEFT, rightMargin=MARGIN_RIGHT,
            topMargin=MARGIN_TOP, bottomMargin=MARGIN_BOTTOM,
            title=title, author=AUTHOR, creator="tools/abnt_doc.py",
        )
        frame = Frame(
            MARGIN_LEFT, MARGIN_BOTTOM, TEXT_WIDTH,
            PAGE_H - MARGIN_TOP - MARGIN_BOTTOM, id="body",
        )
        self.addPageTemplates([
            PageTemplate(id="page", frames=[frame], onPage=self._decorate),
        ])
        self.toc = TableOfContents()
        self.toc.levelStyles = [TOC_1, TOC_2]
        self.toc.dotsMinLevel = 0

    def _decorate(self, canvas, doc):
        if doc.page == 1:
            return
        canvas.saveState()
        canvas.setFont("Times-Roman", 10)
        canvas.drawRightString(PAGE_W - MARGIN_RIGHT, PAGE_H - 1.5 * cm, str(doc.page))
        canvas.restoreState()

    def afterFlowable(self, flowable):
        if not isinstance(flowable, Paragraph):
            return
        level = getattr(flowable, "_toc_level", None)
        if level is None:
            return
        text = flowable.getPlainText()
        self.notify("TOCEntry", (level - 1, text, self.page))


class AbntReport:
    """Fachada de conteudo: capa, sumario, secoes, tabelas e codigo."""

    def __init__(self, output_path, title, subtitle):
        self.output_path = output_path
        self.title = title
        self.subtitle = subtitle
        self.story = []
        self._section = 0
        self._subsection = 0
        self._table = 0
        self._figure = 0

    # ---------------------------------------------------------------- estrutura
    def cover(self):
        self.story += [
            Paragraph(escape(INSTITUTION), COVER_LINE),
            Paragraph(escape(COURSE), COVER_SUBTITLE),
            Paragraph(escape(PROJECT), COVER_LINE),
            Paragraph(escape(TRACK), COVER_SUBTITLE),
            Paragraph(escape(PROFESSOR), COVER_SUBTITLE),
            Spacer(1, 1.6 * cm),
            Paragraph(escape(AUTHOR), COVER_SUBTITLE),
            Paragraph(escape(RU), COVER_SUBTITLE),
            Spacer(1, 2.4 * cm),
            Paragraph(escape(self.title), COVER_TITLE),
            Spacer(1, 0.5 * cm),
            Paragraph(escape(self.subtitle), COVER_SUBTITLE),
            Spacer(1, 4.2 * cm),
            Paragraph(escape(CITY), COVER_SUBTITLE),
            Paragraph(escape(YEAR), COVER_SUBTITLE),
            PageBreak(),
        ]
        return self

    def summary(self):
        heading = Paragraph("<b>SUM\u00c1RIO</b>", SECTION)
        self.story += [heading, Spacer(1, 0.4 * cm), self._pending_toc(), PageBreak()]
        return self

    def _pending_toc(self):
        toc = TableOfContents()
        toc.levelStyles = [TOC_1, TOC_2]
        toc.dotsMinLevel = 0
        return toc

    def h1(self, text, numbered=True):
        if numbered:
            self._section += 1
            self._subsection = 0
            label = "%d %s" % (self._section, escape(text))
        else:
            label = escape(text)

        node = Paragraph(label, SECTION)
        node._toc_level = 1
        self.story.append(node)
        return self

    def h2(self, text, numbered=True):
        if numbered:
            self._subsection += 1
            label = "%d.%d %s" % (self._section, self._subsection, escape(text))
        else:
            label = escape(text)

        node = Paragraph(label, SUBSECTION)
        node._toc_level = 2
        self.story.append(node)
        return self

    def p(self, text):
        self.story.append(Paragraph(escape(text), BODY))
        return self

    def bullets(self, items):
        for item in items:
            self.story.append(Paragraph("\u2022 " + escape(item), LIST_ITEM))
        return self

    def code(self, snippet):
        self.story.append(KeepTogether(Preformatted(snippet.strip("\n"), CODE)))
        return self

    def table(self, caption, headers, rows, widths=None, compact=False, source=None):
        self._table += 1
        self.story.append(
            Paragraph("Tabela %d \u2014 %s" % (self._table, escape(caption)), CAPTION),
        )

        head_style = TABLE_HEAD_SMALL if compact else TABLE_HEAD
        text_style = TABLE_TEXT_SMALL if compact else TABLE_TEXT

        data = [[Paragraph("<b>%s</b>" % escape(h), head_style) for h in headers]]
        for row in rows:
            data.append([Paragraph(escape(str(cell)), text_style) for cell in row])

        if widths is None:
            widths = [TEXT_WIDTH / len(headers)] * len(headers)
        else:
            widths = [TEXT_WIDTH * fraction for fraction in widths]

        table = LongTable(data, colWidths=widths, repeatRows=1)
        table.setStyle(TableStyle([
            ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#888888")),
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EDEDED")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 4 if compact else 5),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4 if compact else 5),
            ("TOPPADDING", (0, 0), (-1, -1), 3 if compact else 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 3 if compact else 4),
        ]))
        self.story.append(table)
        self.story.append(Paragraph(
            escape(source or "Fonte: elaborado pelo autor (2026)."), TABLE_SOURCE,
        ))
        self.story.append(Spacer(1, 0.3 * cm))
        return self

    def figure(self, path, caption, source="Fonte: elaborado pelo autor (2026).",
               max_height=20 * cm):
        if not os.path.exists(path):
            raise FileNotFoundError(
                "Figura nao encontrada: %s. Rode python tools/render_diagrams.py." % path
            )

        reader = ImageReader(path)
        width_px, height_px = reader.getSize()
        width = TEXT_WIDTH
        height = width * height_px / width_px
        if height > max_height:
            height = max_height
            width = height * width_px / height_px

        self._figure += 1
        image = Image(path, width=width, height=height)
        image.hAlign = "CENTER"
        self.story.append(KeepTogether([
            Paragraph("Figura %d \u2014 %s" % (self._figure, escape(caption)), CAPTION),
            image,
            Paragraph(escape(source), TABLE_SOURCE),
            Spacer(1, 0.4 * cm),
        ]))
        return self

    def references(self, lines):
        self.story.append(PageBreak())
        self.h1("REFER\u00caNCIAS", numbered=False)
        for line in lines:
            self.story.append(Paragraph(escape(line), LIST_ITEM))
        return self

    # ------------------------------------------------------------------ saida
    def save(self, extra_pages_builder=None):
        document = _AbntDoc(self.output_path, self.title)

        # Substitui o TOC provisorio pelo definitivo antes do build.
        for index, flowable in enumerate(self.story):
            if isinstance(flowable, TableOfContents):
                self.story[index] = document.toc

        if extra_pages_builder is not None:
            extra_pages_builder(self)

        document.multiBuild(self.story)
        return self.output_path
