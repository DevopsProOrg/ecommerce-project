package com.shop.ecommerce.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private String subcategory;

    private String brand;

    private String sku;

    private String description;

    @Column(length = 1000)
    private String highlights;

    private String badge;

    private Double price;

    private Double originalPrice;

    private String image;

    private Integer stock;

    private String color;

    private String material;

    private String sizeInfo;

    private String deliveryInfo;

    private Double rating;

    private Integer reviewCount;

    private Boolean featured;
}
