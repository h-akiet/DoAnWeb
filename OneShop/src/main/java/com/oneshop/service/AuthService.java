package com.oneshop.service.vendor;

import com.oneshop.dto.vendor.RegisterRequestDto;

public interface AuthService {
    void registerNewVendor(RegisterRequestDto registerRequestDto);
    boolean isUsernameExist(String username);
    boolean isEmailExist(String email);
}