package com.educmind.rag.extract;

import com.educmind.common.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Đọc văn bản thô từ file tài liệu: PDF, DOCX, TXT, MD.
 * Bản đầu với DOCX chỉ lấy chữ trong đoạn văn và ô bảng; bỏ hộp chữ, chú thích cuối trang, hình.
 */
@Component
public class DocumentTextExtractor {

    static final int MAX_INPUT_BYTES = 50 * 1024 * 1024;
    static final int MAX_DOCX_XML_BYTES = 30 * 1024 * 1024;
    static final int MAX_ZIP_ENTRIES = 5000;

    private static final String ERR_UNSUPPORTED = "UNSUPPORTED_CONTENT_TYPE";
    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private enum Kind { PDF, DOCX, TEXT }

    public String extract(InputStream in, String filename, String mimeType) {
        Kind kind = detect(filename, mimeType);
        byte[] bytes = readAll(in);
        String text = switch (kind) {
            case PDF -> extractPdf(bytes);
            case DOCX -> extractDocx(bytes);
            case TEXT -> extractPlainText(bytes);
        };
        if (text == null || text.isBlank()) {
            throw unsupported("Không đọc được chữ nào từ tài liệu (có thể là file ảnh scan hoặc file rỗng)");
        }
        return text.strip();
    }

    private Kind detect(String filename, String mimeType) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        int semi = mime.indexOf(';');
        if (semi >= 0) {
            mime = mime.substring(0, semi);
        }
        mime = mime.strip();
        if (mime.equals("application/pdf")) {
            return Kind.PDF;
        }
        if (mime.equals(DOCX_MIME)) {
            return Kind.DOCX;
        }
        if (mime.equals("text/plain") || mime.equals("text/markdown")) {
            return Kind.TEXT;
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return Kind.PDF;
        }
        if (name.endsWith(".docx")) {
            return Kind.DOCX;
        }
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
            return Kind.TEXT;
        }
        throw unsupported("Loại file chưa được hỗ trợ để trích xuất nội dung: " + mimeType);
    }

    private byte[] readAll(InputStream in) {
        try {
            byte[] bytes = in.readNBytes(MAX_INPUT_BYTES + 1);
            if (bytes.length > MAX_INPUT_BYTES) {
                throw new BusinessException("FILE_TOO_LARGE", "File vượt quá giới hạn xử lý",
                        HttpStatus.PAYLOAD_TOO_LARGE);
            }
            return bytes;
        } catch (IOException e) {
            throw new BusinessException("RESOURCE_READ_FAILED", "Không đọc được dữ liệu file",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractPlainText(byte[] bytes) {
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
            s = s.substring(1);
        }
        return s.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String extractPdf(byte[] bytes) {
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw unsupported("Không đọc được file PDF (hỏng hoặc có mật khẩu)");
        }
    }

    private String extractDocx(byte[] bytes) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ZIP_ENTRIES) {
                    throw unsupported("File DOCX có quá nhiều thành phần bên trong");
                }
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] xml = zip.readNBytes(MAX_DOCX_XML_BYTES + 1);
                    if (xml.length > MAX_DOCX_XML_BYTES) {
                        throw unsupported("Nội dung DOCX quá lớn sau khi giải nén");
                    }
                    return parseDocumentXml(xml);
                }
            }
        } catch (IOException e) {
            throw unsupported("Không đọc được file DOCX (hỏng hoặc không đúng định dạng)");
        }
        throw unsupported("File DOCX thiếu word/document.xml");
    }

    private String parseDocumentXml(byte[] xml) {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        StringBuilder out = new StringBuilder();
        StringBuilder para = new StringBuilder();
        int skipDepth = 0;
        boolean inText = false;
        XMLStreamReader r = null;
        try {
            r = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    if (!W_NS.equals(r.getNamespaceURI())) {
                        continue;
                    }
                    String n = r.getLocalName();
                    if (skipDepth > 0) {
                        if (n.equals("txbxContent")) {
                            skipDepth++;
                        }
                        continue;
                    }
                    switch (n) {
                        case "txbxContent" -> skipDepth = 1;
                        case "t" -> inText = true;
                        case "tab" -> para.append('\t');
                        case "br", "cr" -> para.append('\n');
                        default -> { }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    if (!W_NS.equals(r.getNamespaceURI())) {
                        continue;
                    }
                    String n = r.getLocalName();
                    if (skipDepth > 0) {
                        if (n.equals("txbxContent")) {
                            skipDepth--;
                        }
                        continue;
                    }
                    if (n.equals("t")) {
                        inText = false;
                    } else if (n.equals("p")) {
                        String p = para.toString().strip();
                        if (!p.isEmpty()) {
                            out.append(p).append("\n\n");
                        }
                        para.setLength(0);
                    }
                } else if ((ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA)
                        && inText && skipDepth == 0) {
                    para.append(r.getText());
                }
            }
        } catch (XMLStreamException e) {
            throw unsupported("Nội dung DOCX không đọc được (XML lỗi hoặc không hợp lệ)");
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (XMLStreamException ignored) {
                    // không ảnh hưởng kết quả
                }
            }
        }
        return out.toString();
    }

    private static BusinessException unsupported(String message) {
        return new BusinessException(ERR_UNSUPPORTED, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
