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
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"orderDetails", "user", "shop", "shipper", "promotion"}) // Thêm promotion
@ToString(exclude = {"orderDetails", "user", "shop", "shipper", "promotion"})      // Thêm promotion
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
    private String shippingAddress; 

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
    
    // === BẮT ĐẦU THAY ĐỔI: Thêm thông tin Voucher/Promotion ===

    /**
     * Số tiền thực tế đã được giảm giá (lưu lại để đảm bảo lịch sử).
     */
    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Liên kết đến chương trình khuyến mãi đã áp dụng (nếu có).
     * Giả sử bạn có entity tên là Promotion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id") // Đây sẽ là cột promotion_id trong DB
    private Promotion promotion; // Import com.oneshop.entity.Promotion (Hoặc tên entity tương ứng của bạn)

    // === KẾT THÚC THAY ĐỔI ===

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal total; // Đây là tổng cuối cùng (subtotal + shipping - discount)

    // --- Liên kết ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop; // Import com.oneshop.entity.Shop

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id") // nullable = true
    private User shipper; // Shipper cũng là User

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderDetail> orderDetails = new ArrayList<>(); // Import com.oneshop.entity.OrderDetail

    // Lombok sẽ tự động tạo getter/setter, không cần viết tay
}