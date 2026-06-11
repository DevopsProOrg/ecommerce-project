package com.shop.ecommerce.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.ecommerce.dto.AlertmanagerAlertPayload;
import com.shop.ecommerce.dto.AlertmanagerWebhookPayload;
import com.shop.ecommerce.model.MonitoringAlertEvent;
import com.shop.ecommerce.repository.MonitoringAlertEventRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class MonitoringAlertService {

    private static final int MAX_LIMIT = 500;

    private final MonitoringAlertEventRepository monitoringAlertEventRepository;
    private final ObjectMapper objectMapper;

    public MonitoringAlertService(
            MonitoringAlertEventRepository monitoringAlertEventRepository,
            ObjectMapper objectMapper
    ) {
        this.monitoringAlertEventRepository = monitoringAlertEventRepository;
        this.objectMapper = objectMapper;
    }

    public int storeWebhook(AlertmanagerWebhookPayload payload) {
        List<AlertmanagerAlertPayload> alerts = payload.getAlerts();
        if (alerts == null || alerts.isEmpty()) {
            return 0;
        }

        List<MonitoringAlertEvent> events = new ArrayList<>();
        for (AlertmanagerAlertPayload alert : alerts) {
            events.add(toEvent(payload, alert));
        }

        monitoringAlertEventRepository.saveAll(events);
        return events.size();
    }

    public List<MonitoringAlertEvent> getHistory(int limit) {
        return monitoringAlertEventRepository.findAllByOrderByReceivedAtDesc(PageRequest.of(0, sanitizeLimit(limit))).getContent();
    }

    public List<MonitoringAlertEvent> getOpenAlerts(int limit) {
        int sampleSize = Math.max(sanitizeLimit(limit) * 10, 200);
        List<MonitoringAlertEvent> recentEvents = monitoringAlertEventRepository
                .findAllByOrderByReceivedAtDesc(PageRequest.of(0, sampleSize))
                .getContent();

        Map<String, MonitoringAlertEvent> latestByFingerprint = new LinkedHashMap<>();
        for (MonitoringAlertEvent event : recentEvents) {
            latestByFingerprint.putIfAbsent(event.getFingerprint(), event);
        }

        List<MonitoringAlertEvent> openAlerts = new ArrayList<>();
        for (MonitoringAlertEvent event : latestByFingerprint.values()) {
            if (!"resolved".equalsIgnoreCase(event.getStatus())) {
                openAlerts.add(event);
            }
            if (openAlerts.size() >= sanitizeLimit(limit)) {
                break;
            }
        }
        return openAlerts;
    }

    public Map<String, Object> getSummary() {
        List<MonitoringAlertEvent> recentEvents = monitoringAlertEventRepository
                .findAllByOrderByReceivedAtDesc(PageRequest.of(0, 500))
                .getContent();

        Map<String, MonitoringAlertEvent> latestByFingerprint = new LinkedHashMap<>();
        for (MonitoringAlertEvent event : recentEvents) {
            latestByFingerprint.putIfAbsent(event.getFingerprint(), event);
        }

        int openCount = 0;
        int resolvedCount = 0;
        Map<String, Integer> severityCounts = new LinkedHashMap<>();
        for (MonitoringAlertEvent event : latestByFingerprint.values()) {
            if ("resolved".equalsIgnoreCase(event.getStatus())) {
                resolvedCount++;
            } else {
                openCount++;
            }
            String severityKey = normalizeValue(event.getSeverity(), "unknown");
            severityCounts.put(severityKey, severityCounts.getOrDefault(severityKey, 0) + 1);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("trackedAlerts", latestByFingerprint.size());
        summary.put("openAlerts", openCount);
        summary.put("resolvedAlerts", resolvedCount);
        summary.put("severityCounts", severityCounts);
        summary.put("latestEvents", recentEvents.stream().limit(10).toList());
        return summary;
    }

    public List<Map<String, Object>> getIncidents(int limit) {
        List<MonitoringAlertEvent> recentEvents = monitoringAlertEventRepository
                .findAllByOrderByReceivedAtDesc(PageRequest.of(0, Math.max(sanitizeLimit(limit) * 20, 500)))
                .getContent();

        Map<String, List<MonitoringAlertEvent>> eventsByFingerprint = new LinkedHashMap<>();
        for (MonitoringAlertEvent event : recentEvents) {
            eventsByFingerprint.computeIfAbsent(event.getFingerprint(), ignored -> new ArrayList<>()).add(event);
        }

        List<Map<String, Object>> incidents = new ArrayList<>();
        for (List<MonitoringAlertEvent> events : eventsByFingerprint.values()) {
            MonitoringAlertEvent latest = events.get(0);
            MonitoringAlertEvent oldest = events.get(events.size() - 1);
            LocalDateTime resolvedAt = events.stream()
                    .filter(event -> "resolved".equalsIgnoreCase(event.getStatus()))
                    .map(MonitoringAlertEvent::getReceivedAt)
                    .findFirst()
                    .orElse(null);

            Map<String, Object> incident = new LinkedHashMap<>();
            incident.put("fingerprint", latest.getFingerprint());
            incident.put("status", latest.getStatus());
            incident.put("alertName", latest.getAlertName());
            incident.put("serviceName", latest.getServiceName());
            incident.put("severity", latest.getSeverity());
            incident.put("environment", latest.getEnvironment());
            incident.put("summary", latest.getSummary());
            incident.put("description", latest.getDescription());
            incident.put("firstSeenAt", oldest.getReceivedAt());
            incident.put("lastSeenAt", latest.getReceivedAt());
            incident.put("resolvedAt", resolvedAt);
            incident.put("eventCount", events.size());
            incident.put("generatorUrl", latest.getGeneratorUrl());
            incidents.add(incident);

            if (incidents.size() >= sanitizeLimit(limit)) {
                break;
            }
        }
        return incidents;
    }

    private MonitoringAlertEvent toEvent(AlertmanagerWebhookPayload payload, AlertmanagerAlertPayload alert) {
        Map<String, String> labels = mergeMaps(payload.getCommonLabels(), alert.getLabels());
        Map<String, String> annotations = mergeMaps(payload.getCommonAnnotations(), alert.getAnnotations());

        MonitoringAlertEvent event = new MonitoringAlertEvent();
        event.setFingerprint(firstNonBlank(
                alert.getFingerprint(),
                buildFallbackFingerprint(labels, alert.getStartsAt())
        ));
        event.setStatus(normalizeValue(firstNonBlank(alert.getStatus(), payload.getStatus()), "unknown"));
        event.setReceiver(payload.getReceiver());
        event.setAlertName(firstNonBlank(labels.get("alertname"), "unknown-alert"));
        event.setServiceName(labels.get("service"));
        event.setSeverity(labels.get("severity"));
        event.setEnvironment(firstNonBlank(labels.get("environment"), labels.get("namespace")));
        event.setJobName(labels.get("job"));
        event.setInstance(labels.get("instance"));
        event.setSummary(annotations.get("summary"));
        event.setDescription(annotations.get("description"));
        event.setGeneratorUrl(alert.getGeneratorURL());
        event.setStartsAt(parseTimestamp(alert.getStartsAt()));
        event.setEndsAt(parseTimestamp(alert.getEndsAt()));
        event.setReceivedAt(LocalDateTime.now(ZoneOffset.UTC));
        event.setLabelsJson(toJson(labels));
        event.setAnnotationsJson(toJson(annotations));
        return event;
    }

    private Map<String, String> mergeMaps(Map<String, String> commonMap, Map<String, String> specificMap) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (commonMap != null) {
            merged.putAll(commonMap);
        }
        if (specificMap != null) {
            merged.putAll(specificMap);
        }
        return merged;
    }

    private String toJson(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Map.of() : values);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private LocalDateTime parseTimestamp(String value) {
        if (value == null || value.isBlank() || "0001-01-01T00:00:00Z".equals(value)) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String buildFallbackFingerprint(Map<String, String> labels, String startsAt) {
        return firstNonBlank(labels.get("alertname"), "unknown-alert")
                + "|"
                + firstNonBlank(labels.get("instance"), "unknown-instance")
                + "|"
                + firstNonBlank(startsAt, "unknown-start");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeValue(String value, String fallback) {
        return Objects.requireNonNullElse(firstNonBlank(value), fallback).trim().toLowerCase();
    }

    private int sanitizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return 50;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }
}
