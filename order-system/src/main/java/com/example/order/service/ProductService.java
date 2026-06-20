package com.example.order.service;

import com.example.order.dto.ProductRequest;
import com.example.order.dto.ProductResponse;
import com.example.order.entity.Product;
import com.example.order.repository.ProductRepository;
import org.springframework.stereotype.Service;

import com.example.order.common.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: id=" + id));
        return ProductResponse.fromEntity(product);
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @CacheEvict(value = "product", key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在: id=" + id));
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @CacheEvict(value = "product", key = "#id")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("商品不存在: id=" + id);
        }
        productRepository.deleteById(id);
    }
}
