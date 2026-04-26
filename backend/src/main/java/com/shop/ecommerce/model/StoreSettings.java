package com.shop.ecommerce.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

@Entity
@Data
public class StoreSettings {

    @Id
    private Long id;

    private String brandName;
    private String tagline;
    private String announcementBar;
    private String heroTitle;
    private String heroSubtitle;
    private String featuredMessage;
    private String contactPhone;
    private String contactEmail;
    private String storeAddress;
    private String openingHours;
    private String instagramUrl;
    @Lob
    private String paymentQrImageUrl;
    private String paymentUpiId;
    @Lob
    private String paymentInstructions;
    private Boolean cashOnDeliveryEnabled;
}
