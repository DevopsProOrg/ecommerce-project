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

        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        product.setPrice(request.getPrice());
        product.setImage(request.getImage() == null || request.getImage().isBlank()
                ? "assets/images/products/shirt1.jpg"
                : request.getImage().trim());
        product.setStock(request.getStock());
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
}
