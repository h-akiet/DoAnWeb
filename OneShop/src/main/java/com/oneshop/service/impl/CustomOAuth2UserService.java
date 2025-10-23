package com.oneshop.service.vendor.impl;

import com.oneshop.entity.vendor.Role;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.entity.vendor.User;
import com.oneshop.repository.vendor.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired // Cần PasswordEncoder nếu bạn muốn tạo mật khẩu ngẫu nhiên
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder; 

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = null;
        String name = null;
        String providerId = null; 
        String provider = userRequest.getClientRegistration().getRegistrationId(); 

        // Lấy thông tin tùy theo Provider
        if ("google".equalsIgnoreCase(provider)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("sub"); 
        } else if ("facebook".equalsIgnoreCase(provider)) {
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            providerId = (String) attributes.get("id"); 
        } else {
            throw new OAuth2AuthenticationException("Provider không được hỗ trợ: " + provider);
        }

        // Ưu tiên tìm user bằng email nếu có
        Optional<User> userOptional = Optional.empty();
        if (email != null && !email.isEmpty()) {
             userOptional = userRepository.findByEmail(email);
        }
        
        // (Trong tương lai có thể thêm: Nếu không tìm thấy bằng email, thử tìm bằng providerId)

        User user;
        if (userOptional.isPresent()) {
            // User đã tồn tại -> Cập nhật thông tin nếu cần
            user = userOptional.get();
             if (user.getFullName() == null || user.getFullName().isEmpty() && name != null) {
                 user.setFullName(name);
             }
             // Đảm bảo user có quyền VENDOR nếu đăng nhập qua OAuth2
             user.setRole(Role.VENDOR); 
             // (Cân nhắc: Cập nhật providerId/username nếu user này trước đó đăng ký bằng form?)

        } else {
            // User chưa tồn tại -> Tạo mới
            user = new User();
            user.setEmail(email);
            user.setFullName(name);
            user.setRole(Role.VENDOR); 
            // Tạo username duy nhất từ provider và ID
            user.setUsername(provider + "_" + providerId); 
            // Tạo mật khẩu ngẫu nhiên (vì không dùng để đăng nhập)
            user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString())); 

            Shop newShop = new Shop();
            // Đảm bảo tên shop không bị null nếu tên từ provider bị null
            newShop.setName((name != null ? name : "New Vendor") + "'s Shop"); 
            newShop.setDescription("Shop được tạo tự động qua " + provider + " Login.");
            newShop.setContactEmail(email);
            // Số điện thoại sẽ null, cần cập nhật sau
            newShop.setUser(user);
            user.setShop(newShop);
            
            user = userRepository.save(user); // Lưu user mới (và shop)
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        // Thuộc tính định danh chính (key trong map attributes để Spring Security lấy username)
        String nameAttributeKey = provider.equalsIgnoreCase("google") ? "sub" : "id";

        // Trả về đối tượng User cho Spring Security
        // Quan trọng: Phải trả về username trong DB của bạn làm Principal name
        return new DefaultOAuth2User(authorities, attributes, nameAttributeKey); 
    }
}