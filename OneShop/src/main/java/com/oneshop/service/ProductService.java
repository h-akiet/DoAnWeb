package com.oneshop.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Brand;
import com.oneshop.entity.Product;
import com.oneshop.repository.BrandRepository;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest; // Thêm import này


@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    // --- Public Methods for Index/Home Page ---

    public List<com.oneshop.entity.Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findBestSellingProducts() {
        List<Product> products = productRepository.findTop10ByOrderBySalesCountDesc();
        setProductDetails(products); 
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> findNewestProducts() {
        List<Product> products = productRepository.findTop10ByOrderByProductIdDesc();
        setProductDetails(products);
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> findBestPriceProducts() {
        List<Product> products = productRepository.findTop10ByBestDiscount();
        setProductDetails(products);
        return products;
    }

    // --- Public Methods for Product Detail Page ---

    /**
     * Finds a product by its ID and populates its transient details.
     */
    @Transactional(readOnly = true)
    public Product findProductById(Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            setProductDetails(product); // Set details for single product
            return product;
        }
        return null; 
    }

    /**
     * Finds related products based on the current product's category.
     */
    @Transactional(readOnly = true)
    public List<Product> findRelatedProducts(Product product) {
        if (product == null || product.getCategory() == null) {
            return Collections.emptyList(); // Trả về danh sách rỗng nếu không có sản phẩm hoặc danh mục
        }

        // Tạo một đối tượng Pageable để chỉ lấy 6 sản phẩm đầu tiên
        Pageable limit = PageRequest.of(0, 6); 

        // Gọi phương thức repository mới, truyền vào cả category, productId và giới hạn số lượng
        List<Product> relatedProducts = productRepository.findByCategoryAndProductIdNot(
            product.getCategory(), 
            product.getProductId(),
            limit
        );
        
        // Làm giàu dữ liệu cho các sản phẩm liên quan (tính rating, sold count,...)
        setProductDetails(relatedProducts); 
        return relatedProducts;
    }


    // --- Helper Methods ---

    /**
     * Populates transient fields (rating, reviewCount, soldCount) for a list of products.
     */
    private void setProductDetails(List<Product> products) {
        if (products != null) {
            for (Product product : products) {
                setProductDetails(product); // Calls the single product helper
            }
        }
    }

    /**
     * Populates transient fields (rating, reviewCount, soldCount) for a single product.
     */
    private void setProductDetails(Product product) {
        if (product != null) {
            Long productId = product.getProductId();

            // Get data from Review Repository
            Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
            Integer reviewCount = reviewRepository.countReviewsByProductId(productId);

            // Set the transient fields in the Product entity
            product.setRating(avgRating != null ? avgRating : 0.0);
            product.setReviewCount(reviewCount != null ? reviewCount : 0);
            product.setSoldCount(product.getSalesCount()); 
        }
    }
    
    // --- Other Methods (Kept for completeness) ---
    
    public List<Product> getTopSellingProducts() {
        return productRepository.findBySalesCountGreaterThanOrderBySalesCountDesc(10);
    }
    
    public List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice) {
        return productRepository.searchAndFilter(name, categoryId, minPrice, maxPrice);
    }

	@Transactional(readOnly = true)
	public List<Product> findFeaturedProducts() {
		return null;
	}

	public List<com.oneshop.entity.Brand> getAllBrands() {
	    return brandRepository.findAll();
	}
	public Page<Product> findAll(Specification<Product> spec, Pageable pageable) {
	    // Gọi phương thức findAll() của JpaSpecificationExecutor
	    Page<Product> productPage = productRepository.findAll(spec, pageable);
	    
	    // Làm giàu dữ liệu (tính rating,...) cho các sản phẩm trong trang hiện tại
	    productPage.getContent().forEach(this::setProductDetails);
	    
	    return productPage;
	}
}