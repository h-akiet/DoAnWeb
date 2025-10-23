package com.oneshop.entity.vendor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String fullName;
    @Column(columnDefinition = "nvarchar(500)")
    private String address;
    
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    // Một User có thể có một Shop (nếu họ là VENDOR)
    // mappedBy = "user" nghĩa là việc quản lý quan hệ này được thực hiện bởi thuộc tính "user" bên class Shop
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shop shop;
}