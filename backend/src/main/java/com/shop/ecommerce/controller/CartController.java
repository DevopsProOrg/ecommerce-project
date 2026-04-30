package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.CartItemRequest;
import com.shop.ecommerce.dto.CartItemResponse;
import com.shop.ecommerce.model.CartItem;
import com.shop.ecommerce.service.CartService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public CartItemResponse add(@RequestBody CartItemRequest request){
        return service.upsertCartItem(request);
    }

    @GetMapping("/user/{userId}")
    public List<CartItemResponse> getCart(@PathVariable Long userId){
        return service.getCart(userId);
    }

    @DeleteMapping("/user/{userId}/product/{productId}")
    public void remove(@PathVariable Long userId, @PathVariable Long productId) {
        service.removeCartItem(userId, productId);
    }

    @DeleteMapping("/user/{userId}")
    public void clear(@PathVariable Long userId) {
        service.clearCart(userId);
    }

}
