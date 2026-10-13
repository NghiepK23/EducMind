package com.educmind.rag.extract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.educmind.common.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class DocumentTextExtractorTest {

    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    private final DocumentTextExtractor extractor = new DocumentTextExtractor();

    private String run(byte[] data, String filename, String mime) {
        return extractor.extract(new ByteArrayInputStream(data), filename, mime);
    }

    private static byte[] docx(String documentXml) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] pdfWithText(String text) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null) {
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void plainTextKeepsVietnameseAndStripsBom() {
        String content = "\uFEFFDữ liệu là gì?\r\nĐây là bài học.";
        String result = run(content.getBytes(StandardCharsets.UTF_8), "bai1.txt", "text/plain");
        assertEquals("Dữ liệu là gì?\nĐây là bài học.", result);
    }

    @Test
    void markdownIsRecognizedByExtensionWhenMimeIsGeneric() {
        String result = run("# Tiêu đề\n\nNội dung".getBytes(StandardCharsets.UTF_8),
                "ghi-chu.md", "application/octet-stream");
        assertTrue(result.startsWith("# Tiêu đề"));
    }

    @Test
    void docxReadsParagraphsAndTableCellsButSkipsTextBoxes() throws Exception {
        String xml = "<?xml version='1.0' encoding='UTF-8'?>"
                + "<w:document xmlns:w='" + W + "'><w:body>"
                + "<w:p><w:r><w:t>Bài 1: Thông tin và dữ liệu</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t xml:space='preserve'>Dữ liệu là </w:t></w:r><w:r><w:t>các giá trị.</w:t></w:r></w:p>"
                + "<w:tbl><w:tr>"
                + "<w:tc><w:p><w:r><w:t>Ô A1</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>Ô B1</w:t></w:r></w:p></w:tc>"
                + "</w:tr></w:tbl>"
                + "<w:p><w:r><w:txbxContent><w:p><w:r><w:t>TRONG HỘP</w:t></w:r></w:p></w:txbxContent></w:r></w:p>"
                + "</w:body></w:document>";
        String result = run(docx(xml), "bai1.docx", DOCX_MIME);
        assertEquals("Bài 1: Thông tin và dữ liệu\n\nDữ liệu là các giá trị.\n\nÔ A1\n\nÔ B1", result);
        assertFalse(result.contains("TRONG HỘP"));
    }

    @Test
    void docxWithoutDocumentXmlIsRejected() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("other.xml"));
            zip.write("<a/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        BusinessException e = assertThrows(BusinessException.class,
                () -> run(out.toByteArray(), "x.docx", DOCX_MIME));
        assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
    }

    @Test
    void docxExternalEntityIsNotResolved() throws Exception {
        String xml = "<?xml version='1.0'?>"
                + "<!DOCTYPE d [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>"
                + "<w:document xmlns:w='" + W + "'><w:body>"
                + "<w:p><w:r><w:t>&xxe;</w:t></w:r></w:p>"
                + "</w:body></w:document>";
        try {
            String result = run(docx(xml), "x.docx", DOCX_MIME);
            assertFalse(result.contains("root:"));
        } catch (BusinessException e) {
            assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
        }
    }

    @Test
    void pdfWithTextIsExtracted() throws Exception {
        String result = run(pdfWithText("Hello PDFBox"), "a.pdf", "application/pdf");
        assertTrue(result.contains("Hello PDFBox"));
    }

    @Test
    void pdfWithoutTextLayerIsRejected() throws Exception {
        BusinessException e = assertThrows(BusinessException.class,
                () -> run(pdfWithText(null), "scan.pdf", "application/pdf"));
        assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
    }

    @Test
    void corruptPdfIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> run("không phải pdf".getBytes(StandardCharsets.UTF_8), "a.pdf", "application/pdf"));
        assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
    }

    @Test
    void unsupportedTypeIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> run(new byte[] {1, 2, 3}, "anh.png", "image/png"));
        assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
    }

    @Test
    void emptyTextFileIsRejected() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> run("   \n ".getBytes(StandardCharsets.UTF_8), "trong.txt", "text/plain"));
        assertEquals("UNSUPPORTED_CONTENT_TYPE", e.getErrorCode());
    }
}
