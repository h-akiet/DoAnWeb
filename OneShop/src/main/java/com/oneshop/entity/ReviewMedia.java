package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "REVIEW_MEDIA")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private ProductReview review;

    @Column(name = "media_url", nullable = false, columnDefinition = "VARCHAR(500)") // Tăng độ dài cột
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 10) // Giữ độ dài nhỏ
    private MediaType mediaType; // <<< CẦN IMPORT TỪ FILE RIÊNG
}
// KHÔNG ĐỊNH NGHĨA ENUM TẠI ĐÂY