package com.oneshop.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "order_details")
@Getter
@Setter
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Liên kết ngược lại Order cha

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant productVariant; // Liên kết tới sản phẩm (biến thể) đã mua

    @Column(nullable = false)
    private int quantity; // Số lượng mua

    @Column(nullable = false)
    private BigDecimal price; // Giá của sản phẩm TẠI THỜI ĐIỂM MUA
}

