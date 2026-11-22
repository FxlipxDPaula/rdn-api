import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerador de PDF sem dependencias externas.
 *
 * <p>Le o HTML de docs/explicacao-projeto.html (estrutura controlada: h1..h4, p,
 * li, pre, tr) e escreve um PDF A4 com fontes base Helvetica/Helvetica-Bold/
 * Courier. Nao e um renderizador HTML generico: cobre exatamente as tags usadas
 * no documento de explicacao.</p>
 *
 * <p>Uso: {@code java tools/PdfGen.java docs/explicacao-projeto.pdf}</p>
 */
public final class PdfGen {

    private static final float PAGE_W = 595f;
    private static final float PAGE_H = 842f;
    private static final float MARGIN_L = 50f;
    private static final float TEXT_W = PAGE_W - 100f;
    private static final float TOP_Y = 800f;
    private static final float BOTTOM_Y = 62f;

    private enum Kind {
        H1, H2, H3, H4, P, BULLET, NUMBERED, CODE, ROW
    }

    private record Block(Kind kind, String text, List<String> cells) {
    }

    public static void main(String[] args) throws IOException {
        Path htmlPath = Path.of("docs/explicacao-projeto.html");
        Path pdfPath = Path.of(args.length > 0 ? args[0] : "docs/explicacao-projeto.pdf");

        List<Block> blocks = parse(Files.readString(htmlPath, StandardCharsets.UTF_8));
        String pdf = render(blocks);

        Files.write(pdfPath, pdf.getBytes(StandardCharsets.ISO_8859_1));
        System.out.println("PDF gerado: " + pdfPath.toAbsolutePath());
        System.out.println("Blocos processados: " + blocks.size());
    }

    // ---------------------------------------------------------------- parsing

