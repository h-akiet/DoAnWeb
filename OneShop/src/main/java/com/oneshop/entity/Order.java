// src/main/java/com/oneshop/entity/Order.java
package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORDERS")
@Getter // Dùng @Getter @Setter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"orderDetails", "user", "shop", "shipper"}) // Thêm shop, shipper
@ToString(exclude = {"orderDetails", "user", "shop", "shipper"})      // Thêm shop, shipper
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Import com.oneshop.entity.User

    // --- Thông tin Người nhận (Lưu tại thời điểm đặt hàng) ---
    @Column(name = "recipient_name", nullable = false, columnDefinition = "nvarchar(255)")
    private String recipientName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    @Column(name = "shipping_address", nullable = false, columnDefinition = "nvarchar(500)")
    private String shippingAddress; // Giữ lại MỘT trường này

    // --- Thông tin Đơn hàng ---
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length=50)
    private OrderStatus orderStatus; // Import com.oneshop.entity.OrderStatus

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", precision = 19, scale = 2)
    private BigDecimal shippingCost;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    // --- Liên kết ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop; // Import com.oneshop.entity.Shop

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id") // nullable = true
    private User shipper; // Shipper cũng là User

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails = new ArrayList<>(); // Import com.oneshop.entity.OrderDetail

    // Bỏ các trường duplicate và getter/setter thủ công
    // @Column(...)
    // private String customerName; // Đã có recipientName
    // @Column(...)
    // private String customerEmail; // Lấy từ user.getEmail()
    // @Column(...)
    // private String customerPhone; // Đã có shippingPhone
    // @Column(...)
    // private String shippingAddress; // duplicate

    // @Column(...)
    // private OrderStatus status; // duplicate

    // Bỏ các getter/setter thủ công
    // public Long getOrderId() { return id; }
    // public void setOrderId(Long orderId) { this.id = orderId; }
}