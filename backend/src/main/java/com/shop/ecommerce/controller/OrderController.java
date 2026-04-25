package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.OrderCheckoutRequest;
import com.shop.ecommerce.dto.OrderCheckoutResponse;
import com.shop.ecommerce.service.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public OrderCheckoutResponse checkout(@RequestBody OrderCheckoutRequest request) {
        return orderService.placeOrder(request);
    }
}
