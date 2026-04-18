package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.StoreSettingsRequest;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.service.StoreSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/store-settings")
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    public StoreSettingsController(StoreSettingsService storeSettingsService) {
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public StoreSettings getSettings() {
        return storeSettingsService.getSettings();
    }

    @PutMapping
    public StoreSettings updateSettings(@RequestBody StoreSettingsRequest request) {
        return storeSettingsService.updateSettings(request);
    }
}
