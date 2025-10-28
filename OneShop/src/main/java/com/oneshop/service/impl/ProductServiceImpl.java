// src/main/java/com/oneshop/service/impl/ProductServiceImpl.java
package com.oneshop.service.impl;

import com.oneshop.dto.ProductDto;
import com.oneshop.dto.VariantDto;
import com.oneshop.entity.*;
import com.oneshop.enums.ProductStatus;
import com.oneshop.repository.*;
import com.oneshop.service.BrandService;
import com.oneshop.service.CategoryService;
import com.oneshop.service.FileStorageService;
import com.oneshop.service.ProductReviewService;
import com.oneshop.service.ProductService;
import com.oneshop.service.ReviewService;
import com.oneshop.specification.ProductSpecification;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate; 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired private ProductRepository productRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired @Lazy private ProductReviewRepository reviewRepository; 
    @Autowired private FileStorageService fileStorageService;
    @Autowired private BrandService brandService;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductReviewService reviewService;
   
    
    @Autowired private CategoryService categoryService;

    // --- Vendor Methods ---

    @Override
    @Transactional(readOnly = true)
    public Page<Product> getProductsByShop(Long shopId, Pageable pageable) {
        logger.debug("Fetching products for shop ID: {} with pageable: {}", shopId, pageable);
        Page<Product> productPage = productRepository.findByShopId(shopId, pageable);
        productPage.getContent().forEach(p -> {
            Hibernate.initialize(p.getVariants()); 
            Hibernate.initialize(p.getImages());   
            setProductDetails(p); 
        });
        return productPage;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductByIdForVendor(Long productId, Long shopId) {
        logger.debug("Fetching product ID: {} for vendor shop ID: {}", productId, shopId);
        Optional<Product> productOpt = productRepository.findById(productId)
                .filter(product -> product.getShop() != null && product.getShop().getId().equals(shopId));
        productOpt.ifPresent(p -> {
             Hibernate.initialize(p.getImages());
             Hibernate.initialize(p.getVariants());
             setProductDetails(p);
        });
        return productOpt;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) 
    public Product addProduct(ProductDto productDto, List<MultipartFile> images, Long shopId) {
        logger.info("Adding new product '{}' for shop ID: {}", productDto.getProductName(), shopId);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new EntityNotFoundException("Shop not found with ID: " + shopId));
        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        Brand brand = resolveBrand(productDto.getBrandId(), productDto.getNewBrandName());

        validateVariants(productDto.getVariants());

        List<String> savedGeneralImageFilenames = new ArrayList<>();
        List<String> savedVariantImageFilenames = new ArrayList<>();
        Product product = new Product(); 

        try {
            savedGeneralImageFilenames.addAll(saveImages(images));
            setProductImages(product, savedGeneralImageFilenames); 

            mapDtoToEntity(productDto, product, category, shop, brand);
            product.setPublished(true); 

            mapAndSaveVariants(productDto.getVariants(), product, savedVariantImageFilenames);
            updateProductPriceAndStockFromVariants(product);

            Product savedProduct = productRepository.save(product);
            logger.info("Successfully added product ID: {}", savedProduct.getProductId());
            return savedProduct;

        } catch (Exception e) {
            logger.error("Error during addProduct process for '{}': {}", productDto.getProductName(), e.getMessage(), e);
            deleteImageFiles(savedGeneralImageFilenames);
            deleteImageFiles(savedVariantImageFilenames);
            if (e instanceof IOException) {
                 throw new RuntimeException("Lỗi khi lưu ảnh sản phẩm: " + e.getMessage(), e);
            } else if (e instanceof DataIntegrityViolationException) {
                 throw new RuntimeException("Lỗi dữ liệu khi lưu sản phẩm (có thể trùng lặp?).", e);
            }
            throw new RuntimeException("Lỗi không xác định khi thêm sản phẩm: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) 
    public Product updateProduct(Long productId, ProductDto productDto, List<MultipartFile> newImages, Long shopId) {
        logger.info("Updating product ID: {} for shop ID: {}", productId, shopId);
        Product existingProduct = productRepository.findById(productId)
                .filter(p -> p.getShop() != null && p.getShop().getId().equals(shopId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found or permission denied."));
        Hibernate.initialize(existingProduct.getVariants());
        Hibernate.initialize(existingProduct.getImages());

        Category category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found with ID: " + productDto.getCategoryId()));
        Brand brand = resolveBrand(productDto.getBrandId(), productDto.getNewBrandName());

        validateVariants(productDto.getVariants());

        List<String> savedNewGeneralImageFilenames = new ArrayList<>();
        List<String> savedNewVariantImageFilenames = new ArrayList<>();
        List<String> oldVariantImagesToDelete = new ArrayList<>(); 

        try {
            savedNewGeneralImageFilenames.addAll(saveImages(newImages));
            mapDtoToEntity(productDto, existingProduct, category, existingProduct.getShop(), brand);
            updateAndSaveVariants(productDto.getVariants(), existingProduct, savedNewVariantImageFilenames, oldVariantImagesToDelete);
            updateProductPriceAndStockFromVariants(existingProduct);

            if (!savedNewGeneralImageFilenames.isEmpty()) {
                setProductImages(existingProduct, savedNewGeneralImageFilenames);
            }

            Product updatedProduct = productRepository.save(existingProduct);
            deleteImageFiles(oldVariantImagesToDelete);
            
            logger.info("Successfully updated product ID: {}", updatedProduct.getProductId());
            return updatedProduct;

        } catch (Exception e) {
            logger.error("Error during updateProduct process for ID {}: {}", productId, e.getMessage(), e);
            deleteImageFiles(savedNewGeneralImageFilenames);
            deleteImageFiles(savedNewVariantImageFilenames);
             if (e instanceof IOException) {
                 throw new RuntimeException("Lỗi khi lưu ảnh sản phẩm cập nhật: " + e.getMessage(), e);
            } else if (e instanceof DataIntegrityViolationException) {
                 throw new RuntimeException("Lỗi dữ liệu khi cập nhật sản phẩm.", e);
            }
            throw new RuntimeException("Lỗi không xác định khi cập nhật sản phẩm: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional 
    public void deleteProduct(Long productId, Long shopId) {
        logger.warn("Attempting to delete product ID: {} for shop ID: {}", productId, shopId);
        Product product = productRepository.findById(productId)
                .filter(p -> p.getShop() != null && p.getShop().getId().equals(shopId))
                .orElseThrow(() -> new EntityNotFoundException("Product not found or permission denied."));

        Hibernate.initialize(product.getImages());
        Hibernate.initialize(product.getVariants());
        logger.debug("Successfully loaded images ({}) and variants ({}) for product {} before deletion attempt.",
            product.getImages() != null ? product.getImages().size() : 0,
            product.getVariants() != null ? product.getVariants().size() : 0,
            productId);

        List<String> imageFilenamesToDelete = new ArrayList<>();
        if(product.getImages() != null) {
            product.getImages().stream()
                   .map(ProductImage::getImageUrl)
                   .filter(StringUtils::hasText)
                   .forEach(imageFilenamesToDelete::add);
        }
        if(product.getVariants() != null) {
            product.getVariants().stream()
                   .map(ProductVariant::getImageUrl)
                   .filter(StringUtils::hasText)
                   .forEach(imageFilenamesToDelete::add);
        }
        logger.debug("Found {} image files to delete for product {}", imageFilenamesToDelete.size(), productId);

        try {
            productRepository.delete(product);
            logger.info("Successfully deleted product entity ID: {} from database.", productId);
            deleteImageFiles(imageFilenamesToDelete);
            logger.info("Attempted deletion of associated image files for product {}.", productId);
        } catch (DataIntegrityViolationException e) {
             logger.error("ConstraintViolationException deleting product ID {}: {}", productId, e.getMessage());
             throw new RuntimeException("Không thể xóa sản phẩm đã tồn tại trong đơn hàng.", e);
        } catch (Exception e) {
             logger.error("Unexpected error deleting product ID {}: {}", productId, e.getMessage(), e);
             throw new RuntimeException("Lỗi không xác định khi xóa sản phẩm: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public long countProductsByShop(Long shopId) {
        logger.debug("Counting products for shop ID: {}", shopId);
        return productRepository.countByShopId(shopId);
    }

    // --- User (Public) Methods ---

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        logger.debug("Fetching all categories sorted by name");
        return categoryRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        logger.debug("Fetching all brands via BrandService sorted by name");
        return brandService.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findBestSellingProducts(int limit) {
        logger.debug("Fetching {} best selling published products", limit);
        Pageable pageable = PageRequest.of(0, limit); 
        Page<Product> productPage = productRepository.findTopSellingPublished(pageable);
        List<Product> products = productPage.getContent();
        products.forEach(p -> {
            Hibernate.initialize(p.getImages());
            setProductDetails(p);
        });
        return products;
    }

    // ===>>> SỬA PHƯƠNG THỨC NÀY <<<===
    @Override
    @Transactional(readOnly = true)
    public Page<Product> findNewestProducts(Pageable pageable) {
        logger.debug("Fetching newest published products with pageable: {}", pageable);
        
        // Sửa: Gọi findByPublishedTrue thay vì findTopNewestPublished
        // findByPublishedTrue không có ORDER BY cứng, nó sẽ nhận sắp xếp từ Pageable
        Page<Product> productPage = productRepository.findByPublishedTrue(pageable); 
        
        // Vẫn phải gọi setProductDetails cho nội dung của trang hiện tại
        productPage.getContent().forEach(p -> {
            Hibernate.initialize(p.getImages());
            setProductDetails(p);
        });
        
        return productPage; // Trả về đối tượng Page
    }
    // ===>>> KẾT THÚC SỬA <<<===

    @Override
    @Transactional(readOnly = true)
    public List<Product> findBestPriceProducts(int limit) {
        logger.debug("Fetching {} best discount published products", limit);
        Pageable pageable = PageRequest.of(0, limit); 
        Page<Product> productPage = productRepository.findTopDiscountedPublished(pageable);
        List<Product> products = productPage.getContent();
        products.forEach(p -> {
            Hibernate.initialize(p.getImages());
            setProductDetails(p);
        });
        return products;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long productId) {
        logger.debug("Fetching published product by ID: {}", productId);
        Optional<Product> productOpt = productRepository.findByProductIdAndPublishedTrue(productId);

        productOpt.ifPresent(p -> {
            Hibernate.initialize(p.getImages());
            Hibernate.initialize(p.getVariants());
            logger.debug("Product {} - Initial images count (from p.getImages()): {}", productId, (p.getImages() != null ? p.getImages().size() : 0));

            Set<ProductImage> combinedImages = new HashSet<>();
            if (p.getImages() != null) {
                combinedImages.addAll(p.getImages());
            }
            logger.debug("Product {} - combinedImages count after adding general images: {}", productId, combinedImages.size());

            if (p.getVariants() != null) {
                p.getVariants().forEach(variant -> {
                    String variantImageUrl = variant.getImageUrl();
                    logger.trace("Product {} - Checking variant '{}' image URL: '{}'", productId, variant.getName(), variantImageUrl);
                    if (StringUtils.hasText(variantImageUrl)) {
                        ProductImage variantImgAsProductImage = new ProductImage();
                        variantImgAsProductImage.setProduct(p); 
                        variantImgAsProductImage.setImageUrl(variantImageUrl);
                        variantImgAsProductImage.setIsPrimary(false);
                        boolean added = combinedImages.add(variantImgAsProductImage);
                        if (added) {
                             logger.trace("Product {} - Added variant image '{}' to combinedImages. New size: {}", productId, variantImageUrl, combinedImages.size());
                        } else {
                             logger.trace("Product {} - Variant image '{}' likely already exists in combinedImages.", productId, variantImageUrl);
                        }
                    } else {
                         logger.trace("Product {} - Variant '{}' has no image URL.", productId, variant.getName());
                    }
                });
            }
            logger.debug("Product {} - Final combinedImages count before setting to product: {}", productId, combinedImages.size());
            p.setImages(combinedImages); 
            logger.debug("Product {} - Final p.getImages() count after setting: {}", productId, p.getImages().size());
            
            setProductDetails(p);
        });
        return productOpt;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findRelatedProducts(Product product, int limit) {
        if (product == null || product.getCategory() == null || product.getProductId() == null) {
            return Collections.emptyList();
        }
        Long categoryId = product.getCategory().getId();
        Long currentProductId = product.getProductId();
        logger.debug("Fetching {} related products for product ID: {} in category ID: {}", limit, currentProductId, categoryId);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("salesCount").descending());
        List<Product> relatedProducts = productRepository.findByCategory_IdAndProductIdNotAndPublishedTrue(
                categoryId, currentProductId, pageable);
        relatedProducts.forEach(p -> {
            Hibernate.initialize(p.getImages());
            setProductDetails(p);
        });
        return relatedProducts;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findAllPublishedProducts(Specification<Product> spec, Pageable pageable) {
        logger.debug("Finding all published products with spec and pageable: {}", pageable);
        Specification<Product> publishedSpec = Specification.where(spec)
                                                            .and(ProductSpecification.isPublished());
        Page<Product> productPage = productRepository.findAll(publishedSpec, pageable);
        productPage.getContent().forEach(p -> {
             Hibernate.initialize(p.getImages());
             setProductDetails(p);
        });
        return productPage;
    }
    
     @Override
     @Transactional(readOnly = true)
     public List<Product> searchAndFilterPublic(String name, Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice) {
          logger.debug("Searching public products with name: {}, categoryId: {}, brandId: {}, minPrice: {}, maxPrice: {}", name, categoryId, brandId, minPrice, maxPrice);
         Specification<Product> spec = Specification.where(ProductSpecification.isPublished());
         if (StringUtils.hasText(name)) { spec = spec.and(ProductSpecification.hasName(name)); }
         if (categoryId != null) { spec = spec.and(ProductSpecification.hasCategoryId(categoryId)); }
         if (brandId != null) { spec = spec.and(ProductSpecification.hasBrandId(brandId)); }
         if (minPrice != null || maxPrice != null) { spec = spec.and(ProductSpecification.priceBetween(minPrice, maxPrice)); }

         List<Product> products = productRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "productId"));
         products.forEach(p -> {
             Hibernate.initialize(p.getImages());
             setProductDetails(p);
         });
         return products;
     }

    // --- Helper Methods ---

    private void mapDtoToEntity(ProductDto dto, Product entity, Category category, Shop shop, Brand brand) {
        entity.setName(dto.getProductName());
        entity.setDescription(dto.getProductDescription());
        entity.setTags(dto.getProductTags());
        entity.setCategory(category);
        entity.setShop(shop);
        entity.setBrand(brand);
    }

    private Brand resolveBrand(Long brandId, String newBrandName) {
         Brand brand = null;
        if (brandId != null) {
            brand = brandService.findById(brandId)
                    .orElseThrow(() -> new EntityNotFoundException("Brand not found with ID: " + brandId));
        } else if (StringUtils.hasText(newBrandName)) {
            String trimmedBrandName = newBrandName.trim();
            Optional<Brand> existingBrandOpt = brandService.findByNameIgnoreCase(trimmedBrandName);
            if (existingBrandOpt.isPresent()) {
                brand = existingBrandOpt.get();
            } else {
                Brand newBrand = new Brand();
                newBrand.setName(trimmedBrandName);
                try {
                    brand = brandService.saveBrand(newBrand);
                } catch (Exception e) {
                    throw new RuntimeException("Error creating new brand: " + e.getMessage(), e);
                }
            }
        }
        return brand; 
    }

    private void validateVariants(List<VariantDto> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new IllegalArgumentException("Product must have at least one variant.");
        }
        Set<String> variantNames = new HashSet<>();
        for (VariantDto dto : variants) {
            if (!StringUtils.hasText(dto.getName())) {
                 throw new IllegalArgumentException("Variant name cannot be empty.");
            }
            if (!variantNames.add(dto.getName().trim().toLowerCase())) {
                 throw new IllegalArgumentException("Duplicate variant name found: " + dto.getName());
            }
            if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                 throw new IllegalArgumentException("Price for variant '" + dto.getName() + "' is invalid.");
            }
            if (dto.getOriginalPrice() != null && dto.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
                 throw new IllegalArgumentException("Original price for variant '" + dto.getName() + "' is invalid.");
            }
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("Stock for variant '" + dto.getName() + "' is invalid.");
            }
        }
    }

    private void mapAndSaveVariants(List<VariantDto> variantDtos, Product product, List<String> savedVariantImageFilenames) throws IOException {
         if (product.getVariants() == null) product.setVariants(new HashSet<>());
         product.getVariants().clear();

        for (VariantDto dto : variantDtos) {
            ProductVariant variant = new ProductVariant();
            mapVariantDtoToEntity(dto, variant);

            MultipartFile imageFile = dto.getVariantImageFile();
            if (imageFile != null && !imageFile.isEmpty()) {
                try {
                    String filename = fileStorageService.save(imageFile);
                    variant.setImageUrl(filename);
                    savedVariantImageFilenames.add(filename);
                } catch (Exception e) {
                    throw new IOException("Error saving image for variant '" + dto.getName() + "'", e);
                }
            }
            variant.setProduct(product);
            product.getVariants().add(variant);
        }
    }

    private void updateAndSaveVariants(List<VariantDto> variantDtos, Product existingProduct, List<String> savedNewVariantImageFilenames, List<String> oldVariantImagesToDelete) throws IOException {
        Map<Long, ProductVariant> existingVariantsMap = existingProduct.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));
        Set<ProductVariant> finalVariants = new HashSet<>();
        List<Long> dtoVariantIds = variantDtos.stream()
                                            .map(VariantDto::getVariantId)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());

        for (VariantDto dto : variantDtos) {
            ProductVariant variant;
            String oldImageUrl = null;

            if (dto.getVariantId() != null && existingVariantsMap.containsKey(dto.getVariantId())) {
                variant = existingVariantsMap.get(dto.getVariantId());
                oldImageUrl = variant.getImageUrl();
                mapVariantDtoToEntity(dto, variant);
            } else {
                variant = new ProductVariant();
                mapVariantDtoToEntity(dto, variant);
                variant.setProduct(existingProduct);
            }

            MultipartFile imageFile = dto.getVariantImageFile();
            if (imageFile != null && !imageFile.isEmpty()) {
                 try {
                    String filename = fileStorageService.save(imageFile);
                    variant.setImageUrl(filename);
                    savedNewVariantImageFilenames.add(filename);
                    if (StringUtils.hasText(oldImageUrl)) {
                        oldVariantImagesToDelete.add(oldImageUrl);
                    }
                } catch (Exception e) {
                    throw new IOException("Error saving image for variant '" + dto.getName() + "'", e);
                }
            } else {
                if (StringUtils.hasText(dto.getExistingImageUrl())) {
                    variant.setImageUrl(dto.getExistingImageUrl());
                } else {
                    variant.setImageUrl(null);
                    if (StringUtils.hasText(oldImageUrl)) {
                        oldVariantImagesToDelete.add(oldImageUrl);
                    }
                }
            }
            finalVariants.add(variant);
        }

        existingProduct.getVariants().stream()
            .filter(v -> !dtoVariantIds.contains(v.getVariantId()))
            .forEach(v -> {
                if (StringUtils.hasText(v.getImageUrl())) {
                    oldVariantImagesToDelete.add(v.getImageUrl());
                }
            });

        existingProduct.getVariants().clear();
        existingProduct.getVariants().addAll(finalVariants);
    }

    private void mapVariantDtoToEntity(VariantDto dto, ProductVariant entity) {
        entity.setName(dto.getName());
        entity.setSku(dto.getSku());
        entity.setPrice(dto.getPrice());
        entity.setOriginalPrice(dto.getOriginalPrice());
        entity.setStock(dto.getStock());
        entity.setActive(true);
    }

    private void updateProductPriceAndStockFromVariants(Product product) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            product.setPrice(BigDecimal.ZERO);
            product.setOriginalPrice(null);
            product.setStock(0);
            return;
        }

       BigDecimal minPrice = product.getVariants().stream()
               .map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
       BigDecimal correspondingOriginalPrice = product.getVariants().stream()
               .filter(v -> v.getPrice().compareTo(minPrice) == 0 && v.getOriginalPrice() != null)
               .map(ProductVariant::getOriginalPrice).max(BigDecimal::compareTo).orElse(null);
       
       // Tính tổng tồn kho
       int totalStock = product.getVariants().stream()
                           .mapToInt(ProductVariant::getStock) // Lấy kho của từng variant
                           .sum(); // Cộng tất cả lại

       product.setPrice(minPrice);
       product.setOriginalPrice(correspondingOriginalPrice);
       product.setStock(totalStock); // Gán tổng kho mới
   }

    private List<String> saveImages(List<MultipartFile> images) {
         if (images == null) return Collections.emptyList();
        List<String> savedFilenames = new ArrayList<>();
        for(MultipartFile file : images) {
            if (file != null && !file.isEmpty()) {
                try { savedFilenames.add(fileStorageService.save(file)); }
                catch (Exception e) { logger.error("Error saving image {}: {}", file.getOriginalFilename(), e.getMessage()); }
            }
        }
        return savedFilenames;
    }

     private void deleteImageFiles(List<String> filenames) {
         if (filenames != null) {
            filenames.forEach(filename -> {
                if (StringUtils.hasText(filename)) {
                    try { fileStorageService.delete(filename); }
                    catch (Exception e) { logger.error("Error deleting image file {}: {}", filename, e.getMessage()); }
                }
            });
        }
    }

    private void setProductImages(Product product, List<String> imageFilenames) {
        if (imageFilenames != null && !imageFilenames.isEmpty()) {
            if (product.getImages() == null) product.setImages(new HashSet<>());
            boolean needsPrimary = product.getImages().stream().noneMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
            for (int i = 0; i < imageFilenames.size(); i++) {
                ProductImage img = new ProductImage();
                img.setProduct(product);
                img.setImageUrl(imageFilenames.get(i));
                img.setIsPrimary(needsPrimary && i == 0);
                product.getImages().add(img);
            }
        }
    }

    private void setProductDetails(List<Product> products) { if (products != null) products.forEach(this::setProductDetails); }
    private void setProductDetails(Product product) {
         if (product != null && product.getProductId() != null) {
            try {
                Double avgRating = reviewRepository != null ? reviewRepository.findAverageRatingByProductId(product.getProductId()) : 0.0;
                Integer reviewCount = reviewRepository != null ? reviewRepository.countReviewsByProductId(product.getProductId()) : 0;
                product.setRating(avgRating != null ? avgRating : 0.0);
                product.setReviewCount(reviewCount != null ? reviewCount : 0);
                product.setSoldCount(product.getSalesCount());
            } catch (Exception e) {
                 logger.warn("Could not set details for product {}: {}", product.getProductId(), e.getMessage());
                 product.setRating(0.0);
                 product.setReviewCount(0);
                 product.setSoldCount(product.getSalesCount());
            }
        } else if (product != null) {
             product.setRating(0.0);
             product.setReviewCount(0);
             product.setSoldCount(0);
        }
    }

    // ===>>> THÊM PHƯƠNG THỨC NÀY TỪ INTERFACE <<<===
    @Override
    @Transactional // Quan trọng: Cần Transactional để lưu thay đổi
    public void updateProductStockAndPriceFromVariants(Product product) {
        if (product == null || product.getProductId() == null) {
            logger.warn("Attempted to update stock for a null or unsaved product.");
            return;
        }

        // Tải lại entity trong phiên làm việc (managed state) để tránh lỗi
        Product managedProduct = productRepository.findById(product.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm để cập nhật kho: " + product.getProductId()));

        // Phải load các variants để tính toán
        Hibernate.initialize(managedProduct.getVariants());

        // Gọi hàm helper (hàm private đã có sẵn trong file của bạn) để tính toán
        updateProductPriceAndStockFromVariants(managedProduct);

        // Lưu lại Product cha với tổng kho và giá mới
        productRepository.save(managedProduct);
        logger.debug("Updated aggregate stock/price for product ID: {}", managedProduct.getProductId());
    }
    @Override
	@Transactional(readOnly = true)
	public List<Product> findFilteredProducts(Long shopId, String productCode, ProductStatus status, Long categoryId,
			String brand) {
        logger.debug("Fetching filtered products for Admin/Vendor. ShopId: {}, Code: {}, Status: {}, CategoryId: {}, Brand: {}", shopId, productCode, status, categoryId, brand);
		String codeParam = (productCode != null && !productCode.isEmpty()) ? productCode : null;
		String brandParam = (brand != null && !brand.isEmpty()) ? brand : null;
		return productRepository.findFilteredProducts(shopId, codeParam, status, categoryId, brandParam);
	}

    @Override
	@Transactional(readOnly = true)
	public Set<String> findAllUniqueBrands() {
        logger.debug("Fetching all unique brands.");
		return productRepository.findAllUniqueBrands();
	}

    @Override
	@Transactional
	public void updateStatus(Long productId, ProductStatus newStatus) {
        logger.info("Updating status for product ID: {} to {}", productId, newStatus);
		productRepository.findById(productId).ifPresent(product -> {
			product.setStatus(newStatus);
            logger.debug("Successfully set status for product ID: {}", productId);
		});
	}

    @Override
	@Transactional
	public void updateAdminFields(Product productDetails) {
        logger.info("Updating Admin fields for product ID: {}", productDetails.getProductId());
		productRepository.findById(productDetails.getProductId()).ifPresent(product -> {
			product.setName(productDetails.getName());
			product.setDescription(productDetails.getDescription());
			if (productDetails.getCategory() != null && productDetails.getCategory().getId() != null) {
				Long newCategoryId = productDetails.getCategory().getId();
                // Sử dụng categoryService đã được inject ở trên
				categoryService.findById(newCategoryId).ifPresent(product::setCategory);
                logger.debug("Successfully updated name, description, and category for product ID: {}", product.getProductId());
			}
		});
	}

    @Override
	@Transactional
	public void deleteById(Long id) {
        logger.warn("Admin deleting product by ID: {}", id);
		productRepository.deleteById(id);
	}

    @Override
	@Transactional
	public int updateCategoryForProducts(Long oldCategoryId, Long newCategoryId) {
        logger.info("Updating category from {} to {} for products.", oldCategoryId, newCategoryId);
		return productRepository.updateCategoryByCategoryId(oldCategoryId, newCategoryId);
	}
    @Override
    @Transactional(readOnly = true) // Thêm readOnly vì chỉ đếm
    public long countProductsByCategory(Long categoryId) {
        logger.debug("Counting products for category ID: {}", categoryId);
        if (categoryId == null) {
            return 0; // Hoặc ném lỗi nếu categoryId không bao giờ được null
        }
        // Gọi phương thức countByCategoryId từ ProductRepository
        return productRepository.countByCategoryId(categoryId); // <-- THÊM TRIỂN KHAI NÀY
    }
}