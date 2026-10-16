package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ContactRequestPayload;
import com.shop.ecommerce.dto.ContactRequestResponse;
import com.shop.ecommerce.dto.TicketArchiveRequest;
import com.shop.ecommerce.dto.TicketBulkActionRequest;
import com.shop.ecommerce.dto.TicketResponseRequest;
import com.shop.ecommerce.model.ContactRequest;
import com.shop.ecommerce.service.ContactRequestService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/contact-requests")
public class ContactRequestController {

    private final ContactRequestService contactRequestService;

    public ContactRequestController(ContactRequestService contactRequestService) {
        this.contactRequestService = contactRequestService;
    }

    @GetMapping
    public List<ContactRequest> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt principal
    ) {
        return contactRequestService.listAll(currentAdminId(principal), status, priority, archived, search);
    }

    @GetMapping("/admin/{adminUserId}")
    public List<ContactRequest> listForAdmin(@PathVariable Long adminUserId, @AuthenticationPrincipal Jwt principal) {
        Long currentAdminId = currentAdminId(principal);
        if (!currentAdminId.equals(adminUserId)) throw new AccessDeniedException("Admin identity does not match the authenticated account");
        return contactRequestService.listAll(currentAdminId, null, null, false, null);
    }

    @PostMapping
    public ContactRequestResponse submit(@RequestBody ContactRequestPayload payload) {
        return contactRequestService.submit(payload);
    }

    @PutMapping("/{ticketId}/respond")
    public ContactRequest respond(@PathVariable Long ticketId, @RequestBody TicketResponseRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        return contactRequestService.respond(ticketId, request);
    }

    @PutMapping("/respond/{ticketId}")
    public ContactRequest respondLegacy(@PathVariable Long ticketId, @RequestBody TicketResponseRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        return contactRequestService.respond(ticketId, request);
    }

    @PutMapping("/{ticketId}/archive")
    public ContactRequest archive(@PathVariable Long ticketId, @RequestBody TicketArchiveRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        return contactRequestService.archive(ticketId, request);
    }

    @PutMapping("/bulk/archive")
    public List<ContactRequest> archiveBulk(@RequestBody TicketBulkActionRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        return contactRequestService.archiveBulk(request);
    }

    @DeleteMapping("/{ticketId}")
    public Map<String, Object> delete(@PathVariable Long ticketId, @AuthenticationPrincipal Jwt principal) {
        contactRequestService.delete(ticketId, currentAdminId(principal));
        return Map.of("deleted", true, "ticketId", ticketId);
    }

    @DeleteMapping("/bulk")
    public Map<String, Object> deleteBulk(@RequestBody TicketBulkActionRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentAdminId(principal));
        int deletedCount = contactRequestService.deleteBulk(request);
        return Map.of("deleted", true, "deletedCount", deletedCount);
    }

    private Long currentAdminId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}
