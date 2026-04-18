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
        return storeSettingsRepository.findById(SETTINGS_ID)
                .orElseGet(this::createDefaultSettings);
    }

    public StoreSettings updateSettings(StoreSettingsRequest request) {
        validateAdmin(request.getAdminUserId());

        StoreSettings settings = getSettings();
        settings.setBrandName(requireValue(request.getBrandName(), "Brand name"));
        settings.setTagline(requireValue(request.getTagline(), "Tagline"));
        settings.setAnnouncementBar(requireValue(request.getAnnouncementBar(), "Announcement bar"));
        settings.setHeroTitle(requireValue(request.getHeroTitle(), "Hero title"));
        settings.setHeroSubtitle(requireValue(request.getHeroSubtitle(), "Hero subtitle"));
        settings.setFeaturedMessage(requireValue(request.getFeaturedMessage(), "Featured message"));
        settings.setContactPhone(requireValue(request.getContactPhone(), "Contact phone"));
        settings.setContactEmail(requireValue(request.getContactEmail(), "Contact email"));
        settings.setStoreAddress(requireValue(request.getStoreAddress(), "Store address"));
        settings.setOpeningHours(requireValue(request.getOpeningHours(), "Opening hours"));
        settings.setInstagramUrl(normalizeOptional(request.getInstagramUrl()));

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
        settings.setContactPhone("+91 7780139445");
        settings.setContactEmail("contact@ogfashions.com");
        settings.setStoreAddress("OG Fashion, New green city Budhwel, Rajendhranagar, Hyderabad.\n300030, India");
        settings.setOpeningHours("Monday - Saturday: 10:00 AM - 8:30 PM\nSunday: Closed");
        settings.setInstagramUrl("https://www.instagram.com/ogfashions");
        return storeSettingsRepository.save(settings);
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

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }
}
