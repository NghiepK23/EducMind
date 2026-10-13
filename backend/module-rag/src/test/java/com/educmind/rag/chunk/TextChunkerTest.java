package com.educmind.rag.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void nullOrBlankGivesNoChunks() {
        TextChunker chunker = new TextChunker(100, 200, 0);
        assertTrue(chunker.chunk(null).isEmpty());
        assertTrue(chunker.chunk("").isEmpty());
        assertTrue(chunker.chunk("   \n\n  ").isEmpty());
    }

    @Test
    void shortTextGivesOneChunk() {
        TextChunker chunker = new TextChunker(100, 200, 20);
        List<TextChunker.Chunk> chunks = chunker.chunk("Xin chào các em.");
        assertEquals(1, chunks.size());
        assertEquals(0, chunks.get(0).index());
        assertEquals("Xin chào các em.", chunks.get(0).content());
    }

    @Test
    void windowsLineEndingsAreNormalized() {
        TextChunker chunker = new TextChunker(100, 200, 0);
        List<TextChunker.Chunk> chunks = chunker.chunk("Đoạn một.\r\n\r\nĐoạn hai.");
        assertEquals(1, chunks.size());
        assertFalse(chunks.get(0).content().contains("\r"));
        assertEquals("Đoạn một.\n\nĐoạn hai.", chunks.get(0).content());
    }

    @Test
    void paragraphsAreGroupedUpToTarget() {
        TextChunker chunker = new TextChunker(100, 200, 0);
        String a = "a".repeat(30);
        String b = "b".repeat(30);
        String c = "c".repeat(30);
        String d = "d".repeat(30);
        List<TextChunker.Chunk> chunks = chunker.chunk(a + "\n\n" + b + "\n\n" + c + "\n\n" + d);
        assertEquals(2, chunks.size());
        assertEquals(a + "\n\n" + b + "\n\n" + c, chunks.get(0).content());
        assertEquals(d, chunks.get(1).content());
    }

    @Test
    void longParagraphIsSplitWithoutLosingText() {
        TextChunker chunker = new TextChunker(100, 150, 0);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            sb.append("Câu số ").append(i).append(" được viết ra để kiểm tra việc cắt đoạn. ");
        }
        String original = sb.toString().strip();
        List<TextChunker.Chunk> chunks = chunker.chunk(original);

        assertTrue(chunks.size() > 1);
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            assertEquals(i, chunks.get(i).index());
            assertTrue(chunks.get(i).content().length() <= 150);
            joined.append(chunks.get(i).content()).append(' ');
        }
        String collapsedJoined = joined.toString().replaceAll("\\s+", " ").strip();
        String collapsedOriginal = original.replaceAll("\\s+", " ").strip();
        assertEquals(collapsedOriginal, collapsedJoined);
    }

    @Test
    void overlapStartsAtWordBoundaryAndComesFromPreviousChunk() {
        TextChunker chunker = new TextChunker(100, 200, 20);
        String p1 = "alpha bravo charlie delta echo foxtrot golf hotel india juliet kilo";
        String p2 = "lima mike november oscar papa quebec romeo sierra tango uniform victor";
        List<TextChunker.Chunk> chunks = chunker.chunk(p1 + "\n\n" + p2);

        assertEquals(2, chunks.size());
        assertEquals(p1, chunks.get(0).content());
        String second = chunks.get(1).content();
        assertTrue(second.endsWith(p2));
        String prefix = second.substring(0, second.length() - p2.length() - 2);
        assertFalse(prefix.isEmpty());
        assertTrue(prefix.length() <= 20);
        assertTrue(p1.endsWith(prefix));
        assertTrue(prefix.startsWith("india"));
    }

    @Test
    void textWithoutWhitespaceIsHardCut() {
        TextChunker chunker = new TextChunker(100, 150, 0);
        List<TextChunker.Chunk> chunks = chunker.chunk("x".repeat(1000));
        assertEquals(7, chunks.size());
        int total = 0;
        for (TextChunker.Chunk chunk : chunks) {
            assertTrue(chunk.content().length() <= 150);
            total += chunk.content().length();
        }
        assertEquals(1000, total);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(0, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(10, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(10, 20, 10));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(10, 20, -1));
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(1, 1, 0));
    }
}
