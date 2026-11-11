package com.educmind.auth.service;

import com.educmind.auth.entity.User;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.port.UserLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserLookupPortImpl implements UserLookupPort {

    private final UserRepository userRepository;

    public UserLookupPortImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSummary> findUserSummary(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(u -> new UserSummary(u.getId(), u.getRole().name(), true));
    }

    @Override
    public List<UserDetail> findUserDetails(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> byId = new HashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            if (u.isActive()) {
                byId.put(u.getId(), u);
            }
        }
        List<UserDetail> result = new ArrayList<>();
        for (UUID id : userIds) {
            User u = byId.get(id);
            if (u != null) {
                result.add(new UserDetail(u.getId(), u.getFullName(), u.getEmail(), u.getRole().name()));
            }
        }
        return result;
    }
}
