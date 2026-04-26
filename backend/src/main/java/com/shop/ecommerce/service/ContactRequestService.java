package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.ContactRequestPayload;
import com.shop.ecommerce.dto.ContactRequestResponse;
import com.shop.ecommerce.dto.TicketArchiveRequest;
import com.shop.ecommerce.dto.TicketBulkActionRequest;
import com.shop.ecommerce.dto.TicketResponseRequest;
import com.shop.ecommerce.model.ContactRequest;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ContactRequestRepository;
import com.shop.ecommerce.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final StoreSettingsService storeSettingsService;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@ogfashions.local}")
    private String mailFrom;

    public ContactRequestService(
            ContactRequestRepository contactRequestRepository,
            StoreSettingsService storeSettingsService,
            JavaMailSender mailSender,
            UserRepository userRepository
    ) {
        this.contactRequestRepository = contactRequestRepository;
        this.storeSettingsService = storeSettingsService;
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }

    public ContactRequestResponse submit(ContactRequestPayload payload) {
        ContactRequest request = new ContactRequest();
        request.setName(requireValue(payload.getName(), "Name"));
        request.setEmail(requireEmail(payload.getEmail()));
        request.setSubject(requireValue(payload.getSubject(), "Subject"));
        request.setPhone(normalizeOptional(payload.getPhone()));
        request.setOrderId(normalizeOptional(payload.getOrderId()));
        request.setIssue(requireValue(payload.getIssue(), "Issue details"));
        request.setStatus("NEW");
        request.setPriority("MEDIUM");
        request.setChannel("WEBSITE");
        request.setArchived(false);
        request.setAssigneeName("Support Desk");
        request.setEmailSent(false);
        request.setCustomerEmailSent(false);

        ContactRequest saved = contactRequestRepository.save(request);
        saved.setTicketNumber(formatTicketNumber(saved.getId()));
        saved = contactRequestRepository.save(saved);
        StoreSettings settings = storeSettingsService.getSettings();

        if (mailEnabled) {
            try {
                sendStoreEmail(saved, settings);
                sendCustomerAcknowledgement(saved, settings);
                saved.setEmailSent(true);
                saved.setCustomerEmailSent(true);
                saved.setStatus("NEW");
                contactRequestRepository.save(saved);
                return new ContactRequestResponse(saved.getId(), saved.getTicketNumber(), saved.getStatus(), true,
                        "Support ticket " + saved.getTicketNumber() + " was created and emailed successfully.");
            } catch (MailException | MessagingException ex) {
                saved.setStatus("OPEN");
                contactRequestRepository.save(saved);
                return new ContactRequestResponse(saved.getId(), saved.getTicketNumber(), saved.getStatus(), false,
                        "Support ticket " + saved.getTicketNumber() + " was saved, but email sending failed. Check SMTP settings.");
            }
        }

        saved.setStatus("NEW");
        contactRequestRepository.save(saved);
        return new ContactRequestResponse(saved.getId(), saved.getTicketNumber(), saved.getStatus(), false,
                "Support ticket " + saved.getTicketNumber() + " was created and saved in the admin inbox.");
    }

    public List<ContactRequest> listAll(Long adminUserId, String status, String priority, Boolean archived, String search) {
        validateAdmin(adminUserId);
        String normalizedStatus = normalizeFilter(status);
        String normalizedPriority = normalizeFilter(priority);
        String normalizedSearch = normalizeSearch(search);
        boolean includeArchived = archived != null && archived;

        return contactRequestRepository.findAll()
                .stream()
                .filter(request -> includeArchived || !request.isArchived())
                .filter(request -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(request.getStatus()))
                .filter(request -> normalizedPriority == null || normalizedPriority.equalsIgnoreCase(request.getPriority()))
                .filter(request -> matchesSearch(request, normalizedSearch))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
    }

    public ContactRequest respond(Long ticketId, TicketResponseRequest payload) {
        validateAdmin(payload.getAdminUserId());
        ContactRequest request = contactRequestRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        String nextStatus = normalizeStatus(payload.getStatus());
        String nextPriority = normalizePriority(payload.getPriority());
        String responseMessage = requireValue(payload.getResponseMessage(), "Response message");

        request.setAdminReply(responseMessage);
        request.setStatus(nextStatus);
        request.setPriority(nextPriority == null ? request.getPriority() : nextPriority);
        request.setAssigneeName(normalizeOptional(payload.getAssigneeName()).isBlank()
                ? request.getAssigneeName()
                : normalizeOptional(payload.getAssigneeName()));
        request.setInternalNote(normalizeOptional(payload.getInternalNote()));
        request.setArchived(false);
        request.setLastRespondedAt(LocalDateTime.now());
        StoreSettings settings = storeSettingsService.getSettings();
        if (mailEnabled) {
            try {
                sendCustomerReply(request, responseMessage, settings);
                request.setCustomerEmailSent(true);
            } catch (MailException | MessagingException ex) {
                request.setCustomerEmailSent(false);
            }
        }

        return contactRequestRepository.save(request);
    }

    public ContactRequest archive(Long ticketId, TicketArchiveRequest payload) {
        validateAdmin(payload.getAdminUserId());
        ContactRequest request = contactRequestRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        request.setArchived(payload.isArchived());
        if (payload.isArchived()) {
            request.setStatus("CLOSED");
        }
        return contactRequestRepository.save(request);
    }

    public List<ContactRequest> archiveBulk(TicketBulkActionRequest payload) {
        Long adminUserId = payload == null ? null : payload.getAdminUserId();
        validateAdmin(adminUserId);
        List<Long> ticketIds = requireTicketIds(payload == null ? null : payload.getTicketIds());
        boolean archived = payload.getArchived() != null && payload.getArchived();

        List<ContactRequest> requests = contactRequestRepository.findAllById(ticketIds);
        if (requests.size() != ticketIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more tickets could not be found");
        }

        requests.forEach(request -> {
            request.setArchived(archived);
            if (archived) {
                request.setStatus("CLOSED");
            }
        });
        return contactRequestRepository.saveAll(requests);
    }

    public void delete(Long ticketId, Long adminUserId) {
        validateAdmin(adminUserId);
        if (!contactRequestRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }
        contactRequestRepository.deleteById(ticketId);
    }

    public int deleteBulk(TicketBulkActionRequest payload) {
        Long adminUserId = payload == null ? null : payload.getAdminUserId();
        validateAdmin(adminUserId);
        List<Long> ticketIds = requireTicketIds(payload == null ? null : payload.getTicketIds());
        Set<Long> requestedIds = ticketIds.stream().collect(Collectors.toSet());
        List<ContactRequest> requests = contactRequestRepository.findAllById(ticketIds);
        Set<Long> foundIds = requests.stream().map(ContactRequest::getId).collect(Collectors.toSet());
        if (!foundIds.containsAll(requestedIds)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more tickets could not be found");
        }
        contactRequestRepository.deleteAll(requests);
        return requests.size();
    }

    private void sendStoreEmail(ContactRequest request, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(settings.getContactEmail());
        helper.setReplyTo(request.getEmail());
        helper.setSubject(settings.getBrandName() + " support ticket " + request.getTicketNumber());
        helper.setText(buildStoreTicketEmail(request, settings), true);
        mailSender.send(message);
    }

    private void sendCustomerAcknowledgement(ContactRequest request, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(request.getEmail());
        helper.setReplyTo(settings.getContactEmail());
        helper.setSubject("Your " + settings.getBrandName() + " support ticket " + request.getTicketNumber());
        helper.setText(buildCustomerTicketEmail(request, settings), true);
        mailSender.send(message);
    }

    private void sendCustomerReply(ContactRequest request, String responseMessage, StoreSettings settings) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(request.getEmail());
        helper.setReplyTo(settings.getContactEmail());
        helper.setSubject("Update on ticket " + request.getTicketNumber() + " - " + settings.getBrandName());
        helper.setText(buildCustomerReplyEmail(request, responseMessage, settings), true);
        mailSender.send(message);
    }

    private String buildStoreTicketEmail(ContactRequest request, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#b45309;text-transform:uppercase;">New Support Ticket</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">%s</h2>
                    <div style="margin-bottom:16px;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Ticket ID:</strong> %s<br>
                      <strong>Status:</strong> %s<br>
                      <strong>Priority:</strong> %s<br>
                      <strong>Channel:</strong> %s<br>
                      <strong>Customer:</strong> %s<br>
                      <strong>Email:</strong> %s<br>
                      <strong>Order ID:</strong> %s
                    </div>
                    <div style="margin-bottom:16px;">
                      <strong>Subject</strong>
                      <div>%s</div>
                    </div>
                    <div>
                      <strong>Issue details</strong>
                      <div style="white-space:pre-wrap;">%s</div>
                    </div>
                  </div>
                </div>
                """.formatted(
                escapeHtml(settings.getBrandName() + " customer support request"),
                escapeHtml(request.getTicketNumber()),
                escapeHtml(request.getStatus()),
                escapeHtml(request.getPriority()),
                escapeHtml(request.getChannel()),
                escapeHtml(request.getName()),
                escapeHtml(request.getEmail()),
                escapeHtml(blankFallback(request.getOrderId())),
                escapeHtml(request.getSubject()),
                escapeHtml(request.getIssue())
        );
    }

    private String buildCustomerTicketEmail(ContactRequest request, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#1d4ed8;text-transform:uppercase;">Ticket Created</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">Your support ticket is now live</h2>
                    <p>Hello %s,</p>
                    <p>We received your request and created ticket <strong>%s</strong>.</p>
                    <div style="margin:16px 0;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Ticket ID:</strong> %s<br>
                      <strong>Subject:</strong> %s<br>
                      <strong>Priority:</strong> %s<br>
                      <strong>Order ID:</strong> %s<br>
                      <strong>Status:</strong> %s
                    </div>
                    <div style="margin-bottom:16px;white-space:pre-wrap;">%s</div>
                    <p>You can reply to this email or contact us at <a href="mailto:%s">%s</a>.</p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(request.getName()),
                escapeHtml(request.getTicketNumber()),
                escapeHtml(request.getTicketNumber()),
                escapeHtml(request.getSubject()),
                escapeHtml(request.getPriority()),
                escapeHtml(blankFallback(request.getOrderId())),
                escapeHtml(request.getStatus()),
                escapeHtml(request.getIssue()),
                escapeHtml(settings.getContactEmail()),
                escapeHtml(settings.getContactEmail())
        );
    }

    private String buildCustomerReplyEmail(ContactRequest request, String responseMessage, StoreSettings settings) {
        return """
                <div style="font-family:Arial,sans-serif;color:#172033;line-height:1.6;max-width:720px;margin:0 auto;">
                  <div style="padding:24px;border:1px solid #e5e7eb;border-radius:20px;background:#ffffff;">
                    <div style="font-size:12px;font-weight:700;letter-spacing:.12em;color:#1d4ed8;text-transform:uppercase;">Ticket Update</div>
                    <h2 style="margin:8px 0 12px;font-size:28px;">Ticket %s has a new update</h2>
                    <p>Hello %s,</p>
                    <div style="margin:16px 0;padding:12px 14px;border-radius:14px;background:#f8fafc;">
                      <strong>Status:</strong> %s<br>
                      <strong>Priority:</strong> %s<br>
                      <strong>Subject:</strong> %s<br>
                      <strong>Order ID:</strong> %s
                    </div>
                    <div style="margin-bottom:16px;">
                      <strong>Our response</strong>
                      <div style="white-space:pre-wrap;">%s</div>
                    </div>
                    <div style="margin-bottom:16px;">
                      <strong>Your original issue</strong>
                      <div style="white-space:pre-wrap;">%s</div>
                    </div>
                    <p>For more help, email <a href="mailto:%s">%s</a>.</p>
                  </div>
                </div>
                """.formatted(
                escapeHtml(request.getTicketNumber()),
                escapeHtml(request.getName()),
                escapeHtml(request.getStatus()),
                escapeHtml(request.getPriority()),
                escapeHtml(request.getSubject()),
                escapeHtml(blankFallback(request.getOrderId())),
                escapeHtml(responseMessage),
                escapeHtml(request.getIssue()),
                escapeHtml(settings.getContactEmail()),
                escapeHtml(settings.getContactEmail())
        );
    }

    private String requireValue(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " is required");
        }
        return value.trim();
    }

    private String requireEmail(String email) {
        String value = requireValue(email, "Email");
        if (!value.contains("@") || value.startsWith("@") || value.endsWith("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required");
        }
        return value;
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatTicketNumber(Long id) {
        return String.format(Locale.ENGLISH, "%04d-T-ID", id);
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeOptional(value).toUpperCase(Locale.ENGLISH);
        if (normalized.isBlank()) {
            return "PENDING";
        }
        return switch (normalized) {
            case "NEW", "OPEN", "PENDING", "HOLD", "RESOLVED", "CLOSED" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported ticket status");
        };
    }

    private String normalizePriority(String value) {
        String normalized = normalizeOptional(value).toUpperCase(Locale.ENGLISH);
        if (normalized.isBlank()) {
            return null;
        }
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "URGENT" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported ticket priority");
        };
    }

    private String normalizeFilter(String value) {
        String normalized = normalizeOptional(value).toUpperCase(Locale.ENGLISH);
        return normalized.isBlank() || "ALL".equals(normalized) ? null : normalized;
    }

    private String normalizeSearch(String value) {
        String normalized = normalizeOptional(value).toLowerCase(Locale.ENGLISH);
        return normalized.isBlank() ? null : normalized;
    }

    private boolean matchesSearch(ContactRequest request, String search) {
        if (search == null) {
            return true;
        }
        return containsIgnoreCase(request.getTicketNumber(), search)
                || containsIgnoreCase(request.getName(), search)
                || containsIgnoreCase(request.getEmail(), search)
                || containsIgnoreCase(request.getSubject(), search)
                || containsIgnoreCase(request.getOrderId(), search)
                || containsIgnoreCase(request.getIssue(), search)
                || containsIgnoreCase(request.getAssigneeName(), search);
    }

    private List<Long> requireTicketIds(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one ticket");
        }
        return ticketIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(search);
    }

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user is required");
        }

        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can access support requests");
        }
    }
}
