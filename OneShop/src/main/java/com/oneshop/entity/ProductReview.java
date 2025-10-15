package com.oneshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "PRODUCT_REVIEWS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY) // Liên kết 1-1 với đơn hàng (để xác minh đã mua)
    @JoinColumn(name = "order_id", unique = true) 
    private Order order;

    @Column(nullable = false)
    private Integer rating; // 1-5 sao

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String comment;

    @Column(name = "review_date")
    private LocalDateTime reviewDate = LocalDateTime.now();

    // Mối quan hệ: Một đánh giá có thể có nhiều media (ảnh/video)
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ReviewMedia> media;
}