package com.educmind.notification.scheduler;

import com.educmind.notification.service.DueSoonNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;

@Configuration
@EnableScheduling
public class DueSoonScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(DueSoonScheduler.class);

    private final DueSoonNotificationService service;

    public DueSoonScheduler(DueSoonNotificationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${educmind.notification.due-soon-interval-ms:3600000}",
            initialDelayString = "${educmind.notification.due-soon-initial-delay-ms:60000}")
    public void run() {
        try {
            int n = service.notifyDueSoon(OffsetDateTime.now());
            LOG.info("ASSIGNMENT_DUE_SOON: đã tạo {} notification", n);
        } catch (Exception ex) {
            LOG.error("Job ASSIGNMENT_DUE_SOON thất bại", ex);
        }
    }
}
