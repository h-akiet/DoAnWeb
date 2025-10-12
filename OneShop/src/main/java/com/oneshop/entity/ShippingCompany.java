package com.oneshop.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal fee;
}