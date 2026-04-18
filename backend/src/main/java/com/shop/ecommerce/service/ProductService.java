package com.shop.ecommerce.service;

import com.shop.ecommerce.dto.ProductRequest;
import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.model.User;
import com.shop.ecommerce.repository.ProductRepository;
import com.shop.ecommerce.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;
    private final UserRepository userRepository;
    private static final String DEFAULT_IMAGE = "assets/images/products/shirt1.jpg";

    public ProductService(ProductRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public List<Product> getAllProducts(){
        return repo.findAll();
    }

    public Product getProduct(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product addProduct(ProductRequest request){
        validateAdmin(request.getAdminUserId());

        Product product = new Product();
        applyProductFields(product, request);
        return repo.save(product);
    }

    public Product updateProduct(Long id, ProductRequest request) {
        validateAdmin(request.getAdminUserId());

        Product product = getProduct(id);
        applyProductFields(product, request);
        return repo.save(product);
    }

    public void deleteProduct(Long id, Long adminUserId){
        validateAdmin(adminUserId);

        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        repo.deleteById(id);
    }

    private void applyProductFields(Product product, ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is required");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }
        if (request.getPrice() == null || request.getPrice() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than 0");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }
        if (request.getOriginalPrice() != null && request.getOriginalPrice() < request.getPrice()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original price must be greater than or equal to price");
        }
        if (request.getRating() != null && (request.getRating() < 0 || request.getRating() > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 0 and 5");
        }
        if (request.getReviewCount() != null && request.getReviewCount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review count cannot be negative");
        }

        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setSubcategory(normalizeOptional(request.getSubcategory()));
        product.setBrand(normalizeOptional(request.getBrand(), "OG Fashions"));
        product.setSku(normalizeOptional(request.getSku(), buildSku(request)));
        product.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        product.setHighlights(normalizeOptional(request.getHighlights()));
        product.setBadge(normalizeOptional(request.getBadge()));
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice() == null ? request.getPrice() : request.getOriginalPrice());
        product.setImage(request.getImage() == null || request.getImage().isBlank()
                ? DEFAULT_IMAGE
                : request.getImage().trim());
        product.setStock(request.getStock());
        product.setColor(normalizeOptional(request.getColor()));
        product.setMaterial(normalizeOptional(request.getMaterial()));
        product.setSizeInfo(normalizeOptional(request.getSizeInfo()));
        product.setDeliveryInfo(normalizeOptional(request.getDeliveryInfo(), "Ships across India in 3-5 business days."));
        product.setRating(request.getRating() == null ? 4.4 : request.getRating());
        product.setReviewCount(request.getReviewCount() == null ? 0 : request.getReviewCount());
        product.setFeatured(Boolean.TRUE.equals(request.getFeatured()));
    }

    private void validateAdmin(Long adminUserId) {
        if (adminUserId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin user is required");
        }

        User user = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can manage products");
        }
    }

    private String normalizeOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeOptional(String value, String fallback) {
        String normalized = normalizeOptional(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String buildSku(ProductRequest request) {
        String categoryCode = request.getCategory().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String nameCode = request.getName().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String compactName = nameCode.length() > 6 ? nameCode.substring(0, 6) : nameCode;
        return "OG-" + categoryCode + "-" + compactName;
    }
}
