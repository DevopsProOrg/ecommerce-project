package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.CartItemRequest;
import com.shop.ecommerce.dto.CartItemResponse;
import com.shop.ecommerce.model.CartItem;
import com.shop.ecommerce.service.CartService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public CartItemResponse add(@RequestBody CartItemRequest request, @AuthenticationPrincipal Jwt principal){
        request.setUserId(currentUserId(principal));
        return service.upsertCartItem(request);
    }

    @GetMapping("/user/{userId}")
    public List<CartItemResponse> getCart(@PathVariable Long userId, @AuthenticationPrincipal Jwt principal){
        requireOwnAccount(userId, principal);
        return service.getCart(userId);
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public void remove(@PathVariable Long userId, @PathVariable Long productId, @AuthenticationPrincipal Jwt principal) {
        requireOwnAccount(userId, principal);
        service.removeCartItem(userId, productId);
    }

    @DeleteMapping("/user/{userId}")
    public void clear(@PathVariable Long userId, @AuthenticationPrincipal Jwt principal) {
        requireOwnAccount(userId, principal);
        service.clearCart(userId);
    }

    private void requireOwnAccount(Long userId, Jwt principal) {
        if (!userId.equals(currentUserId(principal))) {
            throw new AccessDeniedException("You may only access your own cart");
        }
    }

    private Long currentUserId(Jwt principal) {
        return Long.valueOf(principal.getSubject());
    }

}