    private static final Pattern CELL = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>",
            Pattern.DOTALL);

    private static List<Block> parse(String src) {
        List<Block> blocks = new ArrayList<>();
        boolean inStyle = false;
        boolean inPre = false;
        boolean ordered = false;
        StringBuilder pre = new StringBuilder();
        int counter = 0;

        for (String rawLine : src.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("<style")) {
                inStyle = true;
                continue;
            }
            if (line.startsWith("</style")) {
                inStyle = false;
                continue;
            }
            if (inStyle) {
                continue;
            }

            if (inPre) {
                if (line.contains("</pre>")) {
                    pre.append(line, 0, line.indexOf("</pre>"));
                    blocks.add(new Block(Kind.CODE, trimCode(pre.toString()), null));
                    pre.setLength(0);
                    inPre = false;
                } else {
                    pre.append(line).append('\n');
                }
                continue;
            }

            if (line.startsWith("<pre")) {
                inPre = true;
                String rest = line.substring(line.indexOf('>') + 1);
                if (rest.contains("</pre>")) {
                    pre.append(rest, 0, rest.indexOf("</pre>"));
                    blocks.add(new Block(Kind.CODE, trimCode(pre.toString()), null));
                    pre.setLength(0);
                    inPre = false;
                } else if (!rest.isEmpty()) {
                    pre.append(rest).append('\n');
                }
                continue;
            }

            if (line.startsWith("<h1")) {
                blocks.add(new Block(Kind.H1, inner(line, "h1"), null));
                continue;
            }
            if (line.startsWith("<h2")) {
                blocks.add(new Block(Kind.H2, inner(line, "h2"), null));
                counter = 0;
                continue;
            }
            if (line.startsWith("<h3")) {
                blocks.add(new Block(Kind.H3, inner(line, "h3"), null));
                counter = 0;
                continue;
            }
            if (line.startsWith("<h4")) {
                blocks.add(new Block(Kind.H4, inner(line, "h4"), null));
                continue;
            }
            if (line.startsWith("<tr")) {
                List<String> cells = new ArrayList<>();
                Matcher matcher = CELL.matcher(line);
                while (matcher.find()) {
                    cells.add(clean(matcher.group(1)));
                }
                if (!cells.isEmpty()) {
                    blocks.add(new Block(Kind.ROW, null, cells));
                }
                continue;
            }
            if (line.startsWith("<ol")) {
                ordered = true;
                counter = 0;
                continue;
            }
            if (line.startsWith("<ul")) {
                ordered = false;
                continue;
            }
            if (line.startsWith("<li")) {
                String text = inner(line, "li");
                if (ordered) {
                    counter++;
                    blocks.add(new Block(Kind.NUMBERED, text, List.of(String.valueOf(counter))));
                } else {
                    blocks.add(new Block(Kind.BULLET, text, null));
                }
                continue;
            }
            if (line.startsWith("<p")) {
                blocks.add(new Block(Kind.P, inner(line, "p"), null));
                continue;
            }
            if (line.startsWith("<div class=\"gabarito\"")) {
                blocks.add(new Block(Kind.H4, "Gabarito", null));
                continue;
            }
            if (line.startsWith("<div")) {
                continue;
            }
            if (line.startsWith("<!DOCTYPE") || line.startsWith("<html")
                    || line.startsWith("<head") || line.startsWith("<title")
                    || line.startsWith("<meta") || line.startsWith("<body")
                    || line.startsWith("</") || line.startsWith("<table")
                    || line.startsWith("<thead") || line.startsWith("<tbody")
                    || line.startsWith("<br")) {
                continue;
            }

            if (line.startsWith("<code>") || line.startsWith("<strong>")) {
                blocks.add(new Block(Kind.P, clean(line), null));
            }
        }

        return blocks;
    }

    private static String trimCode(String code) {
        String result = code.replace("\t", "    ");
        while (result.endsWith("\n") || result.endsWith("\r")) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private static String inner(String line, String tag) {
        int start = line.indexOf('>');
        int end = line.lastIndexOf("</" + tag);
        if (start < 0) {
            return "";
        }
        String body = end > start ? line.substring(start + 1, end) : line.substring(start + 1);

        return clean(body);
    }

    private static String clean(String value) {
        String text = value
                .replaceAll("<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", "")
                .replace("\u0026lt;", "<")
                .replace("\u0026gt;", ">")
                .replace("\u0026amp;", "\u0026")
                .replace("\u0026quot;", String.valueOf((char) 34))
                .replace("\u0026#39;", "'")
                .replace("&nbsp;", " ")
                .replace("&middot;", "\u00b7")
                .replace("&mdash;", "\u2014")
                .replace("&ndash;", "\u2013")
                .replace("\u2192", "->")
                .replace("\u2265", ">=")
                .replace("\u2264", "<=")
                .replace("\u00d7", "x")
                .replace("\u2026", "...")
                .replace("\u2022", "-");

        return text.replaceAll("\\s+", " ").trim();
    }

    // --------------------------------------------------------------- rendering

    private static final class Doc {

        private final List<String> pages = new ArrayList<>();
        private StringBuilder page = new StringBuilder();
        private float y = TOP_Y;

        Doc() {
            pages.add("");
        }

        void newPage() {
            pages.set(pages.size() - 1, page.toString());
            page = new StringBuilder();
            pages.add("");
            y = TOP_Y;
        }

        private void ensure(float needed) {
            if (y - needed < BOTTOM_Y) {
                newPage();
            }
        }

        private void text(String font, float size, String content, float indent, String color) {
            page.append(String.format(Locale.ROOT,
                    "%s BT /%s %.2f Tf 1 0 0 1 %.2f %.2f Tm (%s) Tj ET%n",
                    color, font, size, MARGIN_L + indent, y, escape(content)));
        }

        private void rect(float x, float w, float h, String color) {
            page.append(String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f re f%n",
                    color, x, y, w, h));
        }

        void heading(String text, float size, float leading, String color) {
            for (String line : wrap(text, true, size)) {
                ensure(leading + 4);
                y -= leading;
                text("F2", size, line, 0, color);
            }
        }

        void spaced(float amount) {
            ensure(amount);
            y -= amount;
        }

        void paragraph(String text, float indent, boolean bold) {
            float size = 9.6f;
            float leading = 12.6f;
            for (String line : wrap(text, bold, size)) {
                ensure(leading);
                y -= leading;
                text(bold ? "F2" : "F1", size, line, indent, "0 g");
            }
        }

        void bullet(String marker, String text) {
            List<String> lines = wrap(text, false, 9.6f);
            boolean first = true;
            for (String line : lines) {
                ensure(12.6f);
                y -= 12.6f;
                text("F1", 9.6f, first ? marker : " ", 0, "0 g");
                text("F1", 9.6f, line, 16, "0 g");
                first = false;
            }
        }

        void code(String block) {
            float size = 8.2f;
            float leading = 10.2f;
            int maxChars = (int) ((TEXT_W - 14) / (0.6f * size));
            spaced(3);
            for (String line : block.split("\n")) {
                for (String part : splitLong(line, maxChars)) {
                    ensure(leading + 2);
                    y -= leading;
                    rect(MARGIN_L + 1, 2.2f, leading - 1.2f, "0.45 g");
                    text("F3", size, part, 10, "0.15 g");
                }
            }
            spaced(4);
        }

        void rule() {
            ensure(8);
            y -= 4;
            rect(MARGIN_L, TEXT_W, 0.9f, "0.60 g");
            y -= 4;
        }

        void finish() {
            pages.set(pages.size() - 1, page.toString());
        }

        List<String> pages() {
            return pages;
        }
    }

    private static String render(List<Block> blocks) {
        Doc doc = new Doc();

        for (Block block : blocks) {
            switch (block.kind()) {
                case H1 -> {
                    doc.spaced(10);
                    doc.heading(block.text(), 20f, 24f, "0.05 0.24 0.36 rg");
                    doc.spaced(4);
                }
                case H2 -> {
                    doc.spaced(14);
                    doc.heading(block.text(), 14f, 17f, "0.05 0.24 0.36 rg");
                    doc.rule();
                }
                case H3 -> {
                    doc.spaced(10);
                    doc.heading(block.text(), 11.6f, 14f, "0.08 0.29 0.42 rg");
                }
                case H4 -> {
                    doc.spaced(6);
                    doc.heading(block.text(), 10.2f, 12.5f, "0 g");
                }
                case P -> doc.paragraph(block.text(), 0, false);
                case BULLET -> doc.bullet("-", block.text());
                case NUMBERED -> doc.bullet(block.cells().get(0) + ".", block.text());
                case CODE -> doc.code(block.text());
                case ROW -> {
                    List<String> cells = block.cells();
                    doc.paragraph(cells.get(0), 0, true);
                    for (int i = 1; i < cells.size(); i++) {
                        doc.bullet("-", cells.get(i));
                    }
                    doc.spaced(3);
                }
                default -> {
                }
            }
        }

        doc.finish();
        return assemblePdf(doc.pages());
    }

    private static List<String> splitLong(String line, int maxChars) {
        List<String> parts = new ArrayList<>();
        if (line.length() <= maxChars) {
            parts.add(line);
            return parts;
        }
        for (int i = 0; i < line.length(); i += maxChars) {
            parts.add(line.substring(i, Math.min(line.length(), i + maxChars)));
        }

        return parts;
    }

    // ------------------------------------------------------------ text metrics

    private static final int[] HELV = new int[128];

    static {
        java.util.Arrays.fill(HELV, 556);
        int[] digits = {556, 556, 556, 556, 556, 556, 556, 556, 556, 556};
        for (int i = 0; i < 10; i++) {
            HELV['0' + i] = digits[i];
        }
        for (int i = 32; i < 127; i++) {
            HELV[i] = 500;
        }
        int[] ascii = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333,
            278, 278, 556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278,
            584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278,
            500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944,
            667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556,
            278, 556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500,
            278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
        };
        for (int i = 0; i < ascii.length && 32 + i < 127; i++) {
            HELV[32 + i] = ascii[i];
        }
    }

    private static float width(String text, boolean bold, float size) {
        float total = 0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int units = c < 128 ? HELV[c] : 556;
            total += units;
        }
        float factor = bold ? 1.04f : 1f;

        return total / 1000f * size * factor;
    }

    private static List<String> wrap(String text, boolean bold, float size) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }

        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (width(candidate, bold, size) > TEXT_W - 6 && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    // ------------------------------------------------------------ PDF assembly

    private static String assemblePdf(List<String> pages) {
        int pageCount = pages.size();
        List<String> objects = new ArrayList<>();

        String kids = buildKids(pageCount);

        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Count " + pageCount + " /Kids [" + kids + "] >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>");

        for (int i = 0; i < pageCount; i++) {
            String content = withFooter(pages.get(i), i + 1, pageCount);
            int contentLength = content.getBytes(StandardCharsets.ISO_8859_1).length;
            int contentObj = 6 + (i * 2);
            int pageObj = contentObj + 1;
            objects.add("<< /Length " + contentLength + " >>\nstream\n" + content
                    + "\nendstream");
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + (int) PAGE_W + " " + (int) PAGE_H
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> "
                    + "/Contents " + contentObj + " 0 R >>");
        }

        StringBuilder out = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        out.append("%PDF-1.4\n");

        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.length());
            out.append(i + 1).append(" 0 obj\n").append(objects.get(i))
                    .append("\nendobj\n");
        }

        int xrefOffset = out.length();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objects.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            xref.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        xref.append("trailer\n<< /Size ").append(objects.size() + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xrefOffset)
                .append("\n%%EOF");

        return out.append(xref).toString();
    }

    private static String buildKids(int pageCount) {
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            if (i > 0) {
                kids.append(' ');
            }
            kids.append(7 + (i * 2)).append(" 0 R");
        }

        return kids.toString();
    }

    private static String withFooter(String content, int page, int total) {
        String label = "rdn-api - Explicacao do projeto    " + page + " / " + total;
        float size = 8f;
        float textWidth = width(label, false, size);
        float x = (PAGE_W - textWidth) / 2f;

        return content + String.format(Locale.ROOT,
                "0.45 g BT /F1 %.2f Tf 1 0 0 1 %.2f 34 Tm (%s) Tj ET%n",
                size, x, escape(label));
    }

    private static String escape(String text) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '(' -> escaped.append("\\(");
                case ')' -> escaped.append("\\)");
                case '\r', '\n', '\t' -> escaped.append(' ');
                default -> escaped.append(c > 255 ? '?' : c);
            }
        }

        return escaped.toString();
    }

}
