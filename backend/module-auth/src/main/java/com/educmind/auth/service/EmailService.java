package com.educmind.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gửi email chứa link đặt lại mật khẩu.
 *
 * Ở môi trường dev chưa cấu hình SMTP, việc gửi sẽ lỗi — khi đó chỉ ghi log link
 * (WARN) để dev vẫn thao tác được, API không bị 500. Ở production phải cấu hình
 * MAIL_USERNAME/MAIL_PASSWORD để mail thực sự đi.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String linkBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${educmind.password-reset.link-base-url:http://localhost:5173/reset-password}") String linkBaseUrl
    ) {
        this.mailSender = mailSender;
        this.linkBaseUrl = linkBaseUrl;
    }

    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = linkBaseUrl + "?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("EducMind — Đặt lại mật khẩu");
        message.setText(
                "Chào bạn,\n\n"
                + "Ai đó (có thể là bạn) vừa yêu cầu đặt lại mật khẩu cho tài khoản EducMind.\n"
                + "Bấm vào liên kết sau để đặt mật khẩu mới (liên kết hết hạn sau 15 phút):\n\n"
                + resetLink + "\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này.\n\n"
                + "Trân trọng,\nEducMind"
        );

        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.warn("Không gửi được email tới {} ({}). Link đặt mật khẩu: {}", toEmail, e.getMessage(), resetLink);
        }
    }
}
