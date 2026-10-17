package com.educmind.teacherassistant.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerationWorkerValidationTest {

    private static Map<String, Object> row(Object count) {
        return Map.of("learning_objective_id", "x", "cognitive_level", "UNDERSTAND", "count", count);
    }

    @Test
    void tongKhop_khongNemLoi() {
        assertThatCode(() -> GenerationWorker.checkMatrixTotal(List.of(row(4), row(3), row(3)), 10))
                .doesNotThrowAnyException();
    }

    @Test
    void tongLech_nemAiBadResponse() {
        assertThatThrownBy(() -> GenerationWorker.checkMatrixTotal(List.of(row(4), row(3)), 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_BAD_RESPONSE")
                .hasMessageContaining("7")
                .hasMessageContaining("10");
    }

    @Test
    void khongCoSoYeuCau_boQuaKiemTra() {
        assertThatCode(() -> GenerationWorker.checkMatrixTotal(List.of(row(1)), null))
                .doesNotThrowAnyException();
    }

    @Test
    void countKhongPhaiSo_hoacAm_nemAiBadResponse() {
        assertThatThrownBy(() -> GenerationWorker.checkMatrixTotal(List.of(row("ba")), 3))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("count không hợp lệ");
        assertThatThrownBy(() -> GenerationWorker.checkMatrixTotal(List.of(row(-1), row(4)), 3))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("count không hợp lệ");
    }

    @Test
    void expectedTotal_docDuocSoChuoiVaThieu() {
        assertThat(GenerationWorker.expectedTotal(Map.of("total_questions", 10))).isEqualTo(10);
        assertThat(GenerationWorker.expectedTotal(Map.of("total_questions", "12"))).isEqualTo(12);
        assertThat(GenerationWorker.expectedTotal(Map.of("total_questions", "abc"))).isNull();
        assertThat(GenerationWorker.expectedTotal(Map.of())).isNull();
        assertThat(GenerationWorker.expectedTotal(null)).isNull();
    }
}
