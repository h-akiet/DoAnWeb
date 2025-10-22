package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// import java.util.Set; // Không cần Set nữa

@Entity
@Table(name = "ORDERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@EqualsAndHashCode(exclude = {"orderDetails", "user", "shipper"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    private User shipper;

    // [SỬA 3] - Chuyển từ String sang Enum OrderStatus
    // Đây là trường cũ của bạn:
    // @Column(name = "order_status", length = 50, nullable = false)
    // private String orderStatus;
    
    // Đây là trường mới (đúng) để dùng với OrderService:
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus; // Sử dụng Enum đã tạo

    @Column(nullable = false)
    private BigDecimal total; // Trường này sẽ được dùng làm 'grandTotal'

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // [SỬA 4] - Chuyển từ Set<OrderItem> sang List<OrderDetail>
    // Đây là trường cũ của bạn:
    // @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private Set<OrderItem> items;
    
    // Đây là trường mới (đúng) để dùng với OrderService và OrderDetail.java:
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails = new ArrayList<>();


    // ==========================================================
    // === [SỬA 5] BỔ SUNG TẤT CẢ CÁC TRƯỜNG CÒN THIẾU SAU ===
    // ==========================================================

    @Column(name = "recipient_name", nullable = true)
    private String recipientName;

    @Column(name = "shipping_address", nullable = true)
    private String shippingAddress;
    
    @Column(name = "shipping_phone", nullable = true)
    private String shippingPhone;

    @Column(name = "payment_method", nullable = true)
    private String paymentMethod;

    @Column(name = "subtotal", nullable = true)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", nullable = true)
    private BigDecimal shippingCost;

    // ==========================================================
    // === HẾT PHẦN BỔ SUNG ===
    // ==========================================================


    // Getter cho order_id (tương thích với code cũ)
    public Long getOrderId() {
        return id;
    }

    // Setter cho order_id (tương thích với code cũ)
    public void setOrderId(Long orderId) {
        this.id = orderId;
    }
    
    // Bạn không cần thêm getters/setters cho các trường mới
    // vì @Getter và @Setter (Lombok) đã tự động làm việc đó.
}

