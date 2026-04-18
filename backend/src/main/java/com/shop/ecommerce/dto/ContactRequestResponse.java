package com.shop.ecommerce.dto;

public class ContactRequestResponse {

    private Long id;
    private String status;
    private boolean emailSent;
    private String message;

    public ContactRequestResponse(Long id, String status, boolean emailSent, String message) {
        this.id = id;
        this.status = status;
        this.emailSent = emailSent;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public String getMessage() {
        return message;
    }
}
