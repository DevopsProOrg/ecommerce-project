package com.shop.ecommerce.dto;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private LocalDateTime createdAt;
    private String passwordStatus;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String name, String email, String mobile, String role, LocalDateTime createdAt, String passwordStatus) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.createdAt = createdAt;
        this.passwordStatus = passwordStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPasswordStatus() {
        return passwordStatus;
    }

    public void setPasswordStatus(String passwordStatus) {
        this.passwordStatus = passwordStatus;
    }
}
