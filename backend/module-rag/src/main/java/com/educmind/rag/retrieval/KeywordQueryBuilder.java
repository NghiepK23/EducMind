package com.educmind.rag.retrieval;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dựng chuỗi cho to_tsquery('simple', ...) từ câu hỏi tự nhiên: các từ nối bằng OR.
 * Chỉ giữ chữ và số nên không thể chèn toán tử của tsquery.
 * Cấu hình 'simple' chưa có bỏ dấu hay stemming tiếng Việt, nên từ khóa chỉ bổ trợ cho vector.
 */
public final class KeywordQueryBuilder {

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{N}]+");

    private KeywordQueryBuilder() {
    }

    /** Trả chuỗi rỗng nếu không có từ nào dùng được. */
    public static String build(String text, int maxTokens) {
        if (text == null || text.isBlank() || maxTokens <= 0) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        Set<String> tokens = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(normalized);
        while (m.find() && tokens.size() < maxTokens) {
            String token = m.group();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        return String.join(" | ", tokens);
    }
}
