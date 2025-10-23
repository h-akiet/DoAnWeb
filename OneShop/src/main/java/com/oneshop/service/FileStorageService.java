package com.oneshop.service.vendor;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface FileStorageService {

    // Khởi tạo thư mục lưu trữ
    public void init();

    // Lưu file
    public String save(MultipartFile file);

    // Tải file
    public Resource load(String filename);

    // Xóa file
    public void delete(String filename);
}