package com.educmind.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Gui email cho luong password reset - Module 1, muc A.6.
 * Chi co dung 1 trach nhiem: soan va gui email chua link reset, khong biet gi ve
 * logic sinh/validate token (do la trach nhiem cua AuthService + PasswordResetTokenRepository).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String linkBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${educmind.password-reset.link-base-url}") String linkBaseUrl
    ) {
        this.mailSender = mailSender;
        this.linkBaseUrl = linkBaseUrl;
    }

    /** Gui email chua link reset password toi dia chi email cua user. rawToken la token THO, chua hash. */
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String resetLink = linkBaseUrl + "?token=" + rawToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("EducMind - Yeu cau dat lai mat khau");
        message.setText(
                "Chao ban,\n\n"
                + "Ban (hoac ai do) vua yeu cau dat lai mat khau cho tai khoan EducMind cua ban.\n"
                + "Bam vao link sau de dat lai mat khau (link co hieu luc trong 15 phut):\n\n"
                + resetLink + "\n\n"
                + "Neu ban khong yeu cau viec nay, vui long bo qua email nay.\n\n"
                + "Tran trong,\nEducMind"
        );

        mailSender.send(message);
    }
}
