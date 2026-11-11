package com.educmind.analytics.service;

import com.educmind.analytics.dto.ClassMasteryResponse;
import com.educmind.analytics.dto.EvidenceRow;
import com.educmind.analytics.dto.ObjectiveDetailRow;
import com.educmind.analytics.dto.ObjectiveMasteryRow;
import com.educmind.analytics.dto.ObjectiveSummaryRow;
import com.educmind.analytics.dto.StudentMasteryDetailResponse;
import com.educmind.analytics.dto.StudentMasteryRow;
import com.educmind.analytics.entity.EvidenceSource;
import com.educmind.analytics.entity.LearningEvidence;
import com.educmind.analytics.entity.Mastery;
import com.educmind.analytics.repository.LearningEvidenceRepository;
import com.educmind.analytics.repository.MasteryRepository;
import com.educmind.common.port.ClassAccessPort;
import com.educmind.common.port.CourseAccessPort;
import com.educmind.common.port.CourseContentPort;
import com.educmind.common.port.UserLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Truy vấn phân tích học tập.
 * US-T07 (giáo viên xem theo lớp, AC-T07.1…07.5) và US-S05 (học sinh xem của mình, AC-S05.1…05.4).
 */
@Service
public class AnalyticsQueryService {

    private final ClassAccessPort classAccessPort;
    private final CourseAccessPort courseAccessPort;
    private final CourseContentPort contentPort;
    private final UserLookupPort userLookupPort;
    private final MasteryRepository masteryRepository;
    private final LearningEvidenceRepository evidenceRepository;
    private final AnalyticsPermission permission;
    private final AnalyticsSettings settings;

    public AnalyticsQueryService(ClassAccessPort classAccessPort,
                                 CourseAccessPort courseAccessPort,
                                 CourseContentPort contentPort,
                                 UserLookupPort userLookupPort,
                                 MasteryRepository masteryRepository,
                                 LearningEvidenceRepository evidenceRepository,
                                 AnalyticsPermission permission,
                                 AnalyticsSettings settings) {
        this.classAccessPort = classAccessPort;
        this.courseAccessPort = courseAccessPort;
        this.contentPort = contentPort;
        this.userLookupPort = userLookupPort;
        this.masteryRepository = masteryRepository;
        this.evidenceRepository = evidenceRepository;
        this.permission = permission;
        this.settings = settings;
    }

    /** Bảng mastery của lớp: từng học sinh × từng mục tiêu + trung bình lớp (AC-T07.1/02/03). */
    @Transactional(readOnly = true)
    public ClassMasteryResponse classMastery(UUID teacherId, UUID classId, UUID topicId, UUID lessonId) {
        permission.requireClassOwner(teacherId, classId);

        List<CourseContentPort.ObjectiveInfo> objectives = classObjectives(teacherId, classId, topicId, lessonId);
        List<UUID> studentIds = classAccessPort.findStudentIds(classId);
        Map<String, Mastery> mastery = masteryIndex(studentIds);
        List<UserLookupPort.UserDetail> details = userLookupPort.findUserDetails(studentIds);

        List<StudentMasteryRow> students = new ArrayList<>();
        for (UserLookupPort.UserDetail detail : details) {
            List<ObjectiveMasteryRow> rows = new ArrayList<>();
            for (CourseContentPort.ObjectiveInfo objective : objectives) {
                rows.add(toMasteryRow(objective, mastery.get(key(detail.id(), objective.id()))));
            }
            students.add(new StudentMasteryRow(detail.id(), detail.fullName(), detail.email(), rows));
        }

        List<ObjectiveSummaryRow> summary = new ArrayList<>();
        for (CourseContentPort.ObjectiveInfo objective : objectives) {
            double total = 0d;
            int withData = 0;
            for (UserLookupPort.UserDetail detail : details) {
                Mastery m = mastery.get(key(detail.id(), objective.id()));
                if (m != null && m.getMasteryScore() != null) {
                    total += m.getMasteryScore().doubleValue();
                    withData++;
                }
            }
            Double average = withData == 0 ? null : total / withData;
            summary.add(new ObjectiveSummaryRow(objective.id(), objective.code(), objective.description(),
                    objective.lessonId(), objective.lessonTitle(), objective.topicId(), objective.topicTitle(),
                    MasteryLevel.of(average, settings).name(), scale(average), withData, studentIds.size()));
        }
        return new ClassMasteryResponse(classId, students, summary);
    }

