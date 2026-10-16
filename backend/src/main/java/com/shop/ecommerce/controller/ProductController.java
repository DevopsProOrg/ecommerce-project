package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.ProductRequest;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> getProducts(){
        return service.getAllProducts();
    }

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return service.getProduct(id);
    }

    @PostMapping
    public Product addProduct(@RequestBody ProductRequest request){
        return service.addProduct(request);
    }

    @PutMapping("/{id}")
    public Product updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        return service.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id, @AuthenticationPrincipal Jwt principal){
        service.deleteProduct(id, Long.valueOf(principal.getSubject()));
    }
}
