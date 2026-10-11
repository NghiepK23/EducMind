package com.educmind.reports.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReportWriterTest {

    record Item(String name, int score) {
    }

    record Report(String className, List<Item> items) {
    }

    @Test
    void producesValidPdfWithVietnameseText() {
        byte[] pdf = PdfReportWriter.write("Báo cáo lớp",
                new Report("Lớp 7A1 – Tin học", List.of(new Item("Nguyễn Văn Ấn", 9))));

        assertTrue(pdf.length > 1000);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
        String tail = new String(pdf, Math.max(0, pdf.length - 32), Math.min(32, pdf.length),
                StandardCharsets.US_ASCII);
        assertTrue(tail.contains("%%EOF"));
    }

    @Test
    void emptyListStillProducesValidPdf() {
        byte[] pdf = PdfReportWriter.write("Báo cáo rỗng", new Report("Lớp trống", List.of()));

        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}
