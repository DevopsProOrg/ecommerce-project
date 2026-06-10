package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.AlertmanagerWebhookPayload;
import com.shop.ecommerce.model.MonitoringAlertEvent;
import com.shop.ecommerce.service.MonitoringAlertService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitoring/alerts")
public class MonitoringAlertController {

    private final MonitoringAlertService monitoringAlertService;

    public MonitoringAlertController(MonitoringAlertService monitoringAlertService) {
        this.monitoringAlertService = monitoringAlertService;
    }

    @PostMapping("/webhook")
    public Map<String, Object> receiveWebhook(@RequestBody AlertmanagerWebhookPayload payload) {
        int storedEvents = monitoringAlertService.storeWebhook(payload);
        return Map.of(
                "storedEvents", storedEvents,
                "receiver", payload.getReceiver(),
                "status", payload.getStatus()
        );
    }

    @GetMapping("/history")
    public List<MonitoringAlertEvent> history(@RequestParam(defaultValue = "100") int limit) {
        return monitoringAlertService.getHistory(limit);
    }

    @GetMapping("/open")
    public List<MonitoringAlertEvent> openAlerts(@RequestParam(defaultValue = "50") int limit) {
        return monitoringAlertService.getOpenAlerts(limit);
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return monitoringAlertService.getSummary();
    }
}
