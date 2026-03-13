package com.shop.ecommerce.service;

import com.shop.ecommerce.model.Product;
import com.shop.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public List<Product> getAllProducts(){
        return repo.findAll();
    }

    public Product addProduct(Product product){
        return repo.save(product);
    }

    public void deleteProduct(Long id){
        repo.deleteById(id);
    }
}