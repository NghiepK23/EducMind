package com.educmind.auth.service;

import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.AccountPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * US-T00 / AC-T00.2: giáo viên thêm học sinh bằng email. Nếu chưa có tài khoản thì tạo
 * tài khoản STUDENT với mật khẩu ngẫu nhiên (không ai biết) rồi gửi link đặt mật khẩu.
 */
@Component
public class AccountPortImpl implements AccountPort {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService resetTokenService;
    private final EmailService emailService;

    public AccountPortImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           PasswordResetTokenService resetTokenService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetTokenService = resetTokenService;
        this.emailService = emailService;
    }

    @Override
    public Optional<UUID> findUserIdByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim()).map(User::getId);
    }

    @Override
    @Transactional
    public UUID createStudentAccount(String email, String fullName) {
        String normalized = email.trim();
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(normalized)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "Email đã được sử dụng", HttpStatus.CONFLICT);
        }

        String randomPassword = UUID.randomUUID() + "-" + UUID.randomUUID();
        User user = userRepository.save(new User(
                normalized,
                passwordEncoder.encode(randomPassword),
                fullName == null || fullName.isBlank() ? normalized : fullName.trim(),
                Role.STUDENT
        ));

        String rawToken = resetTokenService.issue(user.getId());
        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        return user.getId();
    }
}
