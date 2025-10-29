package com.oneshop.entity;

import jakarta.persistence.*;
// Bỏ import BigDecimal nếu không dùng nữa
// import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList; // Thêm import này
import java.util.List;     // Thêm import này

@Entity
@Table(name = "SHIPPING_COMPANIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shippingId;

    @Column(length = 150, nullable = false)
    private String name;

    // --- SỬA ---
    // Xóa trường fee không cần thiết
    // @Column(precision = 10, scale = 2, nullable = false)
    // private BigDecimal fee;

    // --- THÊM CÁC TRƯỜNG CÒN THIẾU ---
    @Column(length = 20) // Thêm độ dài cho phone
    private String phone; // Thêm trường phone

    @Column(nullable = false)
    private Boolean isActive = true; // Thêm trường trạng thái, mặc định là true

    // --- THÊM QUAN HỆ VỚI ShippingRule ---
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingRule> rules = new ArrayList<>(); // Thêm danh sách rules
    // ------------- KẾT THÚC THÊM -----------

}