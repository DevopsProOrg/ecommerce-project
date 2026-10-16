package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.AdminUserResponse;
import com.shop.ecommerce.dto.AdminUserUpdateRequest;
import com.shop.ecommerce.service.AdminUserService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> getUsers(@AuthenticationPrincipal Jwt principal) {
        return adminUserService.getAllUsers(currentAdminId(principal));
    }

    @PutMapping("/{userId}")
    public AdminUserResponse updateUser(@PathVariable Long userId, @RequestBody AdminUserUpdateRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        return adminUserService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    public Map<String, Object> deleteUser(@PathVariable Long userId, @AuthenticationPrincipal Jwt principal) {
        adminUserService.deleteUser(userId, currentAdminId(principal));
        return Map.of("deleted", true, "userId", userId);
    }

    private Long currentAdminId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}
