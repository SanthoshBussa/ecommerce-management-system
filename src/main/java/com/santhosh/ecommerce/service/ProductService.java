package com.santhosh.ecommerce.service;

import com.santhosh.ecommerce.entity.Category;
import com.santhosh.ecommerce.entity.Product;
import com.santhosh.ecommerce.repository.CategoryRepository;
import com.santhosh.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Page<Product> getFilteredProducts(
            String category,
            String brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating,
            String keyword,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {
        Sort sort = "desc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.filterProducts(
                category, brand, minPrice, maxPrice, minRating, keyword, pageable
        );
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Product createProduct(Product product, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
        product.setCategory(category);
        return productRepository.save(product);
    }
}
