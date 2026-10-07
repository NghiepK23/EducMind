package com.educmind.auth.service;

import com.educmind.auth.entity.User;
import com.educmind.auth.entity.UserStatus;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.port.UserAnnouncementPort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adapter cho module-notification đọc danh sách recipient của system announcement. */
@Service
public class UserAnnouncementPortImpl implements UserAnnouncementPort {

    private final UserRepository userRepository;

    public UserAnnouncementPortImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UUID> findSchoolIdByUserId(UUID userId) {
        return userRepository.findById(userId).map(User::getSchoolId);
    }

    @Override
    public List<UUID> findActiveUserIdsBySchoolId(UUID schoolId) {
        return userRepository.findBySchoolIdAndStatusAndDeletedAtIsNull(schoolId, UserStatus.ACTIVE)
                .stream()
                .map(User::getId)
                .toList();
    }

    @Override
    public List<UUID> findAllActiveUserIds() {
        return userRepository.findByStatusAndDeletedAtIsNull(UserStatus.ACTIVE)
                .stream()
                .map(User::getId)
                .toList();
    }

    @Override
    public List<UUID> filterActiveUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIdInAndStatusAndDeletedAtIsNull(userIds, UserStatus.ACTIVE)
                .stream()
                .map(User::getId)
                .toList();
    }
}
