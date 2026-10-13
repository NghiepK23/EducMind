package com.educmind.rag.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tách văn bản thành các đoạn (chunk) để embedding.
 * Kích thước tính theo ký tự. Lớp này thuần logic, không phụ thuộc Spring.
 */
public class TextChunker {

    public record Chunk(int index, String content) {
    }

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n");
    private static final String SEPARATOR = "\n\n";

    private final int targetChars;
    private final int maxChars;
    private final int overlapChars;

    public TextChunker(int targetChars, int maxChars, int overlapChars) {
        if (targetChars <= 0) {
            throw new IllegalArgumentException("targetChars phải lớn hơn 0");
        }
        if (maxChars < 2 || maxChars < targetChars) {
            throw new IllegalArgumentException("maxChars phải >= 2 và >= targetChars");
        }
        if (overlapChars < 0 || overlapChars >= targetChars) {
            throw new IllegalArgumentException("overlapChars phải trong khoảng [0, targetChars)");
        }
        this.targetChars = targetChars;
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<Chunk> chunk(String text) {
        List<Chunk> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.isEmpty()) {
            return result;
        }

        List<String> pieces = new ArrayList<>();
        for (String paragraph : PARAGRAPH_SPLIT.split(normalized)) {
            String p = paragraph.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() <= maxChars) {
                pieces.add(p);
            } else {
                pieces.addAll(splitLong(p));
            }
        }

        List<String> bodies = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String piece : pieces) {
            if (current.length() == 0) {
                current.append(piece);
            } else if (current.length() + SEPARATOR.length() + piece.length() <= targetChars) {
                current.append(SEPARATOR).append(piece);
            } else {
                bodies.add(current.toString());
                current = new StringBuilder(piece);
            }
        }
        if (current.length() > 0) {
            bodies.add(current.toString());
        }

        for (int i = 0; i < bodies.size(); i++) {
            String body = bodies.get(i);
            if (i > 0 && overlapChars > 0) {
                String tail = tailOf(bodies.get(i - 1));
                if (!tail.isEmpty()) {
                    body = tail + SEPARATOR + body;
                }
            }
            result.add(new Chunk(i, body));
        }
        return result;
    }

    private String tailOf(String body) {
        int start = Math.max(0, body.length() - overlapChars);
        if (start > 0 && !Character.isWhitespace(body.charAt(start - 1))) {
            int ws = nextWhitespace(body, start);
            if (ws >= 0) {
                start = ws + 1;
            }
        }
        return body.substring(start).strip();
    }

    private static int nextWhitespace(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private List<String> splitLong(String paragraph) {
        List<String> parts = new ArrayList<>();
        String rest = paragraph;
        while (rest.length() > maxChars) {
            int cut = findCut(rest);
            String piece = rest.substring(0, cut).strip();
            if (!piece.isEmpty()) {
                parts.add(piece);
            }
            rest = rest.substring(cut).strip();
        }
        if (!rest.isEmpty()) {
            parts.add(rest);
        }
        return parts;
    }

    private int findCut(String rest) {
        int limit = maxChars;
        int lower = Math.max(1, limit / 2);
        for (int i = limit; i >= lower; i--) {
            char c = rest.charAt(i);
            char prev = rest.charAt(i - 1);
            if (c == '\n' || (Character.isWhitespace(c) && (prev == '.' || prev == '!' || prev == '?'))) {
                return i;
            }
        }
        for (int i = limit; i >= 1; i--) {
            if (Character.isWhitespace(rest.charAt(i))) {
                return i;
            }
        }
        return limit;
    }
}
