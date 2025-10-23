package com.oneshop.service.vendor.impl;

import com.oneshop.dto.vendor.ProductDto;
import com.oneshop.entity.vendor.Category;
import com.oneshop.entity.vendor.Product;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.repository.vendor.CategoryRepository;
import com.oneshop.repository.vendor.ProductRepository;
import com.oneshop.repository.vendor.ShopRepository;
import com.oneshop.service.vendor.FileStorageService;
import com.oneshop.service.vendor.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import java.util.ArrayList;
import java.util.Collections; // Import Collections
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public Page<Product> getProductsByShop(Long shopId, Pageable pageable) {
        return productRepository.findByShopId(shopId, pageable);
    }

    @Override
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));
    }

    @Override
    @Transactional
    public Product addProduct(ProductDto productDto, List<MultipartFile> images, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy shop"));
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Validate giá sale (nếu có) phải nhỏ hơn giá gốc
        if (productDto.getSalePrice() != null && productDto.getPrice() != null && productDto.getSalePrice().compareTo(productDto.getPrice()) >= 0) {
            throw new IllegalArgumentException("Giá khuyến mãi phải nhỏ hơn giá bán gốc.");
        }

        List<String> imageFilenames = saveImages(images);

        Product product = new Product();
        mapDtoToEntity(productDto, product, category, shop, imageFilenames);

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Long productId, ProductDto productDto, List<MultipartFile> newImages) {
        Product existingProduct = getProductById(productId);
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Validate giá sale (nếu có) phải nhỏ hơn giá gốc
         if (productDto.getSalePrice() != null && productDto.getPrice() != null && productDto.getSalePrice().compareTo(productDto.getPrice()) >= 0) {
            throw new IllegalArgumentException("Giá khuyến mãi phải nhỏ hơn giá bán gốc.");
        }

        // Lưu ảnh mới (nếu có)
        List<String> newImageFilenames = saveImages(newImages);

        // Cập nhật thông tin từ DTO vào Entity
        mapDtoToEntity(productDto, existingProduct, category, existingProduct.getShop(), null); // Không ghi đè ảnh cũ ngay

        // Thêm ảnh mới vào danh sách hiện có (nếu có ảnh mới)
        if (!newImageFilenames.isEmpty()) {
            if (existingProduct.getImages() == null) {
                existingProduct.setImages(new ArrayList<>());
            }
            existingProduct.getImages().addAll(newImageFilenames);
        }

        return productRepository.save(existingProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long productId) {
        Product product = getProductById(productId);
        deleteProductImages(product); // Xóa ảnh trước
        productRepository.delete(product); // Xóa sản phẩm
    }

    @Override
    public long countProductsByShop(Long shopId) {
        return productRepository.countByShopId(shopId);
    }
    
    // --- Helper Methods ---

    // Hàm ánh xạ từ DTO sang Entity (tái sử dụng cho add và update)
    private void mapDtoToEntity(ProductDto dto, Product entity, Category category, Shop shop, List<String> initialImages) {
        entity.setName(dto.getProductName());
        entity.setDescription(dto.getProductDescription());
        entity.setPrice(dto.getPrice());
        entity.setSalePrice(dto.getSalePrice()); // Gán giá sale
        entity.setStock(dto.getStock());
        entity.setTags(dto.getProductTags());
        entity.setCategory(category);
        entity.setShop(shop);
        // Chỉ gán danh sách ảnh mới nếu là lúc add (initialImages != null)
        if (initialImages != null) {
             entity.setImages(initialImages);
        }
    }

    // Hàm lưu danh sách ảnh (tái sử dụng)
    private List<String> saveImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList(); // Trả về list rỗng nếu không có ảnh
        }
        return images.stream()
                .filter(file -> file != null && !file.isEmpty() && StringUtils.hasText(file.getOriginalFilename())) // Kiểm tra kỹ hơn
                .map(fileStorageService::save)
                .collect(Collectors.toList());
    }
    
    // Hàm xóa ảnh sản phẩm (tái sử dụng)
    private void deleteProductImages(Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            for (String filename : product.getImages()) {
                 if (StringUtils.hasText(filename)) { // Kiểm tra tên file trước khi xóa
                    fileStorageService.delete(filename);
                 }
            }
        }
    }
}