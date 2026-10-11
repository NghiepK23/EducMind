package com.educmind.reports.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvReportWriterTest {

    public record Item(String name, int count) {
    }

    public record Inner(String note) {
    }

    public record Report(String title, Inner inner, List<Item> items) {
    }

    private String render(Object report) {
        return new String(CsvReportWriter.write(report), StandardCharsets.UTF_8);
    }

    @Test
    void startsWithBomAndHeader() {
        byte[] bytes = CsvReportWriter.write(new Report("t", new Inner("n"), List.of()));
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        assertTrue(render(new Report("t", new Inner("n"), List.of()))
                .startsWith("\uFEFFfield,value\n"));
    }

    @Test
    void writesScalarAndNestedFields() {
        String csv = render(new Report("Báo cáo", new Inner("ghi chú"), List.of()));
        assertTrue(csv.contains("title,Báo cáo\n"));
        assertTrue(csv.contains("inner.note,ghi chú\n"));
    }

    @Test
    void writesListAsSeparateTable() {
        String csv = render(new Report("t", new Inner("n"),
                List.of(new Item("a", 1), new Item("b", 2))));
        assertTrue(csv.contains("# items\nname,count\na,1\nb,2\n"));
    }

    @Test
    void emptyListShowsPlaceholder() {
        String csv = render(new Report("t", new Inner("n"), List.of()));
        assertTrue(csv.contains("# items\n(trống)\n"));
    }

    @Test
    void escapesCommaAndQuote() {
        String csv = render(new Report("a,b \"c\"", new Inner("n"), List.of()));
        assertTrue(csv.contains("title,\"a,b \"\"c\"\"\"\n"));
    }

    @Test
    void neutralizesFormulaInjection() {
        String csv = render(new Report("=SUM(A1)", new Inner("@x"), List.of()));
        assertTrue(csv.contains("title,'=SUM(A1)\n"));
        assertTrue(csv.contains("inner.note,'@x\n"));
    }

    @Test
    void nullValueBecomesEmptyCell() {
        String csv = render(new Report(null, new Inner("n"), List.of()));
        assertTrue(csv.contains("title,\n"));
    }
}