    /** AC-T07.4: từng mục tiêu của một học sinh kèm các kết quả đánh giá tạo nên nó. */
    @Transactional(readOnly = true)
    public StudentMasteryDetailResponse studentMasteryForTeacher(UUID teacherId, UUID classId, UUID studentId,
                                                                 UUID topicId, UUID lessonId) {
        permission.requireClassOwner(teacherId, classId);
        permission.requireStudentInClass(classId, studentId);

        List<CourseContentPort.ObjectiveInfo> objectives = classObjectives(teacherId, classId, topicId, lessonId);
        Map<String, Mastery> mastery = masteryIndex(List.of(studentId));
        Map<UUID, List<LearningEvidence>> evidences = evidenceByObjective(List.of(studentId),
                objectiveIds(objectives));

        List<ObjectiveDetailRow> rows = new ArrayList<>();
        for (CourseContentPort.ObjectiveInfo objective : objectives) {
            Mastery m = mastery.get(key(studentId, objective.id()));
            rows.add(toDetailRow(objective, m,
                    evidences.getOrDefault(objective.id(), List.of()), true));
        }
        UserLookupPort.UserDetail detail = userLookupPort.findUserDetails(List.of(studentId)).stream()
                .findFirst().orElse(null);
        String fullName = detail == null ? "" : detail.fullName();
        String email = detail == null ? "" : detail.email();
        return new StudentMasteryDetailResponse(studentId, fullName, email, rows);
    }

    /** AC-S05.1…05.4: mastery của chính học sinh, kèm bài học/chủ đề và kết quả đánh giá. */
    @Transactional(readOnly = true)
    public List<ObjectiveDetailRow> myMastery(UUID studentId) {
        List<CourseContentPort.ObjectiveInfo> objectives = studentObjectives(studentId);
        Map<String, Mastery> mastery = masteryIndex(List.of(studentId));
        Map<UUID, List<LearningEvidence>> evidences = evidenceByObjective(List.of(studentId),
                objectiveIds(objectives));

        List<ObjectiveDetailRow> rows = new ArrayList<>();
        for (CourseContentPort.ObjectiveInfo objective : objectives) {
            rows.add(toDetailRow(objective, mastery.get(key(studentId, objective.id())),
                    evidences.getOrDefault(objective.id(), List.of()), false));
        }
        return rows;
    }

    /** Mục tiêu của lớp (mọi khóa học thuộc lớp), lọc theo chủ đề/bài học nếu có (AC-T07.5). */
    public List<CourseContentPort.ObjectiveInfo> classObjectives(UUID teacherId, UUID classId,
                                                                 UUID topicId, UUID lessonId) {
        List<CourseContentPort.ObjectiveInfo> result = new ArrayList<>();
        for (UUID courseId : courseAccessPort.findAccessibleCourseIdsForTeacher(teacherId)) {
            if (!classId.equals(courseAccessPort.findClassId(courseId).orElse(null))) {
                continue;
            }
            result.addAll(filter(contentPort.findObjectiveInfosByCourse(courseId), topicId, lessonId));
        }
        return result;
    }

    /** Mục tiêu thuộc các khóa học mà học sinh đang theo học. */
    public List<CourseContentPort.ObjectiveInfo> studentObjectives(UUID studentId) {
        List<CourseContentPort.ObjectiveInfo> result = new ArrayList<>();
        for (UUID courseId : courseAccessPort.findCourseIdsForStudent(studentId)) {
            result.addAll(contentPort.findObjectiveInfosByCourse(courseId));
        }
        return result;
    }

    /** Khoá "studentId:objectiveId" → mastery. */
    public Map<String, Mastery> masteryIndex(Collection<UUID> studentIds) {
        Map<String, Mastery> result = new HashMap<>();
        if (studentIds.isEmpty()) {
            return result;
        }
        for (Mastery mastery : masteryRepository.findByStudentIdIn(studentIds)) {
            result.put(key(mastery.getStudentId(), mastery.getObjectiveId()), mastery);
        }
        return result;
    }

