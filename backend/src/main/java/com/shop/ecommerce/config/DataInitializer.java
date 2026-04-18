package com.shop.ecommerce.config;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.StoreSettingsRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.service.PasswordService;
import com.shop.ecommerce.service.StoreSettingsService;
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
                               PasswordService passwordService,
                               StoreSettingsService storeSettingsService) {
        return args -> {
            seedAdmin(userRepository, passwordService);
            seedProducts(productRepository);
            seedStoreSettings(storeSettingsService);
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
                        "OG Slate Overshirt",
                        "Shirts",
                        "Structured overshirt with a refined drape for sharp everyday layering.",
                        1899.0,
                        "assets/images/products/og-shirt-1.svg",
                        25
                ),
                buildProduct(
                        "OG Ivory Street Shirt",
                        "Shirts",
                        "A clean statement shirt for elevated casual styling and festive wear.",
                        2099.0,
                        "assets/images/products/og-shirt-2.svg",
                        18
                ),
                buildProduct(
                        "OG Ember Tee",
                        "Tshirt",
                        "Soft premium tee with a bold urban palette and relaxed silhouette.",
                        999.0,
                        "assets/images/products/og-tshirt-1.svg",
                        40
                ),
                buildProduct(
                        "OG Midnight Tee",
                        "Tshirt",
                        "Dark everyday essential with a confident streetwear attitude.",
                        1099.0,
                        "assets/images/products/og-tshirt-2.svg",
                        32
                ),
                buildProduct(
                        "OG Sand Trousers",
                        "Pants",
                        "Versatile trousers built for polished day-to-evening looks.",
                        2299.0,
                        "assets/images/products/og-pant-1.svg",
                        20
                ),
                buildProduct(
                        "OG Steel Trousers",
                        "Pants",
                        "Tailored casual bottoms with a sharp silhouette and modern finish.",
                        2499.0,
                        "assets/images/products/og-pant-2.svg",
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

    private void seedStoreSettings(StoreSettingsService storeSettingsService) {
        storeSettingsService.getSettings();
    }
}
