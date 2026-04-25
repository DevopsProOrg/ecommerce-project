package com.shop.ecommerce.dto;

public class OrderCheckoutResponse {

    private Long id;
    private String orderNumber;
    private String status;
    private boolean storeEmailSent;
    private boolean customerEmailSent;
    private String message;

    public OrderCheckoutResponse(Long id, String orderNumber, String status, boolean storeEmailSent, boolean customerEmailSent, String message) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.storeEmailSent = storeEmailSent;
        this.customerEmailSent = customerEmailSent;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getStatus() {
        return status;
    }

    public boolean isStoreEmailSent() {
        return storeEmailSent;
    }

    public boolean isCustomerEmailSent() {
        return customerEmailSent;
    }

    public String getMessage() {
        return message;
    }
}
