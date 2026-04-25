package com.shop.ecommerce.dto;

public class ContactRequestResponse {

    private Long id;
    private String ticketNumber;
    private String status;
    private boolean emailSent;
    private String message;

    public ContactRequestResponse(Long id, String ticketNumber, String status, boolean emailSent, String message) {
        this.id = id;
        this.ticketNumber = ticketNumber;
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

    public String getTicketNumber() {
        return ticketNumber;
    }

    public boolean isEmailSent() {
        return emailSent;
    }

    public String getMessage() {
        return message;
    }
}
