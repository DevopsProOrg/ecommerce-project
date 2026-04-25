package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ContactRequestPayload;
import com.shop.ecommerce.dto.ContactRequestResponse;
import com.shop.ecommerce.dto.TicketArchiveRequest;
import com.shop.ecommerce.dto.TicketResponseRequest;
import com.shop.ecommerce.model.ContactRequest;
import com.shop.ecommerce.service.ContactRequestService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/contact-requests")
public class ContactRequestController {

    private final ContactRequestService contactRequestService;

    public ContactRequestController(ContactRequestService contactRequestService) {
        this.contactRequestService = contactRequestService;
    }

    @GetMapping
    public List<ContactRequest> list(
            @RequestParam Long adminUserId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String search
    ) {
        return contactRequestService.listAll(adminUserId, status, priority, archived, search);
    }

    @GetMapping("/admin/{adminUserId}")
    public List<ContactRequest> listForAdmin(@PathVariable Long adminUserId) {
        return contactRequestService.listAll(adminUserId, null, null, false, null);
    }

    @PostMapping
    public ContactRequestResponse submit(@RequestBody ContactRequestPayload payload) {
        return contactRequestService.submit(payload);
    }

    @PutMapping("/{ticketId}/respond")
    public ContactRequest respond(@PathVariable Long ticketId, @RequestBody TicketResponseRequest request) {
        return contactRequestService.respond(ticketId, request);
    }

    @PutMapping("/respond/{ticketId}")
    public ContactRequest respondLegacy(@PathVariable Long ticketId, @RequestBody TicketResponseRequest request) {
        return contactRequestService.respond(ticketId, request);
    }

    @PutMapping("/{ticketId}/archive")
    public ContactRequest archive(@PathVariable Long ticketId, @RequestBody TicketArchiveRequest request) {
        return contactRequestService.archive(ticketId, request);
    }
}
