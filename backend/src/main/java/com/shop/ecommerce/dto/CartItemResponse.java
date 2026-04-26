package com.shop.ecommerce.dto;

public class CartItemResponse {

    private Long id;
    private Long productId;
    private String name;
    private String category;
    private String description;
    private Double price;
    private String image;
    private Integer stock;
    private Integer quantity;
    private Boolean deliveryChargeEnabled;
    private Double deliveryChargeAmount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getDeliveryChargeEnabled() {
        return deliveryChargeEnabled;
    }

    public void setDeliveryChargeEnabled(Boolean deliveryChargeEnabled) {
        this.deliveryChargeEnabled = deliveryChargeEnabled;
    }

    public Double getDeliveryChargeAmount() {
        return deliveryChargeAmount;
    }

    public void setDeliveryChargeAmount(Double deliveryChargeAmount) {
        this.deliveryChargeAmount = deliveryChargeAmount;
    }
}
