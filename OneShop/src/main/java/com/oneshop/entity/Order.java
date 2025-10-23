package com.oneshop.entity.vendor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Orders") // Đặt tên bảng là "Orders" để tránh xung đột với từ khóa SQL "ORDER"
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Thông tin khách hàng (Lưu trực tiếp thay vì liên kết,
    // vì thông tin giao hàng có thể khác với thông tin tài khoản)
    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String customerName;
    @Column(nullable = false)
    private String customerEmail;
    @Column(nullable = false)
    private String customerPhone;
    @Column(nullable = false, columnDefinition = "nvarchar(500)")
    private String shippingAddress;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Một đơn hàng thuộc về một Shop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;
    
    // Một đơn hàng có nhiều OrderItem (chi tiết sản phẩm)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;
}