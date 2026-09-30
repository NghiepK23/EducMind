package com.educmind.auth.service;

import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.auth.entity.UserStatus;
import com.educmind.common.port.UserLookupPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation thật của UserLookupPort (định nghĩa ở educmind-common).
 * Adapter phía module-auth — nơi duy nhất được phép đọc trực tiếp entity User
 * để phục vụ nhu cầu tra cứu cross-module (hiện tại: module-schoolclass).
 *
 * Xem giải thích đầy đủ về "symmetric need" ở UserLookupPort javadoc.
 */
@Component
public class UserLookupPortImpl implements UserLookupPort {

    private final UserRepository userRepository;

    public UserLookupPortImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSummary> findUserSummary(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getDeletedAt() == null)
                .map(this::toSummary);
    }

    private UserSummary toSummary(User user) {
        boolean active = user.getStatus() == UserStatus.ACTIVE;
        return new UserSummary(
                user.getId(),
                user.getRole().name(),
                user.getSchoolId(),
                active
        );
    }
}
