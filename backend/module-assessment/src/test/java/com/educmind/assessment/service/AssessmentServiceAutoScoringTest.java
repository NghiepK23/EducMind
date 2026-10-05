package com.educmind.assessment.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test cho auto-scoring logic (isAnswerCorrect / normalizeToStringSet) trong
 * AssessmentService — Module04_Assignment_Assessment_FINAL.md, PHẦN E, mục E.5.4.
 *
 * 2 method này đã đổi từ private sang package-private để test gọi trực tiếp — không
 * cần Spring context hay mock, vì constructor của AssessmentService chỉ gán field,
 * và 2 method thuần túy này không đụng tới bất kỳ dependency nào (repository/port) —
 * truyền null cho cả 7 tham số constructor là an toàn.
 *
 * isAnswerCorrect(studentAnswer, correctAnswer) — chú ý đúng thứ tự tham số.
 */
class AssessmentServiceAutoScoringTest {

    private final AssessmentService service =
            new AssessmentService(null, null, null, null, null, null, null, null);

    // ---- isAnswerCorrect ----

    @Test
    void singleChoice_matchingAnswer_isCorrect() {
        assertTrue(service.isAnswerCorrect(List.of("A"), List.of("A")));
    }

    @Test
    void singleChoice_wrongAnswer_isIncorrect() {
        assertFalse(service.isAnswerCorrect(List.of("B"), List.of("A")));
    }

    @Test
    void multipleChoice_sameElementsDifferentOrder_isCorrect() {
        assertTrue(service.isAnswerCorrect(List.of("B", "A"), List.of("A", "B")));
    }

    @Test
    void multipleChoice_missingOneElement_isIncorrect() {
        assertFalse(service.isAnswerCorrect(List.of("A"), List.of("A", "B")));
    }

    @Test
    void multipleChoice_extraElement_isIncorrect() {
        assertFalse(service.isAnswerCorrect(List.of("A", "B", "C"), List.of("A", "B")));
    }

    @Test
    void trueFalse_matchingBoolean_isCorrect() {
        assertTrue(service.isAnswerCorrect(Boolean.TRUE, Boolean.TRUE));
    }

    @Test
    void trueFalse_mismatchedBoolean_isIncorrect() {
        assertFalse(service.isAnswerCorrect(Boolean.TRUE, Boolean.FALSE));
    }

    @Test
    void emptyCorrectAnswer_alwaysIncorrect_evenIfStudentAlsoEmpty() {
        // Câu hỏi thiếu correctAnswer (dữ liệu lỗi) không bao giờ được coi là "đúng"
        assertFalse(service.isAnswerCorrect(List.of(), List.of()));
    }

    @Test
    void nullCorrectAnswer_isIncorrect() {
        assertFalse(service.isAnswerCorrect(List.of("A"), null));
    }

    @Test
    void nullStudentAnswer_isIncorrect() {
        // Học sinh chưa trả lời câu này (autosave chưa đụng tới)
        assertFalse(service.isAnswerCorrect(null, List.of("A")));
    }

    @Test
    void whitespaceAroundAnswer_isTrimmedBeforeComparison() {
        assertTrue(service.isAnswerCorrect(List.of(" A "), List.of("A")));
    }

    // ---- normalizeToStringSet ----

    @Test
    void normalize_nullValue_returnsEmptySet() {
        assertEquals(Set.of(), service.normalizeToStringSet(null));
    }

    @Test
    void normalize_collectionWithNullElements_skipsNulls() {
        List<String> withNull = new ArrayList<>();
        withNull.add("A");
        withNull.add(null);
        withNull.add("B");
        assertEquals(Set.of("A", "B"), service.normalizeToStringSet(withNull));
    }

    @Test
    void normalize_singleNonCollectionValue_returnsSingleElementSet() {
        assertEquals(Set.of("true"), service.normalizeToStringSet(Boolean.TRUE));
    }

    @Test
    void normalize_singleValueWithWhitespace_isTrimmed() {
        assertEquals(Set.of("A"), service.normalizeToStringSet(" A "));
    }
}
