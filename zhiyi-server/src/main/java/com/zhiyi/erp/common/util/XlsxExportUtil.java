package com.zhiyi.erp.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XlsxExportUtil {

    public record Sheet(String name, List<List<Object>> rows) {
    }

    public static void write(OutputStream outputStream, List<Sheet> sheets) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            writeEntry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            writeEntry(zip, "_rels/.rels", rootRels());
            writeEntry(zip, "xl/workbook.xml", workbook(sheets));
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
            writeEntry(zip, "xl/styles.xml", styles());
            for (int i = 0; i < sheets.size(); i++) {
                writeEntry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", worksheet(sheets.get(i).rows()));
            }
        }
    }

    private static void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.stripLeading().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes(int sheetCount) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
        for (int i = 1; i <= sheetCount; i++) {
            builder.append("<Override PartName=\"/xl/worksheets/sheet")
                    .append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        builder.append("</Types>");
        return builder.toString();
    }

    private static String rootRels() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                <sheets>
                """);
        for (int i = 0; i < sheets.size(); i++) {
            builder.append("<sheet name=\"")
                    .append(escapeXml(sheets.get(i).name()))
                    .append("\" sheetId=\"")
                    .append(i + 1)
                    .append("\" r:id=\"rId")
                    .append(i + 1)
                    .append("\"/>");
        }
        builder.append("</sheets></workbook>");
        return builder.toString();
    }

    private static String workbookRels(int sheetCount) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
        for (int i = 1; i <= sheetCount; i++) {
            builder.append("<Relationship Id=\"rId")
                    .append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i)
                    .append(".xml\"/>");
        }
        builder.append("<Relationship Id=\"rId")
                .append(sheetCount + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        builder.append("</Relationships>");
        return builder.toString();
    }

    private static String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
                <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFEAF0F7"/><bgColor indexed="64"/></patternFill></fill></fills>
                <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                <cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs>
                <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
                """;
    }

    private static String worksheet(List<List<Object>> rows) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                """);
        for (int i = 0; i < rows.size(); i++) {
            builder.append("<row r=\"").append(i + 1).append("\">");
            List<Object> row = rows.get(i);
            for (Object value : row) {
                builder.append(cell(value, i == 0));
            }
            builder.append("</row>");
        }
        builder.append("</sheetData></worksheet>");
        return builder.toString();
    }

    private static String cell(Object value, boolean header) {
        String style = header ? " s=\"1\"" : "";
        if (value instanceof Number && !(value instanceof BigDecimal decimal && decimal.scale() > 8)) {
            return "<c" + style + "><v>" + value + "</v></c>";
        }
        return "<c t=\"inlineStr\"" + style + "><is><t>" + escapeXml(value == null ? "" : value.toString()) + "</t></is></c>";
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
