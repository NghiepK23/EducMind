package com.educmind.auth.repository;

import com.educmind.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * [Giải thích cho người mới] JpaRepository<User, UUID> tự động cung cấp sẵn các hàm cơ bản
 * (findById, save, delete, findAll...) — bạn KHÔNG cần tự viết. Chỉ cần khai báo thêm các hàm
 * truy vấn đặc thù nghiệp vụ, Spring Data JPA sẽ tự sinh code cài đặt dựa theo TÊN hàm
 * (query derivation) — vd "findByEmail" tự hiểu là "SELECT * FROM users WHERE email = ?".
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
