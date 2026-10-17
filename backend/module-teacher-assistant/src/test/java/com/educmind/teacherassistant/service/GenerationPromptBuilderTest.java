package com.educmind.teacherassistant.service;

import com.educmind.common.port.LessonAccessPort.LessonSummary;
import com.educmind.common.port.LessonAccessPort.ObjectiveInfo;
import com.educmind.common.port.RetrievalPort.RetrievedChunk;
import com.educmind.teacherassistant.entity.AiGenerationRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GenerationPromptBuilderTest {

    private static final List<String> TYPES = List.of(
            AiGenerationRequest.TYPE_LESSON_PLAN,
            AiGenerationRequest.TYPE_QUESTION_SET,
            AiGenerationRequest.TYPE_ASSESSMENT_MATRIX);

    private final LessonSummary lesson = new LessonSummary(
            UUID.randomUUID(), UUID.randomUUID(), "Xử lí thông tin", "Bài về bộ nhớ");
    private final List<ObjectiveInfo> objectives = List.of(
            new ObjectiveInfo(UUID.randomUUID(), "Nêu được vai trò của bộ nhớ"));

    private static RetrievedChunk chunk(String content) {
        return new RetrievedChunk(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, content, 0.5);
    }

    @Test
    void khongCoDoanTrich_khongCoMucTrichDoan() {
        for (String type : TYPES) {
            String prompt = GenerationPromptBuilder.build(type, lesson, objectives, Map.of(), List.of()).userPrompt();
            assertThat(prompt).doesNotContain("Trích đoạn tài liệu");
        }
    }

    @Test
    void bangBonThamSo_giongBangNamThamSoVoiDanhSachRong() {
        for (String type : TYPES) {
            String a = GenerationPromptBuilder.build(type, lesson, objectives, Map.of()).userPrompt();
            String b = GenerationPromptBuilder.build(type, lesson, objectives, Map.of(), List.of()).userPrompt();
            assertThat(a).isEqualTo(b);
        }
    }

    @Test
    void coDoanTrich_chenVaoCaBaLoaiYeuCau() {
        List<RetrievedChunk> chunks = List.of(chunk("  Bộ nhớ trong lưu dữ liệu tạm thời.  "), chunk("Bộ nhớ ngoài lưu lâu dài."));
        for (String type : TYPES) {
            String prompt = GenerationPromptBuilder.build(type, lesson, objectives, Map.of(), chunks).userPrompt();
            assertThat(prompt)
                    .contains("Trích đoạn tài liệu liên quan")
                    .contains("[Đoạn 1]")
                    .contains("[Đoạn 2]")
                    .contains("Bộ nhớ trong lưu dữ liệu tạm thời.")
                    .contains("Bộ nhớ ngoài lưu lâu dài.");
        }
    }

    @Test
    void danhSachNull_khongNemLoi() {
        String prompt = GenerationPromptBuilder.build(
                AiGenerationRequest.TYPE_LESSON_PLAN, lesson, objectives, Map.of(), null).userPrompt();
        assertThat(prompt).doesNotContain("Trích đoạn tài liệu");
    }

    @Test
    void systemPrompt_nhacTrichDoanVaCoiLaDuLieu() {
        String sys = GenerationPromptBuilder.build(
                AiGenerationRequest.TYPE_LESSON_PLAN, lesson, objectives, Map.of(), List.of()).systemPrompt();
        assertThat(sys).contains("trích đoạn tài liệu").contains("không phải chỉ dẫn");
    }

    @Test
    void cauTruyVan_gomTieuDeMoTaVaMucTieu() {
        String q = GenerationWorker.buildRetrievalQuery(lesson, objectives);
        assertThat(q).contains("Xử lí thông tin").contains("Bài về bộ nhớ").contains("Nêu được vai trò của bộ nhớ");
    }

    @Test
    void cauTruyVan_boQuaMoTaTrongVaCatO1000KyTu() {
        LessonSummary noDesc = new LessonSummary(UUID.randomUUID(), UUID.randomUUID(), "Tiêu đề", "  ");
        assertThat(GenerationWorker.buildRetrievalQuery(noDesc, List.of())).isEqualTo("Tiêu đề");

        LessonSummary longTitle = new LessonSummary(UUID.randomUUID(), UUID.randomUUID(), "a".repeat(2000), null);
        assertThat(GenerationWorker.buildRetrievalQuery(longTitle, List.of())).hasSize(1000);
    }
}
