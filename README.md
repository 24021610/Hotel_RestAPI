# Hotel Reservation API Service

Hệ thống RESTful API Backend chuyên nghiệp phục vụ nghiệp vụ đặt phòng khách sạn. Dự án tuân thủ nghiêm ngặt mô hình kiến trúc phân tầng (Layered Architecture) và chú trọng tối ưu các thuộc tính chất lượng (Quality Attributes) của phần mềm.

---

## 📑 Mục lục
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Bảo mật & Xác thực](#-bảo-mật--xác-thực)
- [Chức năng nghiệp vụ](#-chức-năng-nghiệp-vụ)
- [Tài liệu API (Swagger UI)](#-tài-liệu-api-swagger-ui)
- [Cài đặt & Triển khai (Docker)](#-cài-đặt--triển-khai-docker)

---

## 🏛 Kiến trúc hệ thống

Dự án áp dụng mô hình **Layered Architecture** và tổ chức mã nguồn theo khía cạnh kỹ thuật (**Package by Layer**). Cấu trúc trong `com.example.hotel_restapi` gồm các package chính:

* `controller` **(API Layer)**: Tiếp nhận HTTP Request và trả về HTTP Response (`AuthenticationController`, `RoomController`, `UserController`,...). Định tuyến luồng dữ liệu, không chứa logic nghiệp vụ.
* `service` **(Business Logic Layer)**: Xử lý toàn bộ nghiệp vụ cốt lõi (`AuthenticationService`, `RoomService`, `UserService`,...). Hoàn toàn độc lập, không phụ thuộc vào framework Web hay chi tiết Database.
* `repository` **(Data Access Layer)**: Tương tác với cơ sở dữ liệu MySQL thông qua Spring Data JPA (`RoomRepository`, `UserRepository`,...).
* `entity` & `enums`: Định nghĩa các đối tượng ánh xạ trực tiếp với bảng trong Database (`Room`, `User`,...) và các hằng số trạng thái (`Role`, `RoomType`,...).
* `dto` & `mapper`: Tách biệt dữ liệu nhận vào (Request) và trả về (Response) thông qua Data Transfer Object (DTO). Sử dụng mapper (`RoomMapper`, `UserMapper`,...) để che giấu cấu trúc database khỏi client.
* `exception`: Xử lý lỗi tập trung qua `@ControllerAdvice` (`GlobalExceptionHandler`) cùng mã lỗi chuẩn hóa (`ErrorCode`, `AppException`), đảm bảo format phản hồi đồng nhất.
* `configuration`: Chứa cấu hình bảo mật (Spring Security, JWT) và khởi tạo các Bean hệ thống.

---

## 🔐 Bảo mật & Xác thực

* **Cơ chế**: Sử dụng JSON Web Token (JWT) cho Authentication và Authorization.
* **Xử lý tập trung**: Kiểm tra tính hợp lệ của Token và quyền hạn người dùng tại tầng Filter/Middleware của Spring Security.
* **Mã nguồn tinh gọn (Clean Code)**: Tầng Controller sạch bóng code kiểm tra quyền thủ công; bảo vệ tài nguyên hoàn toàn qua các Annotation bảo mật (e.g. `@PreAuthorize`).

---

## 🚀 Chức năng nghiệp vụ

### 1. Quản lý Tài khoản (Auth & Users)
* Đăng ký tài khoản khách hàng mới.
* Đăng nhập và cấp phát JWT.
* Xem và cập nhật thông tin cá nhân.
* Phân quyền Admin: Quản lý danh sách toàn bộ người dùng trong hệ thống.

### 2. Quản lý Kho phòng (Rooms)
* Tra cứu, lọc và liệt kê danh sách phòng trống theo tiêu chí.
* Phân quyền Admin: Toàn quyền CRUD (Thêm, sửa thông tin, ngưng kinh doanh) đối với từng phòng.

### 3. Nghiệp vụ Đặt phòng (Bookings)
* Tạo đơn đặt phòng (tự động nhận diện User thông qua JWT).
* Tự động tính toán tổng chi phí dựa trên `basePrice` và số đêm lưu trú thực tế.
* **Xử lý chống chồng chéo (Conflict Validation)**: Tự động kiểm tra khoảng thời gian `checkIn` - `checkOut`, ngăn chặn việc đặt trùng phòng tại cùng một thời điểm.
* Hỗ trợ hủy đặt phòng và tích hợp mô phỏng thanh toán.

---

## 📖 Tài liệu API (Swagger UI)

Dự án tích hợp sẵn thư viện `springdoc-openapi` để tự động tạo tài liệu tương tác API trực quan:

* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) *(khi chạy local)*
* **OpenAPI Spec (JSON)**: `http://localhost:8080/v3/api-docs`

---

## 🐳 Cài đặt & Triển khai (Docker)

Hệ thống đã cấu hình sẵn `Dockerfile` và `docker-compose.yml` để đóng gói toàn bộ môi trường (App & MySQL Database).

### Yêu cầu tiên quyết
* Máy tính đã cài đặt [Docker](https://docs.docker.com/get-docker/) và Docker Compose.

### Các bước thực hiện

**Bước 1: Clone repository**
**Bước 2: Khởi chạy toàn bộ hệ thống**

Chạy lệnh sau tại thư mục gốc của dự án để build image và khởi động các container ở chế độ chạy ngầm (detached mode):

```bash
docker compose up --build -d
```bash
git clone [https://github.com/your-username/hotel-booking-backend.git](https://github.com/your-username/hotel-booking-backend.git)
cd hotel-booking-backend
