package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.ContactRequestPayload;
import com.shop.ecommerce.dto.ContactRequestResponse;
import com.shop.ecommerce.model.ContactRequest;
import com.shop.ecommerce.model.StoreSettings;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ContactRequestRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

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
        request.setPhone(normalizeOptional(payload.getPhone()));
        request.setOrderId(normalizeOptional(payload.getOrderId()));
        request.setIssue(requireValue(payload.getIssue(), "Issue details"));
        request.setStatus("RECEIVED");
        request.setEmailSent(false);

        ContactRequest saved = contactRequestRepository.save(request);
        StoreSettings settings = storeSettingsService.getSettings();

        if (mailEnabled) {
            try {
                sendEmail(saved, settings);
                saved.setEmailSent(true);
                saved.setStatus("EMAIL_SENT");
                contactRequestRepository.save(saved);
                return new ContactRequestResponse(saved.getId(), saved.getStatus(), true,
                        "Your message was submitted and emailed successfully.");
            } catch (MailException ex) {
                saved.setStatus("EMAIL_FAILED");
                contactRequestRepository.save(saved);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Your request was saved, but email sending failed. Check SMTP settings and try again.");
            }
        }

        saved.setStatus("SAVED_ONLY");
        contactRequestRepository.save(saved);
        return new ContactRequestResponse(saved.getId(), saved.getStatus(), false,
                "Support request submitted successfully. It is saved in the admin inbox and ready for follow-up.");
    }

    public List<ContactRequest> listAll(Long adminUserId) {
        validateAdmin(adminUserId);
        return contactRequestRepository.findAll()
                .stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
    }

    private void sendEmail(ContactRequest request, StoreSettings settings) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(settings.getContactEmail());
        message.setReplyTo(request.getEmail());
        message.setSubject("OG Fashions contact request #" + request.getId());
        message.setText(buildMessageBody(request, settings));
        mailSender.send(message);
    }

    private String buildMessageBody(ContactRequest request, StoreSettings settings) {
        return """
                New support request for %s

                Request ID: %s
                Customer Name: %s
                Customer Email: %s
                Customer Phone: %s
                Order ID: %s

                Issue Details:
                %s

                This message was sent from the OG Fashions contact form.
                """.formatted(
                settings.getBrandName(),
                request.getId(),
                request.getName(),
                request.getEmail(),
                blankFallback(request.getPhone()),
                blankFallback(request.getOrderId()),
                request.getIssue()
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

    private String blankFallback(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
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
