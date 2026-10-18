package com.educmind.teacherassistant.service;

import com.educmind.teacherassistant.config.TeacherAssistantProperties;
import com.educmind.teacherassistant.repository.AiGenerationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Định kỳ đánh FAILED các request còn PROCESSING quá lâu (thường do app tắt hoặc crash giữa lúc worker chạy),
 * để giáo viên không thấy request quay vòng mãi.
 */
@Component
public class StuckRequestCleaner {

    private static final Logger log = LoggerFactory.getLogger(StuckRequestCleaner.class);

    static final String MESSAGE = "Bị gián đoạn khi xử lý (có thể do máy chủ khởi động lại), vui lòng gửi yêu cầu lại";

    private final AiGenerationRequestRepository requestRepository;
    private final TeacherAssistantProperties properties;

    public StuckRequestCleaner(AiGenerationRequestRepository requestRepository,
                               TeacherAssistantProperties properties) {
        this.requestRepository = requestRepository;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${educmind.ai.teacher-assistant.stuck-check-interval-ms:300000}")
    @Transactional
    public int sweep() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoff = now.minusMinutes(properties.getStuckMinutes());
        int n = requestRepository.failStuck(cutoff, now, MESSAGE);
        if (n > 0) {
            log.warn("Đã đánh FAILED {} AI generation request kẹt PROCESSING quá {} phút",
                    n, properties.getStuckMinutes());
        }
        return n;
    }
}
