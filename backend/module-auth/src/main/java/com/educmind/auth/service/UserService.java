package com.educmind.auth.service;

import com.educmind.auth.dto.UserResponse;
import com.educmind.auth.repository.UserRepository;
import com.educmind.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }
}
