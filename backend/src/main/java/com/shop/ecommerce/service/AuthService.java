package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.AuthRequest;
import com.shop.ecommerce.dto.AuthResponse;
import com.shop.ecommerce.dto.RegisterRequest;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordService passwordService;

    public AuthService(UserRepository repo, PasswordService passwordService) {
        this.repo = repo;
        this.passwordService = passwordService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null || request.getName() == null || request.getMobile() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, mobile number, and password are required");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedMobile = normalizeMobile(request.getMobile());

        if (repo.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        if (repo.existsByMobile(normalizedMobile)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mobile number already exists");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setMobile(normalizedMobile);
        user.setPassword(passwordService.hash(request.getPassword()));
        user.setRole("CUSTOMER");

        return toResponse(repo.save(user));
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        User user = repo.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getMobile(),
                user.getRole()
        );
    }

    public java.util.Map<String, Object> checkAvailability(String email, String mobile) {
        String normalizedEmail = email == null || email.isBlank() ? null : email.trim().toLowerCase();
        String normalizedMobile = mobile == null || mobile.isBlank() ? null : normalizeMobile(mobile);
        boolean emailExists = normalizedEmail != null && repo.existsByEmail(normalizedEmail);
        boolean mobileExists = normalizedMobile != null && repo.existsByMobile(normalizedMobile);
        return java.util.Map.of(
                "emailExists", emailExists,
                "mobileExists", mobileExists,
                "emailMessage", emailExists ? "This email is already registered." : "Email is available.",
                "mobileMessage", mobileExists ? "This mobile number is already registered." : "Mobile number is available."
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeMobile(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mobile number is required");
        }
        return value.replaceAll("[^0-9+]", "").trim();
    }
}
