package com.shop.ecommerce.dto;

public class OrderCheckoutResponse {

    private Long id;
    private String orderNumber;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private double subtotalAmount;
    private double shippingAmount;
    private double totalAmount;
    private boolean storeEmailSent;
    private boolean customerEmailSent;
    private String message;

    public OrderCheckoutResponse(Long id, String orderNumber, String status, String paymentMethod, String paymentStatus, double subtotalAmount, double shippingAmount, double totalAmount, boolean storeEmailSent, boolean customerEmailSent, String message) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.subtotalAmount = subtotalAmount;
        this.shippingAmount = shippingAmount;
        this.totalAmount = totalAmount;
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public double getSubtotalAmount() {
        return subtotalAmount;
    }

    public double getShippingAmount() {
        return shippingAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
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
