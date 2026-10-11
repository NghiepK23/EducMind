package com.educmind.reports.service;

import com.educmind.common.exception.BusinessException;
import com.educmind.reports.entity.ReportExportRequest;
import com.educmind.reports.repository.ReportExportRequestRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportExportService {

    private static final List<String> SUPPORTED = List.of(
            "CLASS_PROGRESS", "STUDENT_PROGRESS", "ASSESSMENT_RESULT");

    private final ReportExportRequestRepository repository;
    private final ReportExportWorker worker;
    private final ExportFileStorage storage;
    private final ClassProgressReportService classProgressService;
    private final StudentProgressReportService studentProgressService;
    private final AssessmentResultReportService assessmentResultService;

    public ReportExportService(ReportExportRequestRepository repository,
                               ReportExportWorker worker,
                               ExportFileStorage storage,
                               ClassProgressReportService classProgressService,
                               StudentProgressReportService studentProgressService,
                               AssessmentResultReportService assessmentResultService) {
        this.repository = repository;
        this.worker = worker;
        this.storage = storage;
        this.classProgressService = classProgressService;
        this.studentProgressService = studentProgressService;
        this.assessmentResultService = assessmentResultService;
    }

    public ReportExportRequest create(UUID actorId, Map<String, Object> body) {
        String type = str(body.get("report_type"));
        String format = body.get("format") == null ? "CSV" : str(body.get("format")).toUpperCase();
        if (type == null || (!SUPPORTED.contains(type) && !"AI_USAGE".equals(type))) {
            throw new BusinessException("INVALID_REPORT_TYPE",
                    "report_type không hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if ("AI_USAGE".equals(type)) {
            throw new BusinessException("REPORT_TYPE_NOT_AVAILABLE",
                    "Báo cáo AI_USAGE chưa hỗ trợ xuất file", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!List.of("CSV", "PDF").contains(format)) {
            throw new BusinessException("FORMAT_NOT_SUPPORTED",
                    "Chỉ hỗ trợ định dạng CSV hoặc PDF", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Object raw = body.get("scope_params");
        if (!(raw instanceof Map<?, ?> rawMap)) {
            throw new BusinessException("INVALID_SCOPE_PARAMS",
                    "Thiếu scope_params", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Map<String, Object> scope = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : rawMap.entrySet()) {
            scope.put(String.valueOf(e.getKey()), e.getValue() == null ? null : e.getValue().toString());
        }
        switch (type) {
            case "CLASS_PROGRESS" -> requireUuid(scope, "course_id", true);
            case "STUDENT_PROGRESS" -> {
                requireUuid(scope, "student_id", true);
                requireUuid(scope, "course_id", true);
            }
            default -> requireUuid(scope, "assessment_id", true);
        }
        requireUuid(scope, "class_id", false);

        switch (type) {
            case "CLASS_PROGRESS" -> classProgressService.checkAccess(
                    actorId, uuidOf(scope, "course_id"));
            case "STUDENT_PROGRESS" -> studentProgressService.checkAccess(
                    actorId, uuidOf(scope, "course_id"));
            default -> assessmentResultService.checkAccess(
                    actorId, uuidOf(scope, "assessment_id"));
        }

        ReportExportRequest saved = repository.save(new ReportExportRequest(type, actorId, scope, format));
        worker.process(saved.getId());
        return saved;
    }

    public ReportExportRequest find(UUID actorId, UUID id) {
        return repository.findByIdAndRequestedBy(id, actorId).orElseThrow(() ->
                new BusinessException("EXPORT_NOT_FOUND",
                        "Không tìm thấy yêu cầu xuất báo cáo", HttpStatus.NOT_FOUND));
    }

    public FileSystemResource file(UUID actorId, UUID id) {
        ReportExportRequest req = find(actorId, id);
        if (!ReportExportRequest.STATUS_COMPLETED.equals(req.getStatus())) {
            throw new BusinessException("EXPORT_NOT_READY",
                    "File chưa sẵn sàng (trạng thái: " + req.getStatus() + ")", HttpStatus.CONFLICT);
        }
        return storage.load(req.getFileStorageKey());
    }

    private static UUID uuidOf(Map<String, Object> scope, String key) {
        return UUID.fromString(scope.get(key).toString());
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static void requireUuid(Map<String, Object> scope, String key, boolean required) {
        Object v = scope.get(key);
        if (v == null) {
            if (required) {
                throw new BusinessException("INVALID_SCOPE_PARAMS",
                        "Thiếu " + key + " trong scope_params", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return;
        }
        try {
            UUID.fromString(v.toString());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_SCOPE_PARAMS",
                    key + " không phải UUID hợp lệ", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
