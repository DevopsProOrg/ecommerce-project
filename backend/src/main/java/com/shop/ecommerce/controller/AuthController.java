package com.shop.ecommerce.controller;

import com.shop.ecommerce.dto.AuthRequest;
import com.shop.ecommerce.dto.AuthResponse;
import com.shop.ecommerce.dto.RegisterRequest;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.service.AuthService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request){
        return service.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request){
        return service.login(request);
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile
    ) {
        return service.checkAvailability(email, mobile);
    }
}
