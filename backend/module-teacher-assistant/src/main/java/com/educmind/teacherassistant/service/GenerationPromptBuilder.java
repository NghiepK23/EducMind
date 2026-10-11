package com.educmind.teacherassistant.service;

import com.educmind.common.port.LessonAccessPort.LessonSummary;
import com.educmind.common.port.LessonAccessPort.ObjectiveInfo;
import com.educmind.teacherassistant.entity.AiGenerationRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dựng prompt + schema JSON cho 3 loại yêu cầu của Module 7. */
public final class GenerationPromptBuilder {

    public record Built(String systemPrompt, String userPrompt, Map<String, Object> schema) {
    }

    public static final List<String> COGNITIVE_LEVELS =
            List.of("REMEMBER", "UNDERSTAND", "APPLY", "ANALYZE", "EVALUATE", "CREATE");
    public static final List<String> QUESTION_TYPES = List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE");
    public static final List<String> DIFFICULTIES = List.of("EASY", "MEDIUM", "HARD");

    private static final String SYSTEM_PROMPT =
            "Bạn là trợ lý soạn bài cho giáo viên môn Tin học ở trường phổ thông Việt Nam. "
                    + "Luôn viết bằng tiếng Việt, chính xác, phù hợp lứa tuổi học sinh. "
                    + "Chỉ dựa trên thông tin bài học được cung cấp, không bịa thêm dữ kiện. "
                    + "Nếu thông tin bài học còn ít, hãy viết ở mức tổng quát và thận trọng. "
                    + "Kết quả chỉ là bản nháp để giáo viên xem xét và chỉnh sửa.";

    private GenerationPromptBuilder() {
    }

    public static Built build(String type, LessonSummary lesson,
                              List<ObjectiveInfo> objectives, Map<String, Object> params) {
        String context = context(lesson, objectives);
        return switch (type) {
            case AiGenerationRequest.TYPE_LESSON_PLAN -> lessonPlan(context, params);
            case AiGenerationRequest.TYPE_QUESTION_SET -> questionSet(context, params);
            case AiGenerationRequest.TYPE_ASSESSMENT_MATRIX -> assessmentMatrix(context, params);
            default -> throw new IllegalArgumentException("Loại yêu cầu không hợp lệ: " + type);
        };
    }

    private static Built lessonPlan(String context, Map<String, Object> params) {
        int minutes = intParam(params, "duration_minutes", 45);
        String user = context
                + "\nHãy soạn giáo án nháp cho bài học trên, thời lượng " + minutes + " phút.\n"
                + "- objectives: các mục tiêu của tiết học (ngắn gọn).\n"
                + "- activities: các hoạt động dạy học theo thứ tự, mỗi hoạt động có title, description, minutes.\n"
                + "- timing: phân bổ thời gian theo từng giai đoạn (phase, minutes); tổng minutes khoảng " + minutes + ".";
        Map<String, Object> schema = object(props(
                "objectives", array(string()),
                "activities", array(object(props(
                        "title", string(), "description", string(), "minutes", integer()))),
                "timing", array(object(props("phase", string(), "minutes", integer())))));
        return new Built(SYSTEM_PROMPT, user, schema);
    }

    private static Built questionSet(String context, Map<String, Object> params) {
        int count = intParam(params, "count", 5);
        StringBuilder user = new StringBuilder(context);
        user.append("\nHãy viết đúng ").append(count).append(" câu hỏi trắc nghiệm cho bài học trên.\n");
        Object dist = params == null ? null : params.get("cognitive_level_distribution");
        if (dist instanceof Map<?, ?> m && !m.isEmpty()) {
            user.append("Phân bổ mức nhận thức mong muốn (Bloom): ").append(m).append(".\n");
        }
        user.append("Quy tắc:\n")
                .append("- question_type chỉ là SINGLE_CHOICE (1 đáp án đúng) hoặc MULTIPLE_CHOICE (nhiều đáp án đúng).\n")
                .append("- options gồm 4 lựa chọn có id lần lượt A, B, C, D; correct_answer là danh sách id đúng, ví dụ [\"B\"].\n")
                .append("- learning_objective_id PHẢI là một trong các id trong danh sách mục tiêu ở trên, chép đúng nguyên văn.\n")
                .append("- explanation giải thích ngắn vì sao đáp án đúng.");
        Map<String, Object> schema = object(props("questions", array(object(props(
                "question_text", string(),
                "question_type", enumOf(QUESTION_TYPES),
                "cognitive_level", enumOf(COGNITIVE_LEVELS),
                "difficulty", enumOf(DIFFICULTIES),
                "learning_objective_id", string(),
                "options", array(object(props("id", string(), "text", string()))),
                "correct_answer", array(string()),
                "explanation", string())))));
        return new Built(SYSTEM_PROMPT, user.toString(), schema);
    }

    private static Built assessmentMatrix(String context, Map<String, Object> params) {
        int total = intParam(params, "total_questions", 20);
        String user = context
                + "\nHãy đề xuất ma trận đề kiểm tra gồm tổng " + total + " câu cho bài học trên.\n"
                + "- distribution: danh sách các dòng, mỗi dòng gồm learning_objective_id, cognitive_level (Bloom) và count (số câu).\n"
                + "- learning_objective_id PHẢI là một trong các id trong danh sách mục tiêu ở trên, chép đúng nguyên văn.\n"
                + "- Tổng count của mọi dòng phải bằng total_questions = " + total + ".";
        Map<String, Object> schema = object(props(
                "total_questions", integer(),
                "distribution", array(object(props(
                        "learning_objective_id", string(),
                        "cognitive_level", enumOf(COGNITIVE_LEVELS),
                        "count", integer())))));
        return new Built(SYSTEM_PROMPT, user, schema);
    }

    private static String context(LessonSummary lesson, List<ObjectiveInfo> objectives) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thông tin bài học:\n");
        sb.append("- Tiêu đề: ").append(lesson.title()).append("\n");
        sb.append("- Mô tả: ").append(lesson.description() == null || lesson.description().isBlank()
                ? "(chưa có mô tả)" : lesson.description()).append("\n");
        sb.append("- Mục tiêu học tập (id | nội dung):\n");
        if (objectives.isEmpty()) {
            sb.append("  (bài học chưa có mục tiêu nào)\n");
        }
        for (ObjectiveInfo o : objectives) {
            sb.append("  - ").append(o.id()).append(" | ").append(o.text()).append("\n");
        }
        return sb.toString();
    }

    private static int intParam(Map<String, Object> params, String key, int def) {
        if (params == null || params.get(key) == null) {
            return def;
        }
        Object v = params.get(key);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static Map<String, Object> type(String t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", t);
        return m;
    }

    private static Map<String, Object> string() {
        return type("STRING");
    }

    private static Map<String, Object> integer() {
        return type("INTEGER");
    }

    private static Map<String, Object> enumOf(List<String> values) {
        Map<String, Object> m = type("STRING");
        m.put("enum", values);
        return m;
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> m = type("ARRAY");
        m.put("items", items);
        return m;
    }

    private static Map<String, Object> object(Map<String, Object> properties) {
        Map<String, Object> m = type("OBJECT");
        m.put("properties", properties);
        m.put("required", new ArrayList<>(properties.keySet()));
        return m;
    }

    private static Map<String, Object> props(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
        return m;
    }
}
