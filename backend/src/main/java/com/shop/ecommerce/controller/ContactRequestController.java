package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ContactRequestPayload;
import com.shop.ecommerce.dto.ContactRequestResponse;
import com.shop.ecommerce.model.ContactRequest;
import com.shop.ecommerce.service.ContactRequestService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public List<ContactRequest> list(@RequestParam Long adminUserId) {
        return contactRequestService.listAll(adminUserId);
    }

    @GetMapping("/admin/{adminUserId}")
    public List<ContactRequest> listForAdmin(@PathVariable Long adminUserId) {
        return contactRequestService.listAll(adminUserId);
    }

    @PostMapping
    public ContactRequestResponse submit(@RequestBody ContactRequestPayload payload) {
        return contactRequestService.submit(payload);
    }
}
