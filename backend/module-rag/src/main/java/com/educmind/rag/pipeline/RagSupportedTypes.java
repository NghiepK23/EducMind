package com.educmind.rag.pipeline;

import java.util.Locale;
import java.util.Set;

/**
 * Các loại file mà pipeline RAG đọc được (PDF, DOCX, TXT, MD).
 * Danh sách này phải khớp với RAG_PROCESSABLE_MIME_TYPES trong ResourceService (module-course).
 */
public final class RagSupportedTypes {

    public static final Set<String> MIME_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown"
    );

    private RagSupportedTypes() {
    }

    public static boolean isSupported(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        String mime = mimeType.toLowerCase(Locale.ROOT);
        int semi = mime.indexOf(';');
        if (semi >= 0) {
            mime = mime.substring(0, semi);
        }
        return MIME_TYPES.contains(mime.strip());
    }
}
