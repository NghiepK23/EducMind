package com.educmind.rag.retrieval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RrfFuserTest {

    private static List<String> keys(List<RrfFuser.Scored<String>> scored) {
        return scored.stream().map(RrfFuser.Scored::key).toList();
    }

    @Test
    void phanTuXuatHienOCaHaiDanhSach_xepTruoc() {
        var result = RrfFuser.fuse(List.of(List.of("A", "B", "C"), List.of("B", "D")), 60, 10);
        assertEquals(List.of("B", "A", "D", "C"), keys(result));
        assertEquals(1.0 / 62 + 1.0 / 61, result.get(0).score(), 1e-12);
    }

    @Test
    void giuDungSoLuongToiDa() {
        var result = RrfFuser.fuse(List.of(List.of("A", "B", "C", "D")), 60, 2);
        assertEquals(List.of("A", "B"), keys(result));
    }

    @Test
    void danhSachRong_traRong() {
        assertTrue(RrfFuser.fuse(List.of(List.<String>of(), List.<String>of()), 60, 5).isEmpty());
    }

    @Test
    void trungLapTrongCungMotDanhSach_chiTinhLanDau() {
        var result = RrfFuser.fuse(List.of(List.of("A", "A", "B")), 60, 5);
        assertEquals(List.of("A", "B"), keys(result));
        assertEquals(1.0 / 62, result.get(1).score(), 1e-12);
    }

    @Test
    void thamSoSai_nemLoi() {
        assertThrows(IllegalArgumentException.class, () -> RrfFuser.fuse(List.of(List.of("A")), 0, 5));
        assertThrows(IllegalArgumentException.class, () -> RrfFuser.fuse(List.of(List.of("A")), 60, 0));
    }
}
