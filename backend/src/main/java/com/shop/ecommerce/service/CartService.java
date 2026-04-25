package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.CartItemRequest;
import com.shop.ecommerce.dto.CartItemResponse;
import com.shop.ecommerce.model.CartItem;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.CartRepository;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<CartItemResponse> getCart(Long userId) {
        validateUser(userId);
        return cartRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CartItemResponse upsertCartItem(CartItemRequest request) {
        if (request.getUserId() == null || request.getProductId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User and product are required");
        }

        int quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        validateUser(request.getUserId());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        CartItem item = cartRepository.findByUserIdAndProductId(request.getUserId(), request.getProductId())
                .orElseGet(CartItem::new);

        item.setUserId(request.getUserId());
        item.setProductId(product.getId());
        item.setQuantity(quantity);

        return toResponse(cartRepository.save(item));
    }

    public void removeCartItem(Long userId, Long productId) {
        validateUser(userId);

        CartItem item = cartRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        cartRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        validateUser(userId);
        cartRepository.deleteByUserId(userId);
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private CartItemResponse toResponse(CartItem item) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(product.getId());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setImage(product.getImage());
        response.setStock(product.getStock());
        response.setQuantity(item.getQuantity());
        return response;
    }
}
