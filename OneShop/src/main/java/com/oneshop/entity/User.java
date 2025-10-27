package com.oneshop.entity; 

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonManagedReference; // Dùng cho Address

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


@Entity // Chỉ cần 1 @Entity
@Table(name = "USERS") // Chỉ cần 1 @Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"addresses", "cart", "shop", "role"}) // Thêm role, shop vào exclude
@ToString(exclude = {"addresses", "cart", "shop", "role"})      // Thêm role, shop vào exclude
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100) // Thêm length
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 150) // Thêm length
    private String email;

    @Column(name = "full_name", columnDefinition = "nvarchar(255)") // Đổi tên cột và giữ nvarchar
    private String fullName;

    @Column(name = "phone_number", length = 20) // Đổi tên cột và thêm length
    private String phoneNumber;

    @Column(columnDefinition = "nvarchar(500)") // Giữ nvarchar cho địa chỉ chung (nếu cần)
    private String address; // Địa chỉ chung, có thể không cần thiết nếu dùng Address entity

    @Column(nullable = false)
    private boolean activated = false; // Trạng thái kích hoạt

    // --- SỬA QUAN HỆ ROLE ---
    // Bỏ: private Set<Role> roles = new HashSet<>();
    // Bỏ: @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;

    // Thêm: Quan hệ ManyToOne tới Entity Role mới
    @ManyToOne(fetch = FetchType.EAGER) // EAGER để dễ lấy role khi xác thực
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    // -------------------------

    // --- CÁC QUAN HỆ KHÁC ---
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cart cart;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // Giữ lại để quản lý JSON two-way reference với Address
    private Set<Address> addresses = new HashSet<>();

    // Quan hệ OneToOne với Shop (Một User Vendor có một Shop)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shop shop; // Shop thuộc về User này (nếu là Vendor)

    // --- IMPLEMENT UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        // Quan trọng: Thêm tiền tố "ROLE_" và lấy tên từ Enum RoleName
        if (this.role != null && this.role.getName() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.getName().name()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Hoặc logic kiểm tra hết hạn tài khoản nếu có
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Hoặc logic kiểm tra khóa tài khoản
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Hoặc logic kiểm tra hết hạn mật khẩu
    }

    @Override
    public boolean isEnabled() {
        return this.activated; // Tài khoản chỉ enabled khi đã activated
    }

    // --- GETTER/SETTER ID (Giữ lại để tương thích code cũ nếu cần) ---
    public Long getUserId() {
        return id;
    }

    public void setUserId(Long id) {
        this.id = id;
    }
}