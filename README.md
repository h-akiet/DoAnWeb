# OneShop - Website Bán Mỹ Phẩm 💄

## Giới thiệu chung

OneShop là một trang web thương mại điện tử (E-commerce) chuyên cung cấp các sản phẩm mỹ phẩm đa dạng, kết nối người bán và người mua. Dự án được thiết kế theo mô hình **đa gian hàng (Multi-vendor)**, bao gồm các chức năng quản lý sản phẩm, đơn hàng, người dùng, gian hàng, khuyến mãi, vận chuyển và tích hợp thanh toán.

<p align="center">
  <img src="https://raw.githubusercontent.com/h-akiet/DoAnWeb/main/trangchu.jpg" alt="Ảnh chụp màn hình trang chủ OneShop" width="700"/>
</p>

## Mục lục (Table of Contents)

1. [Tính năng nổi bật](#tính-năng-nổi-bật-)
2. [Kiến trúc và Công nghệ](#kiến-trúc-và-công-nghệ-)
3. [Cấu hình & Cài đặt](#cài-đặt-dự-án-)
4. [Cách sử dụng](#cách-sử-dụng-)
5. [Liên hệ](#liên-hệ-)

---

## Tính năng nổi bật ✨

Các chức năng chính được triển khai, phân chia rõ ràng theo vai trò:

### 1. Tính năng chung

* **Xác thực & Phân quyền:** Quản lý Người dùng (Đăng ký, Đăng nhập). Phân quyền chi tiết: **Admin, Vendor, User, Shipper** (sử dụng Spring Security và JWT).
* **Giao tiếp:** Tích hợp **Chat** thời gian thực giữa Khách hàng và Shop (sử dụng Spring WebSocket).

### 2. User (Khách hàng)

* **Giỏ hàng & Thanh toán:** Quản lý giỏ hàng, đặt hàng. Lựa chọn thanh toán **COD** hoặc **VNPAY**.
* **Khuyến mãi:** Áp dụng voucher/khuyến mãi trong quá trình đặt hàng.
* **Đánh giá:** Đánh giá Sản phẩm (Bằng văn bản và hình ảnh đính kèm).
* **Quản lý cá nhân:** Quản lý sổ địa chỉ (nhiều địa chỉ nhận hàng).

### 3. Vendor (Gian hàng)

* **Quản lý Sản phẩm:** Thêm/Sửa/Xóa sản phẩm, quản lý Biến thể (Variant), Danh mục, và Thương hiệu.
* **Quản lý Đơn hàng:** Xem, cập nhật trạng thái đơn hàng và lịch sử mua hàng.
* **Quản lý Tài chính:** Quản lý Doanh thu.
* **Thông tin Shop:** Quản lý Thương hiệu và Thông tin chung của cửa hàng.

### 4. Admin (Quản trị viên)

* **Quản lý Gian hàng:** Duyệt đăng ký Shop, kiểm duyệt Sản phẩm.
* **Quản lý Hệ thống:** Quản lý Người dùng, Danh mục sản phẩm.
* **Quản lý Khuyến mãi:** Quản lý Khuyến mãi/Chiết khấu App.
* **Quản lý Vận chuyển:** Thêm/Sửa nhà vận chuyển và Quy tắc tính phí.

---

## Kiến trúc và Công nghệ 💻

Dự án sử dụng mô hình kiến trúc MVC (Model-View-Controller) với Spring Boot là nền tảng chính.

| Khu vực | Công nghệ | Vai trò chính | Badge |
| :--- | :--- | :--- | :--- |
| **Backend Core** | **Spring Boot** | Khung phát triển ứng dụng (Core Framework) | ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-green?style=flat-square&logo=spring-boot) |
| **Ngôn ngữ** | **Java 22** | Ngôn ngữ lập trình chính | ![Java](https://img.shields.io/badge/Java-22-orange?style=flat-square&logo=openjdk) |
| **Bảo mật** | **Spring Security** | Xác thực & Phân quyền theo vai trò (Roles) | ![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-blue?style=flat-square&logo=spring-security) |
| **Bảo mật** | **JWT** | Xác thực người dùng qua Token (Auth Token) | ![JWT](https://img.shields.io/badge/JWT-black?style=flat-square&logo=jsonwebtokens) |
| **Database** | **Spring Data JPA / Hibernate** | Tương tác với CSDL (ORM) | ![JPA/Hibernate](https://img.shields.io/badge/JPA_/_Hibernate-orange?style=flat-square) |
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
    * Cập nhật thông tin kết nối trong file `src/main/resources/application.properties` (đảm bảo thông tin JWT và CSDL là chính xác):

        ```properties
        # Cấu hình CSDL
        spring.datasource.url=jdbc:sqlserver://[YourServer];databaseName=[YourDB];integratedSecurity=true;encrypt=false;trustServerCertificate=true;characterEncoding=UTF-8
       
        # Cấu hình JWT (Ví dụ)
        jwt.secret=[Khóa bí mật mạnh]
        jwt.expirationMs=[Thời gian hết hạn JWT (ms)]
        ```

3.  **Build và Chạy:**

    * Sử dụng Maven để build dự án và chạy ứng dụng Spring Boot:

    ```bash
    mvn clean install
    mvn spring-boot:run
    ```

---
## Cách sử dụng 🚀

Hướng dẫn chi tiết để trải nghiệm đầy đủ các tính năng của **OneShop** theo từng vai trò.

### 1. Truy cập hệ thống

- Mở trình duyệt và truy cập: `http://localhost:8080` (sau khi chạy dự án thành công).
- Trang chủ sẽ hiển thị danh sách sản phẩm và các gian hàng.

---

### 2. Tài khoản mẫu (Demo Accounts)

| Vai trò       | Email                          | Mật khẩu    | Ghi chú |
|---------------|--------------------------------|-------------|--------|
| **Admin**     | `admin@oneshop.com`            | `admin123`  | Quản trị toàn hệ thống |

* Vai tròL User, Vendor: có thể đăng kí ở giao diện hệ thống. Đối với User có thể đăng kí Shop và nâng cấp role Vendor khi được duyệt bởi Admin

> **Lưu ý:** Đăng nhập bằng tài khoản tương ứng để trải nghiệm đúng chức năng.

---

### 3. Hướng dẫn theo vai trò

#### **Khách hàng (User)**
1. **Đăng ký / Đăng nhập** → Nhấn nút **Đăng nhập** ở góc trên bên phải.
2. **Duyệt sản phẩm** → Tìm kiếm, lọc theo danh mục, thương hiệu.
3. **Thêm vào giỏ hàng** → Chọn biến thể (loại, số lượng) → **Thêm vào giỏ**.
4. **Thanh toán**:
   - Vào giỏ hàng → Kiểm tra sản phẩm.
   - Nhập **mã khuyến mãi** (nếu có).
   - **Thanh toán**:
   - Chọn địa chỉ giao hàng → Phương thức thanh toán (**COD** hoặc **VNPAY**).
5. **Theo dõi đơn hàng** → Xem trạng thái trong **Lịch sử mua hàng**.
6. **Đánh giá sản phẩm** → Sau khi nhận hàng, vào đơn hàng → **Viết đánh giá + tải ảnh**.

#### **Chủ shop (Vendor)**
1. Đăng nhập bằng tài khoản Vendor.
2. Vào **Dashboard Shop** → Quản lý:
   - **Sản phẩm**: Thêm/sửa/xóa, quản lý biến thể.
   - **Đơn hàng**: Xác nhận, bàn giao vận chuyển.
   - **Doanh thu**: Xem báo cáo theo ngày/tháng.
   - **Thông tin shop**: Cập nhật logo, banner, mô tả.

#### **Quản trị viên (Admin)**
1. Đăng nhập bằng tài khoản Admin.
2. Vào **Admin Panel** (`/admin`) → Quản lý:
   - **Người dùng**: Khóa/mở tài khoản.
   - **Gian hàng**: Duyệt đơn đăng ký mở shop.
   - **Sản phẩm**: Kiểm duyệt trước khi lên kệ.
   - **Khuyến mãi**: Tạo mã giảm giá toàn hệ thống.
   - **Vận chuyển**: Cấu hình phí theo khu vực.

#### **Nhân viên giao hàng (Shipper)**
1. Đăng nhập bằng tài khoản Shipper.
2. Xem danh sách **đơn hàng cần giao**.
3. Cập nhật trạng thái: **Đã giao**.

---

### 4. Tính năng nổi bật cần thử

| Tính năng           | Cách trải nghiệm |
|---------------------|------------------|
| **Chat thời gian thực** | Mở trang sản phẩm → Nhấn biểu tượng chat → Gửi tin nhắn đến shop |
| **Thanh toán VNPAY** | Chọn VNPAY khi thanh toán → Nhập thẻ (dùng tài khoản test) |
| **Tải ảnh đánh giá** | Sau khi nhận hàng → Vào đơn hàng → Tải ảnh |

---
## Liên hệ 📧

* **Nhóm tác giả:**

  | STT | Họ và tên              | MSSV     |
  |-----|------------------------|----------|
  | 1   | Nguyễn Hoàng Anh Kiệt  | 23110247 |
  | 2   | Trần Thành Trung       | 23110351 |
  | 3   | Nguyễn Trung Hậu       | 23110212 |
  | 4   | Nguyễn Thị Thu Linh    | 23110254 |
* **Email:** 23110247@student.hcmute.edu.vn
* **Giáo viên hướng dẫn:** Nguyễn Hữu Trung

Link dự án: <https://github.com/h-akiet/DoAnWeb.git>
```eof
