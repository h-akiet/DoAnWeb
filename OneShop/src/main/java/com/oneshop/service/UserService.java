package com.oneshop.service.vendor;

import com.oneshop.dto.vendor.ProfileUpdateDto;
import com.oneshop.entity.vendor.User;

public interface UserService {
    User findByUsername(String username);
    User updateUserProfile(String username, ProfileUpdateDto profileUpdateDto);
}