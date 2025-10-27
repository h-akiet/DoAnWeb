package com.oneshop.service;

import java.util.List;
import java.util.Set;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Product;
import com.oneshop.enums.ProductStatus;
import com.oneshop.repository.CategoryRepository;
import com.oneshop.repository.ProductRepository;

@Service
public class ProductService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	public List<Product> getTopSellingProducts() {
		return productRepository.findBySalesCountGreaterThanOrderBySalesCountDesc(10);
	}

	public List<Product> searchAndFilter(String name, Long categoryId, Double minPrice, Double maxPrice) {
		return productRepository.searchAndFilter(name, categoryId, minPrice, maxPrice);
	}

	public List<com.oneshop.entity.Category> getAllCategories() {
		return categoryRepository.findAll();
	}

	@Transactional(readOnly = true)
	public List<Product> findBestSellingProducts() {
		return productRepository.findTop6ByOrderBySalesCountDesc();
	}

	public List<Product> findNewestProducts() {
		return productRepository.findTop6ByOrderByProductIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Product> findFeaturedProducts() {
		// TODO Auto-generated method stub
		return null;
	}

	// ADMIN
	private final CategoryService categoryService;

	@Autowired
	public ProductService(ProductRepository productRepository, CategoryService categoryService) {
		this.productRepository = productRepository;
		this.categoryService = categoryService;
	}

	@Transactional(readOnly = true)
	public List<Product> findFilteredProducts(Long shopId, String productCode, ProductStatus status, Long categoryId,
			String brand) {
		String codeParam = (productCode != null && !productCode.isEmpty()) ? productCode : null;
		String brandParam = (brand != null && !brand.isEmpty()) ? brand : null;
		return productRepository.findFilteredProducts(shopId, codeParam, status, categoryId, brandParam);
	}

	@Transactional(readOnly = true)
	public Set<String> findAllUniqueBrands() {
		return productRepository.findAllUniqueBrands();
	}

//Xử lý việc duyệt/từ chối (cập nhật trạng thái).

	@Transactional
	public void updateStatus(Long productId, ProductStatus newStatus) {
		productRepository.findById(productId).ifPresent(product -> {
			product.setStatus(newStatus);
		});
	}

//Xử lý chỉnh sửa thông tin Admin được phép thay đổi.
	@Transactional
	public void updateAdminFields(Product productDetails) {
		productRepository.findById(productDetails.getProductId()).ifPresent(product -> {
			product.setName(productDetails.getName());
			product.setDescription(productDetails.getDescription());
			if (productDetails.getCategory() != null && productDetails.getCategory().getCategoryId() != null) {
				Long newCategoryId = productDetails.getCategory().getCategoryId();
				categoryService.findById(newCategoryId).ifPresent(product::setCategory);
			}
		});
	}

// Xử lý xóa sản phẩm.
	@Transactional
	public void deleteById(Long id) {
		productRepository.deleteById(id);
	}

	@Transactional
	public int updateCategoryForProducts(Long oldCategoryId, Long newCategoryId) {
		return productRepository.updateCategoryByCategoryId(oldCategoryId, newCategoryId);
	}
}