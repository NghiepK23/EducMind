package com.educmind.reports.service;

import com.educmind.reports.entity.ReportExportRequest;
import com.educmind.reports.repository.ReportExportRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ReportExportWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportExportWorker.class);

    private final ReportExportRequestRepository repository;
    private final ExportFileStorage storage;
    private final ClassProgressReportService classProgressService;
    private final StudentProgressReportService studentProgressService;
    private final AssessmentResultReportService assessmentResultService;

    public ReportExportWorker(ReportExportRequestRepository repository,
                              ExportFileStorage storage,
                              ClassProgressReportService classProgressService,
                              StudentProgressReportService studentProgressService,
                              AssessmentResultReportService assessmentResultService) {
        this.repository = repository;
        this.storage = storage;
        this.classProgressService = classProgressService;
        this.studentProgressService = studentProgressService;
        this.assessmentResultService = assessmentResultService;
    }

    @Async
    public void process(UUID requestId) {
        ReportExportRequest req = repository.findById(requestId).orElse(null);
        if (req == null) {
            log.error("Export request {} không tồn tại", requestId);
            return;
        }
        try {
            UUID actor = req.getRequestedBy();
            Map<String, Object> p = req.getScopeParams();
            Object report = switch (req.getReportType()) {
                case "CLASS_PROGRESS" -> classProgressService.generate(
                        actor, uuid(p, "course_id"), uuid(p, "class_id"));
                case "STUDENT_PROGRESS" -> studentProgressService.generate(
                        actor, uuid(p, "student_id"), uuid(p, "course_id"));
                case "ASSESSMENT_RESULT" -> assessmentResultService.generate(
                        actor, uuid(p, "assessment_id"));
                default -> throw new IllegalStateException(
                        "Loại báo cáo chưa hỗ trợ xuất file: " + req.getReportType());
            };
            boolean pdf = "PDF".equals(req.getFormat());
            String key = pdf
                    ? storage.store(requestId, "pdf",
                            PdfReportWriter.write("Báo cáo " + req.getReportType(), report))
                    : storage.store(requestId, "csv", CsvReportWriter.write(report));
            req.markCompleted(key);
        } catch (Exception e) {
            log.warn("Export {} thất bại: {}", requestId, e.getMessage());
            req.markFailed(e.getMessage());
        }
        repository.save(req);
    }

    private static UUID uuid(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return v == null ? null : UUID.fromString(v.toString());
    }
}
