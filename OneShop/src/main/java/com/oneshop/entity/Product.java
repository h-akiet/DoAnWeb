package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


import java.util.Set;

import com.oneshop.enums.ProductStatus;
@Entity
@Table(name = "PRODUCTS")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 100)
    private String brand; // <-- BỔ SUNG: Thương hiệu sản phẩm

    @Column(name = "display_price", nullable = false)
    private double price; // Giờ đóng vai trò là giá hiển thị (có thể là giá thấp nhất)

    @Column(name = "original_price")
    private Double originalPrice; // Giá gốc để hiển thị chung

    @Lob // Dùng cho các chuỗi văn bản dài
    private String description;

    @Column(nullable = false)
    private int salesCount = 0;

    @Column(name = "is_published", nullable = false)
    private boolean published = false; // <-- BỔ SUNG: Trạng thái công khai/ẩn
    
    @Enumerated(EnumType.STRING)//Trạng thái duyệt đơn của admin
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.PENDING; // Mặc định là 'Chờ duyệt'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProductVariant> variants;

    // <-- BỔ SUNG: Hoàn thiện quan hệ với ProductImage
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ProductImage> images;
 // Dán đoạn code này vào bên trong class Product của bạn
 // File: com/oneshop/entity/Product.java

 public String getPrimaryImageUrl() {
     // Nếu sản phẩm không có ảnh nào, trả về một ảnh mặc định
     if (images == null || images.isEmpty()) {
         return "/assets/img/product/product1.png"; // Thay bằng đường dẫn ảnh mặc định của bạn
     }

     // Tìm trong danh sách ảnh, ảnh nào được đánh dấu là isPrimary = true
     return images.stream()
             .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
             .findFirst()
             // Nếu tìm thấy, lấy ra imageUrl của nó
             .map(ProductImage::getImageUrl)
             // Nếu không có ảnh nào là primary, thì lấy tạm ảnh đầu tiên trong danh sách
             .orElse(images.iterator().next().getImageUrl());
 }
}