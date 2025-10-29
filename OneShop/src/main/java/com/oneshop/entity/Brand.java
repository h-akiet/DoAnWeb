package com.oneshop.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Entity
@Table(name = "BRANDS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long brandId;

    @Column(length = 150, nullable = false, columnDefinition = "nvarchar(150)") 
    private String name;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;
}
