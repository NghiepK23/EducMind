package com.educmind.auth.service;

import com.educmind.auth.dto.ChangePasswordForcedRequest;
import com.educmind.auth.dto.PasswordResetConfirmRequest;
import com.educmind.auth.dto.PasswordResetRequestRequest;
import com.educmind.auth.dto.RefreshRequest;
import com.educmind.auth.dto.RefreshResponse;
import com.educmind.auth.dto.RegisterRequest;
import com.educmind.auth.dto.RegisterResponse;
import com.educmind.auth.entity.InviteCode;
import com.educmind.auth.entity.PasswordResetToken;
import com.educmind.auth.entity.RefreshToken;
import com.educmind.auth.entity.Role;
import com.educmind.auth.entity.User;
import com.educmind.auth.dto.LoginRequest;
import com.educmind.auth.dto.LoginResponse;
import com.educmind.auth.repository.InviteCodeRepository;
import com.educmind.auth.repository.RefreshTokenRepository;
import com.educmind.auth.repository.UserRepository;
import com.educmind.auth.security.JwtService;
import com.educmind.common.exception.BusinessException;
import com.educmind.common.port.ClassAccessPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Logic nghiệp vụ cho luồng Authentication (Module 1, Phần A).
 * register(): self-register bằng invite code — kênh phụ, theo mục A.5 rule 2 + B.5 rule 3.
 *
 * [Giải thích cross-module] Không còn gọi thẳng ClassMemberRepository của module-schoolclass
 * (khác bản trước) — dùng ClassAccessPort, cùng lý do đã giải thích ở UserService.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final ClassAccessPort classAccessPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            InviteCodeRepository inviteCodeRepository,
            ClassAccessPort classAccessPort,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenService passwordResetTokenService,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.inviteCodeRepository = inviteCodeRepository;
        this.classAccessPort = classAccessPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenService = passwordResetTokenService;
        this.emailService = emailService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "Sai email hoac mat khau",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "INVALID_CREDENTIALS",
                    "Sai email hoac mat khau",
                    HttpStatus.UNAUTHORIZED
            );
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name(), user.getSchoolId());
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId());

        return new LoginResponse(accessToken, refreshToken, user.isMustChangePassword());
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    "EMAIL_ALREADY_EXISTS",
                    "Email đã tồn tại",
                    HttpStatus.CONFLICT
            );
        }

        InviteCode inviteCode = inviteCodeRepository.findByCode(request.getInviteCode())
                .orElseThrow(() -> new BusinessException(
                        "INVITE_CODE_INVALID",
                        "Invite code is invalid or expired",
                        HttpStatus.BAD_REQUEST
                ));

        if (!inviteCode.isUsable()) {
            throw new BusinessException(
                    "INVITE_CODE_INVALID",
                    "Invite code is invalid or expired",
                    HttpStatus.BAD_REQUEST
            );
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), passwordHash, request.getFullName(), inviteCode.getIntendedRole());
        user.setSchoolId(inviteCode.getSchoolId());
        user = userRepository.save(user);

        boolean isTeacherRole = inviteCode.getIntendedRole() == Role.TEACHER;
        classAccessPort.addMember(inviteCode.getClassId(), user.getId(), isTeacherRole);

        inviteCode.incrementUsedCount();
        inviteCodeRepository.save(inviteCode);

        return new RegisterResponse(user.getId(), user.getRole(), user.getSchoolId(), inviteCode.getClassId());
    }

    /**
     * POST /api/v1/auth/refresh — Module 1, muc A.6.
     * [DECIDED - xac nhan voi Nghiep] Ap dung refresh token rotation: token cu bi revoke
     * NGAY sau khi validate thanh cong, cap 1 cap access+refresh token HOAN TOAN MOI.
     * Ly do rotation an toan hon: neu 1 refresh token bi lo va ca ke tan cong lan chu so huu
     * cung dung, ben nao dung SAU se bi loi ngay (dau hieu phat hien tan cong som).
     */
    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken oldToken = refreshTokenService.findValidTokenOrThrow(request.getRefreshToken());

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "TOKEN_EXPIRED",
                        "Refresh token khong hop le hoac da het han",
                        HttpStatus.UNAUTHORIZED
                ));

        refreshTokenService.revokeToken(oldToken);

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name(), user.getSchoolId());
        String newRefreshToken = refreshTokenService.issueRefreshToken(user.getId());

        return new RefreshResponse(newAccessToken, newRefreshToken);
    }

    /**
     * POST /api/v1/auth/logout — Module 1, muc A.6.
     * Chi revoke DUNG 1 refresh token duoc client gui len (thiet bi dang logout),
     * KHONG revoke toan bo token cua user (khac voi doi mat khau - muc A.5 rule 5,
     * vi logout tren 1 thiet bi khong nen anh huong cac thiet bi khac dang dang nhap).
     */
    @Transactional
    public void logout(RefreshRequest request) {
        RefreshToken token = refreshTokenService.findValidTokenOrThrow(request.getRefreshToken());
        refreshTokenService.revokeToken(token);
    }

    /**
     * POST /api/v1/auth/password-reset/request - Module 1, muc A.6.
     * [Quyet dinh bao mat, khong hoi lai] Neu email khong ton tai trong he thong, VAN tra ve
     * thanh cong (khong nem loi) - chong "user enumeration" (OWASP): neu bao loi ro "email
     * khong ton tai", ke xau co the do ra danh sach email da dang ky bang cach thu hang loat.
     */
    @Transactional
    public void requestPasswordReset(PasswordResetRequestRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String rawToken = passwordResetTokenService.issueResetToken(user.getId());
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
    }

    /**
     * POST /api/v1/auth/password-reset/confirm - Module 1, muc A.6.
     * Sau khi doi mat khau thanh cong: [DECIDED - muc A.5 rule 5] revoke TOAN BO refresh
     * token cua user (khac logout - chi revoke 1 token) + danh dau reset token da dung
     * (dung 1 lan duy nhat - muc A.5.4).
     */
    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenService.findValidTokenOrThrow(request.getToken());

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "RESET_TOKEN_INVALID",
                        "Password reset token khong hop le hoac da het han",
                        HttpStatus.BAD_REQUEST
                ));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        passwordResetTokenService.markUsed(resetToken);
        refreshTokenRepository.revokeAllByUserId(user.getId(), OffsetDateTime.now());
    }

    /**
     * POST /api/v1/auth/change-password-forced - Module 1, muc A.6.
     * Chi goi duoc khi must_change_password=true (da chan boi JwtAuthenticationFilter cho
     * moi endpoint khac). [DECIDED - xac nhan voi Nghiep]:
     * (1) Bat buoc nhap lai currentPassword de xac thuc lai truoc khi doi.
     * (2) Doi thanh cong -> revoke TOAN BO refresh token cua user, giong het rule 5 (muc A.5)
     *     da ap dung o confirmPasswordReset - vi day cung la 1 truong hop "doi mat khau thanh cong".
     */
    @Transactional
    public void changePasswordForced(UUID userId, ChangePasswordForcedRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "INVALID_CREDENTIALS",
                        "Nguoi dung khong ton tai",
                        HttpStatus.UNAUTHORIZED
                ));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(
                    "CURRENT_PASSWORD_INVALID",
                    "Mat khau hien tai khong dung",
                    HttpStatus.UNAUTHORIZED
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId(), OffsetDateTime.now());
    }
}
