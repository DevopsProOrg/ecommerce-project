package com.shop.ecommerce.dto;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String mobile;
    private String role;
    private String accessToken;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String name, String email, String mobile, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
    }

    public AuthResponse(Long id, String name, String email, String mobile, String role, String accessToken) {
        this(id, name, email, mobile, role);
        this.accessToken = accessToken;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
