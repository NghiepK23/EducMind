package com.educmind.reports.service;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Chuyển 1 báo cáo thành PDF (khổ A4 ngang), dùng chung cách làm phẳng dữ liệu với CsvReportWriter.
 * Nhúng font Noto Sans để hiển thị đúng tiếng Việt có dấu.
 */
public final class PdfReportWriter {

    private static final String REGULAR_FONT = "/fonts/NotoSans-Regular.ttf";
    private static final String BOLD_FONT = "/fonts/NotoSans-Bold.ttf";
    private static final Color HEADER_BG = new Color(230, 230, 230);

    private PdfReportWriter() {
    }

    public static byte[] write(String title, Object report) {
        CsvReportWriter.Flat flat = CsvReportWriter.toFlat(report);
        BaseFont regular = loadFont(REGULAR_FONT, "NotoSans-Regular.ttf");
        BaseFont bold = loadFont(BOLD_FONT, "NotoSans-Bold.ttf");
        Font titleFont = new Font(bold, 16);
        Font sectionFont = new Font(bold, 12);
        Font headFont = new Font(bold, 9);
        Font bodyFont = new Font(regular, 9);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph heading = new Paragraph(title, titleFont);
            heading.setSpacingAfter(10);
            doc.add(heading);

            if (!flat.summary().isEmpty()) {
                PdfPTable summary = new PdfPTable(new float[]{1f, 2f});
                summary.setWidthPercentage(100);
                for (String[] r : flat.summary()) {
                    summary.addCell(cell(r[0], headFont, false));
                    summary.addCell(cell(r[1], bodyFont, false));
                }
                doc.add(summary);
            }

            for (CsvReportWriter.Section sec : flat.sections()) {
                Paragraph h = new Paragraph(sec.title(), sectionFont);
                h.setSpacingBefore(14);
                h.setSpacingAfter(6);
                doc.add(h);
                if (sec.header().isEmpty()) {
                    doc.add(new Paragraph("(trống)", bodyFont));
                    continue;
                }
                PdfPTable table = new PdfPTable(sec.header().size());
                table.setWidthPercentage(100);
                table.setHeaderRows(1);
                for (String col : sec.header()) {
                    table.addCell(cell(col, headFont, true));
                }
                for (java.util.List<String> row : sec.rows()) {
                    for (String value : row) {
                        table.addCell(cell(value, bodyFont, false));
                    }
                }
                doc.add(table);
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được PDF: " + e.getMessage(), e);
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }
        }
        return out.toByteArray();
    }

    private static PdfPCell cell(String text, Font font, boolean header) {
        PdfPCell c = new PdfPCell(new Phrase(text == null ? "" : text, font));
        c.setPadding(4);
        if (header) {
            c.setBackgroundColor(HEADER_BG);
        }
        return c;
    }

    private static BaseFont loadFont(String resourcePath, String fontName) {
        try (InputStream in = PdfReportWriter.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Thiếu file font: " + resourcePath);
            }
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Không nạp được font " + resourcePath + ": " + e.getMessage(), e);
        }
    }
}
