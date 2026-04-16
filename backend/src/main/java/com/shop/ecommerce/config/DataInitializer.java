package com.shop.ecommerce.config;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.service.PasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Value("${app.bootstrap.admin.enabled:false}")
    private boolean bootstrapAdminEnabled;

    @Value("${app.bootstrap.admin.email:}")
    private String bootstrapAdminEmail;

    @Value("${app.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    @Value("${app.bootstrap.admin.name:Store Admin}")
    private String bootstrapAdminName;

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               ProductRepository productRepository,
                               PasswordService passwordService) {
        return args -> {
            seedAdmin(userRepository, passwordService);
            seedProducts(productRepository);
        };
    }

    private void seedAdmin(UserRepository userRepository, PasswordService passwordService) {
        if (!bootstrapAdminEnabled) {
            return;
        }

        String email = bootstrapAdminEmail == null ? "" : bootstrapAdminEmail.trim().toLowerCase();
        if (email.isBlank() || bootstrapAdminPassword == null || bootstrapAdminPassword.isBlank()) {
            return;
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        User admin = new User();
        admin.setName(bootstrapAdminName);
        admin.setEmail(email);
        admin.setPassword(passwordService.hash(bootstrapAdminPassword));
        admin.setRole("ADMIN");

        userRepository.save(admin);
    }

    private void seedProducts(ProductRepository productRepository) {
        if (productRepository.count() > 0) {
            return;
        }

        productRepository.saveAll(List.of(
                buildProduct(
                        "Premium Oxford Shirt",
                        "Shirts",
                        "Tailored cotton shirt for workwear and premium casual styling.",
                        1799.0,
                        "assets/images/products/shirt1.jpg",
                        25
                ),
                buildProduct(
                        "Classic Linen Shirt",
                        "Shirts",
                        "Breathable linen blend shirt designed for smart summer looks.",
                        1999.0,
                        "assets/images/products/shirt2.jpg",
                        18
                ),
                buildProduct(
                        "Urban Essential Tee",
                        "Tshirt",
                        "Soft premium tee with a clean silhouette for everyday layering.",
                        899.0,
                        "assets/images/products/tshirt1.jpg",
                        40
                ),
                buildProduct(
                        "Signature Graphic Tee",
                        "Tshirt",
                        "Statement t-shirt with a modern fit and durable fabric finish.",
                        999.0,
                        "assets/images/products/tshirt2.jpg",
                        32
                ),
                buildProduct(
                        "Slim Fit Chino",
                        "Pants",
                        "Versatile chinos with a structured fit for office and weekend wear.",
                        2199.0,
                        "assets/images/products/pant1.jpg",
                        20
                ),
                buildProduct(
                        "Relaxed Fit Denim",
                        "Pants",
                        "All-day comfort denim crafted for a polished casual wardrobe.",
                        2499.0,
                        "assets/images/products/pant2.jpg",
                        16
                )
        ));
    }

    private Product buildProduct(String name,
                                 String category,
                                 String description,
                                 Double price,
                                 String image,
                                 Integer stock) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(price);
        product.setImage(image);
        product.setStock(stock);
        return product;
    }
}