    private Map<UUID, List<LearningEvidence>> evidenceByObjective(Collection<UUID> studentIds,
                                                                 Set<UUID> objectiveIds) {
        Map<UUID, List<LearningEvidence>> result = new LinkedHashMap<>();
        if (studentIds.isEmpty()) {
            return result;
        }
        for (LearningEvidence evidence : evidenceRepository.findByStudentIdInOrderByUpdatedAtDesc(studentIds)) {
            if (objectiveIds.isEmpty() || !objectiveIds.contains(evidence.getObjectiveId())) {
                continue;
            }
            result.computeIfAbsent(evidence.getObjectiveId(), k -> new ArrayList<>()).add(evidence);
        }
        return result;
    }

    private ObjectiveMasteryRow toMasteryRow(CourseContentPort.ObjectiveInfo objective, Mastery mastery) {
        Double score = mastery == null || mastery.getMasteryScore() == null
                ? null : mastery.getMasteryScore().doubleValue();
        return new ObjectiveMasteryRow(objective.id(), objective.code(), objective.description(),
                objective.lessonId(), objective.lessonTitle(), objective.lessonPublished(),
                objective.topicId(), objective.topicTitle(),
                MasteryLevel.of(score, settings).name(),
                mastery == null ? null : mastery.getMasteryScore(),
                mastery == null ? 0 : mastery.getEvidenceCount());
    }

    private ObjectiveDetailRow toDetailRow(CourseContentPort.ObjectiveInfo objective, Mastery mastery,
                                           List<LearningEvidence> evidences, boolean teacherView) {
        Double score = mastery == null || mastery.getMasteryScore() == null
                ? null : mastery.getMasteryScore().doubleValue();
        List<EvidenceRow> rows = new ArrayList<>();
        for (LearningEvidence evidence : evidences) {
            rows.add(new EvidenceRow(evidence.getSourceType().name(), evidence.getSourceId(),
                    evidence.getScore(), evidence.getUpdatedAt(), targetType(evidence),
                    teacherView ? targetApi(evidence) : null));
        }
        return new ObjectiveDetailRow(objective.id(), objective.code(), objective.description(),
                objective.lessonId(), objective.lessonTitle(), objective.lessonPublished(),
                objective.topicId(), objective.topicTitle(),
                MasteryLevel.of(score, settings).name(),
                mastery == null ? null : mastery.getMasteryScore(),
                mastery == null ? 0 : mastery.getEvidenceCount(), rows);
    }

    private String targetType(LearningEvidence evidence) {
        return evidence.getSourceType() == EvidenceSource.ASSIGNMENT ? "ASSIGNMENT_SUBMISSION" : "ASSESSMENT";
    }

    private String targetApi(LearningEvidence evidence) {
        return evidence.getSourceType() == EvidenceSource.ASSIGNMENT
                ? "/api/v1/submissions/" + evidence.getSourceId()
                : "/api/v1/assessments/" + evidence.getSourceId() + "/attempts";
    }

    private List<CourseContentPort.ObjectiveInfo> filter(List<CourseContentPort.ObjectiveInfo> objectives,
                                                          UUID topicId, UUID lessonId) {
        List<CourseContentPort.ObjectiveInfo> result = new ArrayList<>();
        for (CourseContentPort.ObjectiveInfo objective : objectives) {
            if (topicId != null && !topicId.equals(objective.topicId())) {
                continue;
            }
            if (lessonId != null && !lessonId.equals(objective.lessonId())) {
                continue;
            }
            result.add(objective);
        }
        return result;
    }

    private Set<UUID> objectiveIds(List<CourseContentPort.ObjectiveInfo> objectives) {
        Set<UUID> ids = new HashSet<>();
        for (CourseContentPort.ObjectiveInfo objective : objectives) {
            ids.add(objective.id());
        }
        return ids;
    }

    private BigDecimal scale(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP);
    }

    private String key(UUID studentId, UUID objectiveId) {
        return studentId + ":" + objectiveId;
    }
}
