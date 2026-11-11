package com.educmind.common.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Port cấp tài khoản học sinh (US-T00 / AC-T00.2): giáo viên thêm học sinh bằng email,
 * nếu chưa có tài khoản thì hệ thống tạo tài khoản và gửi đường dẫn đặt mật khẩu.
 */
public interface AccountPort {

    Optional<UUID> findUserIdByEmail(String email);

    /**
     * Tạo tài khoản STUDENT với mật khẩu ngẫu nhiên (không dùng được ngay) và gửi email
     * chứa link đặt mật khẩu. Trả về id tài khoản vừa tạo.
     */
    UUID createStudentAccount(String email, String fullName);
}
