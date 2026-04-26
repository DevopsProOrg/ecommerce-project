package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.AdminUserResponse;
import com.shop.ecommerce.dto.AdminUserUpdateRequest;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<AdminUserResponse> getAllUsers(Long adminUserId) {
        validateAdmin(adminUserId);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        validateAdmin(request == null ? null : request.getAdminUserId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        user.setMobile(normalizeOptional(request.getMobile()));
        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(request.getRole().trim().toUpperCase());
        }

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, Long adminUserId) {
        validateAdmin(adminUserId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if ("ADMIN".equalsIgnoreCase(user.getRole()) && userId.equals(adminUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own admin account");
        }
        userRepository.delete(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole(),
                user.getCreatedAt(),
                user.getPassword() == null || user.getPassword().isBlank() ? "Not set" : "Protected"
        );
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user is required");
        }
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can manage users");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
