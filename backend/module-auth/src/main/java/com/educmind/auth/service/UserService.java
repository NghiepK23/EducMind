package com.educmind.auth.service;

import com.educmind.auth.dto.CreateUserRequest;
import com.educmind.auth.dto.CreateUserResponse;
import com.educmind.auth.dto.UserResponse;
import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Logic nghiệp vụ cho POST /api/v1/users — SCHOOL_ADMIN tạo tài khoản GV/HS (cơ chế chính).
 * Theo spec Module 1, mục A.5 rule 2 + B.5 rule 3.
 *
 * [Giải thích cross-module] Service này KHÔNG còn gọi thẳng ClassMemberRepository của
 * module-schoolclass nữa (khác bản trước) — thay vào đó dùng ClassAccessPort (định nghĩa
 * ở educmind-common, implement thật ở module-schoolclass). Lý do đổi: tránh module-auth
 * phụ thuộc Maven trực tiếp vào module-schoolclass, phòng rủi ro circular dependency khi
 * module-schoolclass sau này cần gọi ngược lại module-auth (vd lúc code Class CRUD).
 */
@Service
public class UserService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ClassAccessPort classAccessPort;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            ClassAccessPort classAccessPort,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.classAccessPort = classAccessPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_EXISTS",
                    "Email đã tồn tại",
                    HttpStatus.CONFLICT
            );
        }

        String temporaryPassword = generateTemporaryPassword();
        String passwordHash = passwordEncoder.encode(temporaryPassword);

        User user = new User(request.getEmail(), passwordHash, request.getFullName(), request.getRole());
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        if (request.getClassId() != null) {
            boolean isTeacherRole = request.getRole() == Role.TEACHER;
            classAccessPort.addMember(request.getClassId(), user.getId(), isTeacherRole);
        }

        return new CreateUserResponse(user.getId(), user.getRole(), temporaryPassword);
    }

    /**
     * GET /api/v1/users/me — Module01_Auth_SchoolClass_FINAL.md, mục A.6.
     * userId truyền vào lấy từ SecurityContextHolder (đã được JwtAuthenticationFilter set sẵn
     * ở tầng Controller) — Service không tự đọc SecurityContext, chỉ nhận id đã xác thực.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "USER_NOT_FOUND",
                        "Không tìm thấy người dùng",
                        HttpStatus.NOT_FOUND
                ));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getSchoolId(),
                user.getStatus(),
                user.isMustChangePassword()
        );
    }

    private String generateTemporaryPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
