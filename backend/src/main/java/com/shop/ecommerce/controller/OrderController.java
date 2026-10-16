package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.OrderBulkActionRequest;
import com.shop.ecommerce.dto.OrderCheckoutRequest;
import com.shop.ecommerce.dto.OrderCheckoutResponse;
import com.shop.ecommerce.dto.OrderStatusUpdateRequest;
import com.shop.ecommerce.model.ShopOrder;
import com.shop.ecommerce.service.OrderService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderCheckoutResponse checkout(@RequestBody OrderCheckoutRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setUserId(currentUserId(principal));
        return orderService.placeOrder(request);
    }

    @GetMapping("/user/{userId}")
    public List<ShopOrder> getUserOrders(@PathVariable Long userId, @AuthenticationPrincipal Jwt principal) {
        if (!userId.equals(currentUserId(principal))) {
            throw new AccessDeniedException("You may only access your own orders");
        }
        return orderService.getUserOrders(userId);
    }

    @GetMapping
    public List<ShopOrder> getAllOrders(@AuthenticationPrincipal Jwt principal) {
        return orderService.getAllOrders(currentUserId(principal));
    }

    @PutMapping("/{orderId}/status")
    public ShopOrder updateOrderStatus(@PathVariable Long orderId, @RequestBody OrderStatusUpdateRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentUserId(principal));
        return orderService.updateOrderStatus(orderId, request);
    }

    @PutMapping("/{orderId}/archive")
    public ShopOrder archiveOrder(@PathVariable Long orderId, @RequestBody OrderBulkActionRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentUserId(principal));
        return orderService.archiveOrder(orderId, request.getAdminUserId(), request.isArchived());
    }

    @PutMapping("/bulk/archive")
    public Map<String, Object> archiveOrders(@RequestBody OrderBulkActionRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentUserId(principal));
        return Map.of(
                "updatedCount", orderService.archiveOrders(request),
                "archived", request.isArchived()
        );
    }

    @DeleteMapping("/{orderId}")
    public Map<String, Object> deleteOrder(@PathVariable Long orderId, @AuthenticationPrincipal Jwt principal) {
        orderService.deleteOrder(orderId, currentUserId(principal));
        return Map.of("deleted", true, "orderId", orderId);
    }

    @DeleteMapping("/bulk")
    public Map<String, Object> deleteOrders(@RequestBody OrderBulkActionRequest request, @AuthenticationPrincipal Jwt principal) {
        request.setAdminUserId(currentUserId(principal));
        return Map.of("deletedCount", orderService.deleteOrders(request));
    }

    private Long currentUserId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }
}
