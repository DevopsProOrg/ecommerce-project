package com.shop.ecommerce.config;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import com.shop.ecommerce.service.PasswordService;
import com.shop.ecommerce.service.StoreSettingsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
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
        if (bootstrapAdminPassword.length() < 12 || bootstrapAdminPassword.length() > 128) {
            throw new IllegalStateException("Bootstrap admin password must be between 12 and 128 characters");
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
            backfillProducts(productRepository);
            return;
        }

        productRepository.saveAll(List.of(
                buildProduct(
                        "OG Slate Overshirt",
                        "Fashion",
                        "Overshirts",
                        "OG Fashions",
                        "OG-FASH-OVERSHIRT-001",
                        "Structured overshirt with a refined drape for sharp everyday layering.",
                        "Layer-ready silhouette,Premium cotton blend,Easy day-to-night styling",
                        "New Arrival",
                        1899.0,
                        2499.0,
                        "assets/images/products/shirt1.jpg",
                        25,
                        "Slate Grey",
                        "Cotton twill blend",
                        "S / M / L / XL",
                        "Free delivery in Hyderabad and fast dispatch nationwide.",
                        4.6,
                        84,
                        true
                ),
                buildProduct(
                        "OG Ivory Street Shirt",
                        "Fashion",
                        "Shirts",
                        "OG Fashions",
                        "OG-FASH-SHIRT-002",
                        "A clean statement shirt for elevated casual styling and festive wear.",
                        "Soft handfeel,Smart festive finish,Regular comfort fit",
                        "Best Seller",
                        2099.0,
                        2799.0,
                        "assets/images/products/shirt2.jpg",
                        18,
                        "Ivory",
                        "Premium poplin",
                        "M / L / XL",
                        "Ready to ship within 24 hours.",
                        4.7,
                        126,
                        true
                ),
                buildProduct(
                        "OG Ember Tee",
                        "Fashion",
                        "T-Shirts",
                        "OG Fashions",
                        "OG-FASH-TEE-003",
                        "Soft premium tee with a bold urban palette and relaxed silhouette.",
                        "Breathable fabric,Street-inspired tone,Everyday comfort",
                        "Trending",
                        999.0,
                        1399.0,
                        "assets/images/products/tshirt1.jpg",
                        40,
                        "Rust Orange",
                        "Combed cotton",
                        "S / M / L / XL / XXL",
                        "Cash on delivery available in select pin codes.",
                        4.5,
                        210,
                        true
                ),
                buildProduct(
                        "OG Heritage Watch",
                        "Accessories",
                        "Watches",
                        "OG Fashions",
                        "OG-ACC-WATCH-004",
                        "A modern analog watch that sharpens both office and occasion looks.",
                        "Minimal dial,Comfort strap,Gift-ready packaging",
                        "Hot Pick",
                        2499.0,
                        3299.0,
                        "assets/images/products/og-marketplace-watch-1.svg",
                        22,
                        "Gunmetal Black",
                        "Alloy case with leather strap",
                        "One size",
                        "Insured shipping and easy returns within 7 days.",
                        4.7,
                        162,
                        true
                ),
                buildProduct(
                        "OG Urban Phone Pouch",
                        "Accessories",
                        "Mobile Pouches",
                        "OG Fashions",
                        "OG-ACC-POUCH-005",
                        "Crossbody phone pouch designed for quick-access city travel and casual outings.",
                        "Hands-free carry,Front zip pocket,Adjustable strap",
                        "New Drop",
                        899.0,
                        1299.0,
                        "assets/images/products/og-marketplace-pouch-1.svg",
                        50,
                        "Sand Beige",
                        "Textured vegan leather",
                        "Fits most smartphones",
                        "Delivered in 3-5 business days.",
                        4.4,
                        73,
                        true
                ),
                buildProduct(
                        "OG Voyager Sling Bag",
                        "Accessories",
                        "Bags",
                        "OG Fashions",
                        "OG-ACC-BAG-006",
                        "Compact sling bag with enough room for essentials, chargers, and daily accessories.",
                        "Organized compartments,Travel-friendly build,Smart unisex styling",
                        "Limited Stock",
                        1599.0,
                        2199.0,
                        "assets/images/products/og-marketplace-bag-1.svg",
                        14,
                        "Midnight Blue",
                        "Water-resistant canvas",
                        "7L capacity",
                        "Priority dispatch from Hyderabad store.",
                        4.6,
                        58,
                        false
                ),
                buildProduct(
                        "OG Smart Strap Watch",
                        "Accessories",
                        "Watches",
                        "OG Fashions",
                        "OG-ACC-WATCH-007",
                        "Bold sport-inspired watch with an active strap and versatile styling.",
                        "Statement bezel,Flexible fit,Daily wear durability",
                        "Trending",
                        1099.0,
                        1699.0,
                        "assets/images/products/og-marketplace-watch-2.svg",
                        32,
                        "Olive Black",
                        "Silicone strap with alloy frame",
                        "One size",
                        "Fast delivery and live order support on WhatsApp.",
                        4.3,
                        41,
                        false
                ),
                buildProduct(
                        "OG Leather Card Wallet",
                        "Accessories",
                        "Wallets",
                        "OG Fashions",
                        "OG-ACC-WALLET-008",
                        "Slim card wallet that keeps essentials organized without adding bulk.",
                        "Compact profile,Giftable design,Secure card slots",
                        "Everyday Pick",
                        699.0,
                        999.0,
                        "assets/images/products/og-marketplace-wallet-1.svg",
                        60,
                        "Classic Tan",
                        "Faux leather",
                        "Bi-fold slim wallet",
                        "Ships across India with order tracking.",
                        4.5,
                        94,
                        false
                ),
                buildProduct(
                        "OG Sand Trousers",
                        "Fashion",
                        "Trousers",
                        "OG Fashions",
                        "OG-FASH-TROUSER-009",
                        "Versatile trousers built for polished day-to-evening looks.",
                        "Tailored ankle finish,Comfort waist,Occasion-ready styling",
                        "Staff Favorite",
                        2299.0,
                        2899.0,
                        "assets/images/products/pant1.jpg",
                        20,
                        "Sand",
                        "Structured stretch fabric",
                        "30 / 32 / 34 / 36",
                        "Store pickup available in Hyderabad.",
                        4.6,
                        77,
                        false
                ),
                buildProduct(
                        "OG Midnight Travel Organizer",
                        "Accessories",
                        "Travel Essentials",
                        "OG Fashions",
                        "OG-ACC-TRAVEL-010",
                        "Multi-pocket organizer for chargers, earbuds, cards, and daily carry gear.",
                        "Neat cable storage,Zip-around access,Travel-friendly format",
                        "Smart Add-On",
                        1799.0,
                        2499.0,
                        "assets/images/products/og-marketplace-organizer-1.svg",
                        16,
                        "Midnight Black",
                        "Durable nylon shell",
                        "Compact organizer",
                        "Packaged safely for gifting and travel use.",
                        4.4,
                        33,
                        false
                )
        ));
    }

    private Product buildProduct(String name,
                                 String category,
                                 String subcategory,
                                 String brand,
                                 String sku,
                                 String description,
                                 String highlights,
                                 String badge,
                                 Double price,
                                 Double originalPrice,
                                 String image,
                                 Integer stock,
                                 String color,
                                 String material,
                                 String sizeInfo,
                                 String deliveryInfo,
                                 Double rating,
                                 Integer reviewCount,
                                 Boolean featured) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setBrand(brand);
        product.setSku(sku);
        product.setDescription(description);
        product.setHighlights(highlights);
        product.setBadge(badge);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setImage(image);
        product.setStock(stock);
        product.setColor(color);
        product.setMaterial(material);
        product.setSizeInfo(sizeInfo);
        product.setDeliveryInfo(deliveryInfo);
        product.setRating(rating);
        product.setReviewCount(reviewCount);
        product.setFeatured(featured);
        return product;
    }

    private void seedStoreSettings(StoreSettingsService storeSettingsService) {
        storeSettingsService.getSettings();
    }

    private void backfillProducts(ProductRepository productRepository) {
        List<Product> products = new ArrayList<>(productRepository.findAll());
        boolean changed = false;

        for (Product product : products) {
            if (isBlank(product.getBrand())) {
                product.setBrand("OG Fashions");
                changed = true;
            }
            if (isBlank(product.getSubcategory())) {
                product.setSubcategory(product.getCategory());
                changed = true;
            }
            if (isBlank(product.getSku())) {
                product.setSku("OG-" + product.getId());
                changed = true;
            }
            if (product.getOriginalPrice() == null || product.getOriginalPrice() < product.getPrice()) {
                product.setOriginalPrice(product.getPrice());
                changed = true;
            }
            if (isBlank(product.getHighlights())) {
                product.setHighlights("Quality pick,Admin-manageable catalog item,Ready for OG marketplace");
                changed = true;
            }
            if (product.getRating() == null) {
                product.setRating(4.3);
                changed = true;
            }
            if (product.getReviewCount() == null) {
                product.setReviewCount(24);
                changed = true;
            }
            if (product.getFeatured() == null) {
                product.setFeatured(false);
                changed = true;
            }
            if (isBlank(product.getDeliveryInfo())) {
                product.setDeliveryInfo("Ships across India in 3-5 business days.");
                changed = true;
            }
            if (product.getImage() == null || product.getImage().isBlank()) {
                product.setImage(defaultImageFor(product));
                changed = true;
            } else {
                String expectedImage = defaultImageFor(product);
                if (!expectedImage.equals(product.getImage()) && shouldRefreshImage(product)) {
                    product.setImage(expectedImage);
                    changed = true;
                }
            }
        }

        if (changed) {
            productRepository.saveAll(products);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean shouldRefreshImage(Product product) {
        return product.getImage().contains("og-marketplace-style")
                || product.getImage().contains("og-shirt")
                || product.getImage().contains("og-tshirt")
                || product.getImage().contains("og-pant");
    }

    private String defaultImageFor(Product product) {
        String subcategory = product.getSubcategory() == null ? "" : product.getSubcategory().toLowerCase();
        String category = product.getCategory() == null ? "" : product.getCategory().toLowerCase();
        String name = product.getName() == null ? "" : product.getName().toLowerCase();

        if (subcategory.contains("watch")) {
            return "assets/images/products/og-marketplace-watch-1.svg";
        }
        if (subcategory.contains("pouch")) {
            return "assets/images/products/og-marketplace-pouch-1.svg";
        }
        if (subcategory.contains("bag")) {
            return "assets/images/products/og-marketplace-bag-1.svg";
        }
        if (subcategory.contains("wallet")) {
            return "assets/images/products/og-marketplace-wallet-1.svg";
        }
        if (subcategory.contains("travel")) {
            return "assets/images/products/og-marketplace-organizer-1.svg";
        }
        if (subcategory.contains("pant") || subcategory.contains("trouser")) {
            return "assets/images/products/pant1.jpg";
        }
        if (subcategory.contains("shirt") && !subcategory.contains("t-shirt") && !subcategory.contains("tshirt")) {
            return name.contains("ivory") ? "assets/images/products/shirt2.jpg" : "assets/images/products/shirt1.jpg";
        }
        if (subcategory.contains("t-shirt") || subcategory.contains("tshirt")) {
            return "assets/images/products/tshirt1.jpg";
        }
        if (category.contains("fashion")) {
            return "assets/images/products/shirt1.jpg";
        }
        return "assets/images/products/shirt1.jpg";
    }
}
