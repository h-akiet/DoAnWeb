package com.oneshop.service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Product;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.ProductReviewRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    // This method is not used by the homepage but is kept for other features
    public List<Product> getTopSellingProducts() {
        return productRepository.findBySalesCountGreaterThanOrderBySalesCountDesc(10);
    }
    
    // This method is not used by the homepage but is kept for other features
    public List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice) {
        return productRepository.searchAndFilter(name, categoryId, minPrice, maxPrice);
    }

    public List<com.oneshop.entity.Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Finds the top 10 best-selling products and populates their details.
     */
    @Transactional(readOnly = true)
    public List<Product> findBestSellingProducts() {
        List<Product> products = productRepository.findTop10ByOrderBySalesCountDesc();
        // Use the helper method to add details like rating and review count
        setProductDetails(products); 
        return products;
    }

    /**
     * Finds the top 10 newest products and populates their details.
     */
    @Transactional(readOnly = true)
    public List<Product> findNewestProducts() {
        List<Product> products = productRepository.findTop10ByOrderByProductIdDesc();
        // Use the helper method to add details
        setProductDetails(products);
        return products;
    }

    /**
     * Finds the top 10 products with the best price (biggest discount)
     * and populates their details.
     */
    @Transactional(readOnly = true)
    public List<Product> findBestPriceProducts() {
        List<Product> products = productRepository.findTop10ByBestDiscount();
        // Use the helper method to add details
        setProductDetails(products);
        return products;
    }

    /**
     * A private helper method to populate transient fields (rating, reviewCount, etc.)
     * for a list of products. This avoids code duplication.
     * @param products The list of products to process.
     */
    private void setProductDetails(List<Product> products) {
        for (Product product : products) {
            Long productId = product.getProductId();
            
            // Get data from Review Repository
            Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
            Integer reviewCount = reviewRepository.countReviewsByProductId(productId);

            // Set the transient fields in the Product entity
            product.setRating(avgRating);
            product.setReviewCount(reviewCount);
            product.setSoldCount(product.getSalesCount());
        }
    }

    // This method is likely not needed for the homepage tabs, but I've left it here.
	@Transactional(readOnly = true)
	public List<Product> findFeaturedProducts() {
		// You can implement logic here later if needed
		return null;
	}
}