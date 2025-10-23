package com.oneshop.entity.vendor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;

    @Column(length = 2000, columnDefinition = "nvarchar(2000)")
    private String description;

    @Column(nullable = false)
    private BigDecimal price; 
    
    @Column(columnDefinition = "numeric(19, 2)") 
    private BigDecimal salePrice;

    @Column(nullable = false)
    private Integer stock; // Số lượng tồn kho
    
    @Column(columnDefinition = "nvarchar(500)")
    private String tags; // Các từ khóa, cách nhau bằng dấu phẩy

    // Một Sản phẩm chỉ thuộc một Danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // Một Sản phẩm chỉ thuộc một Shop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // Quản lý hình ảnh sản phẩm (Cách đơn giản)
    // ElementCollection sẽ tạo ra một bảng riêng (Product_Images)
    // để lưu danh sách tên các file ảnh
    @ElementCollection
    @CollectionTable(name = "Product_Images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", columnDefinition = "nvarchar(255)") 
    private List<String> images;
}