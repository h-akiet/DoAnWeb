package com.oneshop.entity;

import jakarta.persistence.*;
// Bỏ import BigDecimal nếu không dùng nữa
// import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList; 
import java.util.List;     

@Entity
@Table(name = "SHIPPING_COMPANIES")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shippingId;

    @Column(length = 150, nullable = false, columnDefinition = "nvarchar(150)") 
    private String name;

    @Column(length = 20) 
    private String phone; 

    @Column(nullable = false)
    private Boolean isActive = true; 

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingRule> rules = new ArrayList<>(); 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_company_id")
    private ShippingCompany shippingCompany;

}