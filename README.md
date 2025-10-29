OneShop - Website Bán Mỹ Phẩm


OneShop là một trang web thương mại điện tử chuyên cung cấp các sản phẩm mỹ phẩm đa dạng, kết nối người bán và người mua. Dự án này bao gồm các chức năng quản lý sản phẩm, đơn hàng, người dùng, gian hàng, khuyến mãi, vận chuyển và tích hợp thanh toán.


<p align="center">
<img src="trangchu.jpg" alt="Ảnh chụp màn hình ứng dụng" width="600"/>
</p> 

Mục lục (Table of Contents)

Tính năng

Công nghệ sử dụng

Cài đặt

Cách sử dụng

Đóng góp

Liên hệ

Tính năng ✨

Liệt kê các tính năng nổi bật của dự án:

Quản lý Người dùng (Đăng ký, Đăng nhập, Phân quyền Admin/Vendor/User/Shipper).

Chat giữa Khách hàng và Shop.

User

    Giỏ hàng và Thanh toán (Thêm vào giỏ, Quản lý giỏ hàng, Đặt hàng, Áp dụng khuyến mãi).

    Đánh giá Sản phẩm (Bằng văn bản, hình ảnh).

    Quản lý sổ địa chỉ (User có nhiều địa chỉ nhận hàng khác nhau).

    Cập nhật những thông tin mới của cửa hàng (Voucher khuyến mãi, sản phẩm mới được thêm vào).

    Lựa chọn thanh toán Cod hoặc VNPAY.

Vendor

    Quản lý Sản phẩm (Thêm/Sửa/Xóa sản phẩm, Biến thể sản phẩm, Danh mục, Thương hiệu).

    Quản lý Đơn hàng (Xem đơn hàng, Lịch sử mua hàng, Cập nhật trạng thái).

    Quản lý Doanh thu.

    Quản lý Cửa hàng (Thương hiệu, Thông tin chung).

Admin

    Quản lý Gian hàng (Đăng ký shop, Duyệt sản phẩm). 

    Quản lý Người dùng.

    Quản lý Danh mục sản phẩm.

    Quản lý Khuyến mãi/Chiết khấu.

    Quản lý Vận chuyển (Thêm/Sửa nhà vận chuyển, Quy tắc tính phí).


Công nghệ sử dụng 💻

Dự án này được xây dựng bằng các công nghệ sau:

Backend: Spring Boot, Spring Security Spring Data JPA / Hibernate, JWT (JSON Web Tokens), Spring WebSocket:

Frontend: Thymeleaf, HTML5, CSS3, JavaScript, Bootstrap, AJAX.

Database: SQL Server

Build Tool: Maven

Version Control: GitHub


Cài đặt 🔧

Hướng dẫn từng bước cách cài đặt dự án của bạn.

# Clone repository
git clone https://github.com/h-akiet/DoAnWeb.git
cd DoAnWeb

# Cấu hình file application.properties (ví dụ: kết nối CSDL)
# src/main/resources/application.properties

# Build dự án bằng Maven
mvn clean install

# Chạy ứng dụng
mvn spring-boot:run


Cách sử dụng 🚀

Truy cập ứng dụng tại http://localhost:8080 (hoặc cổng bạn đã cấu hình).

Đăng ký tài khoản hoặc đăng nhập bằng tài khoản admin/user mẫu (nếu có).

Sử dụng các chức năng tương ứng với vai trò của bạn.

(Tùy chọn) Cấu hình ⚙️

Các cấu hình chính nằm trong file src/main/resources/application.properties.

spring.datasource.url: Chuỗi kết nối đến SQL Server.

jwt.secret: Khóa bí mật để ký JWT.

jwt.expirationMs: Thời gian hết hạn của JWT (mili giây).

... (Các cấu hình khác nếu có)

Đóng góp 🤝

Hướng dẫn cách người khác có thể đóng góp vào dự án của bạn. Ví dụ:

Fork repository

Tạo branch mới (git checkout -b feature/AmazingFeature)

Commit thay đổi (git commit -m 'Add some AmazingFeature')

Push lên branch (git push origin feature/AmazingFeature)

Mở Pull Request


Liên hệ 📧

Nguyễn Hoàng Anh Kiệt - 23110247@student.hcmute.edu.vn

Link dự án: https://github.com/h-akiet/DoAnWeb.git

**Giáo viên hướng dẫn:** Nguyễn Hữu Trung
