package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.StoreSettingsRequest;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.StoreSettingsRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoreSettingsService {

    private static final long SETTINGS_ID = 1L;

    private final StoreSettingsRepository storeSettingsRepository;
    private final UserRepository userRepository;

    public StoreSettingsService(StoreSettingsRepository storeSettingsRepository, UserRepository userRepository) {
        this.storeSettingsRepository = storeSettingsRepository;
        this.userRepository = userRepository;
    }

    public StoreSettings getSettings() {
        StoreSettings settings = storeSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);
        return normalizeStoredSettings(settings);
    }

    public StoreSettings updateSettings(StoreSettingsRequest request) {
        validateAdmin(request.getAdminUserId());

        StoreSettings settings = getSettings();
        settings.setBrandName(resolveRequired(request.getBrandName(), settings.getBrandName(), "Brand name"));
        settings.setTagline(resolveRequired(request.getTagline(), settings.getTagline(), "Tagline"));
        settings.setAnnouncementBar(resolveRequired(request.getAnnouncementBar(), settings.getAnnouncementBar(), "Announcement bar"));
        settings.setHeroTitle(resolveRequired(request.getHeroTitle(), settings.getHeroTitle(), "Hero title"));
        settings.setHeroSubtitle(resolveRequired(request.getHeroSubtitle(), settings.getHeroSubtitle(), "Hero subtitle"));
        settings.setFeaturedMessage(resolveRequired(request.getFeaturedMessage(), settings.getFeaturedMessage(), "Featured message"));
        settings.setContactPhone(normalizeOptional(request.getContactPhone()));
        settings.setContactEmail(resolveRequired(request.getContactEmail(), settings.getContactEmail(), "Contact email"));
        settings.setStoreAddress(resolveRequired(request.getStoreAddress(), settings.getStoreAddress(), "Store address"));
        settings.setOpeningHours(resolveRequired(request.getOpeningHours(), settings.getOpeningHours(), "Opening hours"));
        settings.setInstagramUrl(normalizeOptional(request.getInstagramUrl()));
        settings.setPaymentQrImageUrl(normalizeOptional(request.getPaymentQrImageUrl()));
        settings.setPaymentUpiId(normalizeOptional(request.getPaymentUpiId(), "contact.ogfashions@upi"));
        settings.setPaymentInstructions(normalizeOptional(request.getPaymentInstructions(), "Scan the QR code, complete the payment, and enter your payment reference before placing the order."));
        settings.setCashOnDeliveryEnabled(request.getCashOnDeliveryEnabled() == null || request.getCashOnDeliveryEnabled());

        return storeSettingsRepository.save(settings);
    }

    public StoreSettings createDefaultSettings() {
        StoreSettings settings = new StoreSettings();
        settings.setId(SETTINGS_ID);
        settings.setBrandName("OG Fashions");
        settings.setTagline("Hyderabad's fashion and accessories marketplace.");
        settings.setAnnouncementBar("New arrivals, fashion essentials, watches, pouches, and everyday picks shipped across India.");
        settings.setHeroTitle("Build your lifestyle catalog with OG Fashions.");
        settings.setHeroSubtitle("Sell fashion, accessories, watches, mobile pouches, and curated everyday essentials from one modern storefront backed by Spring Boot and MySQL.");
        settings.setFeaturedMessage("Launch apparel drops, accessory collections, and gifting picks from one admin-managed marketplace.");
        settings.setContactPhone("");
        settings.setContactEmail("contact.ogfashions@gmail.com");
        settings.setStoreAddress("OG Fashion, New green city Budhwel, Rajendhranagar, Hyderabad.\n300030, India");
        settings.setOpeningHours("Monday - Saturday: 10:00 AM - 8:30 PM\nSunday: Closed");
        settings.setInstagramUrl("https://www.instagram.com/ogfashions");
        settings.setPaymentQrImageUrl("");
        settings.setPaymentUpiId("contact.ogfashions@upi");
        settings.setPaymentInstructions("Scan the QR code, complete the payment, and enter your payment reference before placing the order.");
        settings.setCashOnDeliveryEnabled(true);
        return storeSettingsRepository.save(settings);
    }

    private StoreSettings normalizeStoredSettings(StoreSettings settings) {
        boolean changed = false;
        if (settings.getBrandName() == null || settings.getBrandName().isBlank()) {
            settings.setBrandName("OG Fashions");
            changed = true;
        }
        if (settings.getTagline() == null || settings.getTagline().isBlank()) {
            settings.setTagline("Signature styles from Hyderabad.");
            changed = true;
        }
        if (settings.getAnnouncementBar() == null || settings.getAnnouncementBar().isBlank()) {
            settings.setAnnouncementBar("New arrivals, flash sales, fashion essentials, watches, pouches, and gifting picks shipped across India.");
            changed = true;
        }
        if (settings.getHeroTitle() == null || settings.getHeroTitle().isBlank()) {
            settings.setHeroTitle("Own your everyday style with OG Fashions.");
            changed = true;
        }
        if (settings.getHeroSubtitle() == null || settings.getHeroSubtitle().isBlank()) {
            settings.setHeroSubtitle("Fresh catalog drops and elevated essentials for Hyderabad shoppers.");
            changed = true;
        }
        if (settings.getFeaturedMessage() == null || settings.getFeaturedMessage().isBlank()) {
            settings.setFeaturedMessage("Sharper product browsing with richer filters and quick detail views.");
            changed = true;
        }
        if (settings.getContactEmail() == null
                || settings.getContactEmail().isBlank()
                || "contact@ogfashions.com".equalsIgnoreCase(settings.getContactEmail())) {
            settings.setContactEmail("contact.ogfashions@gmail.com");
            changed = true;
        }
        if (settings.getStoreAddress() == null || settings.getStoreAddress().isBlank()) {
            settings.setStoreAddress("OG Fashion, New green city Budhwel, Rajendhranagar, Hyderabad.\n300030, India");
            changed = true;
        }
        if (settings.getOpeningHours() == null || settings.getOpeningHours().isBlank()) {
            settings.setOpeningHours("Monday - Saturday: 10:00 AM - 8:30 PM\nSunday: Closed");
            changed = true;
        }
        if (settings.getContactPhone() == null) {
            settings.setContactPhone("");
            changed = true;
        }
        if (settings.getInstagramUrl() == null) {
            settings.setInstagramUrl("");
            changed = true;
        }
        if (settings.getPaymentQrImageUrl() == null) {
            settings.setPaymentQrImageUrl("");
            changed = true;
        }
        if (settings.getPaymentUpiId() == null || settings.getPaymentUpiId().isBlank()) {
            settings.setPaymentUpiId("contact.ogfashions@upi");
            changed = true;
        }
        if (settings.getPaymentInstructions() == null || settings.getPaymentInstructions().isBlank()) {
            settings.setPaymentInstructions("Scan the QR code, complete the payment, and enter your payment reference before placing the order.");
            changed = true;
        }
        if (settings.getCashOnDeliveryEnabled() == null) {
            settings.setCashOnDeliveryEnabled(true);
            changed = true;
        }
        if (changed) {
            return storeSettingsRepository.save(settings);
        }
        return settings;
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user is required");
        }

        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can update store settings");
        }
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        return value.trim();
    }

    private String resolveRequired(String candidate, String currentValue, String label) {
        if (candidate != null && !candidate.isBlank()) {
            return candidate.trim();
        }
        return requireValue(currentValue, label);
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}
