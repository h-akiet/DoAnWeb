# OneShop - Website Bán Mỹ Phẩm 💄

## Giới thiệu chung

OneShop là một trang web thương mại điện tử (E-commerce) chuyên cung cấp các sản phẩm mỹ phẩm đa dạng, kết nối người bán và người mua. Dự án được thiết kế theo mô hình **đa gian hàng (Multi-vendor)**, bao gồm các chức năng quản lý sản phẩm, đơn hàng, người dùng, gian hàng, khuyến mãi, vận chuyển và tích hợp thanh toán (COD/VNPAY).

<p align="center">
  <img src="https://raw.githubusercontent.com/h-akiet/DoAnWeb/main/trangchu.jpg" alt="Ảnh chụp màn hình trang chủ OneShop" width="700"/>
</p>

## Badges & Trạng thái

![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?style=for-the-badge&logo=spring-boot)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-E04E00?style=for-the-badge&logo=thymeleaf&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL_Server-CC2927?style=for-the-badge&logo=microsoft-sql-server&logoColor=white)
![Status](https://img.shields.io/badge/Status-Hoàn_Thành_Cơ_Bản-blue?style=for-the-badge)

## Mục lục (Table of Contents)

1. [Tính năng nổi bật](#tính-năng-nổi-bật-)
2. [Kiến trúc và Công nghệ](#kiến-trúc-và-công-nghệ-)
3. [Cấu hình & Cài đặt](#cài-đặt-dự-án-)
4. [Cách sử dụng](#cách-sử-dụng-)
5. [Liên hệ](#liên-hệ-)

---

## Tính năng nổi bật ✨

Các chức năng chính được triển khai trong dự án OneShop:

### 1. User (Khách hàng)

* **Giỏ hàng & Thanh toán:** Quản lý giỏ hàng, đặt hàng, áp dụng khuyến mãi. Hỗ trợ thanh toán **COD** và **VNPAY**.
* **Đánh giá:** Đánh giá Sản phẩm (Bằng văn bản và hình ảnh).
* **Quản lý cá nhân:** Quản lý sổ địa chỉ, cập nhật thông báo (Voucher, sản phẩm mới).

### 2. Vendor (Gian hàng)

* **Quản lý sản phẩm:** Thêm/Sửa/Xóa sản phẩm, quản lý Biến thể (Variant), Danh mục, và Thương hiệu.
* **Quản lý Đơn hàng:** Xem, cập nhật trạng thái đơn hàng.
* **Quản lý Tài chính:** Quản lý Doanh thu của Shop.

### 3. Admin (Quản trị viên)

* **Quản lý Gian hàng:** Duyệt đăng ký Shop, kiểm duyệt Sản phẩm.
* **Quản lý Hệ thống:** Quản lý Người dùng, Danh mục sản phẩm, Khuyến mãi/Chiết khấu App.
* **Quản lý Vận chuyển:** Thêm/Sửa nhà vận chuyển và Quy tắc tính phí.
* **Hệ thống:** Tích hợp **Chat** thời gian thực giữa Khách hàng và Shop.

---

## Kiến trúc và Công nghệ 💻

Dự án sử dụng mô hình kiến trúc MVC (Model-View-Controller) với Spring Boot là nền tảng chính.

| Khu vực | Công nghệ | Vai trò chính | Badge |
| :--- | :--- | :--- | :--- |
| **Backend Core** | **Spring Boot** | Khung phát triển ứng dụng (Core Framework) | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?style=flat-square&logo=spring-boot) |
| **Bảo mật** | **Spring Security** | Xác thực & Phân quyền theo vai trò (Roles) | ![Spring Security](https://imgādges.io/badge/Spring_Security-6.x-blue?style=flat-square&logo=spring-security) |
| **Bảo mật** | **JWT** | Xác thực người dùng qua Token (Auth Token) | ![JWT](https://img.shields.io/badge/JWT-black?style=flat-square&logo=jsonwebtokens) |
| **Database** | **Spring Data JPA / Hibernate** | Tương tác với cơ sở dữ liệu (ORM) | ![JPA/Hibernate](https://img.shields.io/badge/JPA_/_Hibernate-orange?style=flat-square) |
| **Database** | **SQL Server** | Hệ quản trị CSDL quan hệ | ![SQL Server](https://img.shields.io/badge/SQL_Server-CC2927?style=flat-square&logo=microsoft-sql-server&logoColor=white) |
| **Realtime** | **Spring WebSocket** | Giao tiếp thời gian thực (Chat/Thông báo) | ![WebSocket](https://img.shields.io/badge/WebSocket-blue?style=flat-square&logo=websocket) |
| **Frontend** | **Thymeleaf** | Template Engine (Render View từ Backend) | ![Thymeleaf](https://img.shields.io/badge/Thymeleaf-E04E00?style=flat-square&logo=thymeleaf) |
| **Frontend** | **Bootstrap 5** | Thư viện CSS/UI (Giao diện Responsive) | ![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-7952B3?style=flat-square&logo=bootstrap&logoColor=white) |
| **Frontend** | **JavaScript/AJAX** | Tương tác động, gửi request bất đồng bộ | ![JavaScript](https://img.shields.io/badge/JavaScript-ES6+-yellow?style=flat-square&logo=javascript&logoColor=black) |
| **Build Tool** | **Maven** | Quản lý dependencies và Build | ![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apache-maven&logoColor=white) |

---

## Cấu hình & Cài đặt 🔧

Các bước cần thiết để khởi chạy dự án trên máy cục bộ của bạn:

1.  **Clone Repository:**

    ```bash
    git clone [https://github.com/h-akiet/DoAnWeb.git](https://github.com/h-akiet/DoAnWeb.git)
    cd DoAnWeb
    ```

2.  **Cấu hình Database:**

    * Tạo cơ sở dữ liệu mới trên SQL Server.
    * Cập nhật thông tin kết nối trong file `src/main/resources/application.properties`:

        ```properties
        # Cấu hình CSDL
        spring.datasource.url=jdbc:sqlserver://[YourServer];databaseName=[YourDB];encrypt=true;trustServerCertificate=true;
        spring.datasource.username=your_user
        spring.datasource.password=your_password

        # Cấu hình JWT (Bảo mật)
        jwt.secret: [Khóa bí mật mạnh]
        jwt.expirationMs: [Thời gian hết hạn JWT (ms)]
        ```

3.  **Build và Chạy:**

    * Sử dụng Maven để build dự án và chạy ứng dụng Spring Boot:

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

---

## Cách sử dụng 🚀

1.  **Truy cập:** Mở trình duyệt và truy cập `http://localhost:8080` (hoặc cổng bạn đã cấu hình).
2.  **Đăng nhập Admin:** Đăng nhập bằng tài khoản admin mẫu để quản lý hệ thống.
3.  **Đăng ký Shop (Vendor):** Đăng ký tài khoản User mới và đăng ký Shop (Vendor) để có quyền bán hàng.

---

## Liên hệ 📧

* **Tác giả:** Nguyễn Hoàng Anh Kiệt
* **Email:** 23110247@student.hcmute.edu.vn
* **Giáo viên hướng dẫn:** Nguyễn Hữu Trung

Link dự án: <https://github.com/h-akiet/DoAnWeb.git>
