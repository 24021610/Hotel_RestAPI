# Hotel Booking REST API (Nhánh: TungBeng)

Dự án Backend RESTful API quản lý và đặt phòng khách sạn xây dựng trên nền tảng **Java 17 & Spring Boot**, tích hợp **Spring Security, JWT, Spring Data JPA và MySQL**.

---

## 1. Tóm tắt các module & tính năng đã thực hiện

### 🏨 Quản lý phòng (Room Management)
- **CRUD phòng đầy đủ**: Tạo mới, xem danh sách, tra cứu theo ID / số phòng, cập nhật từng phần (PATCH), và xóa phòng.
- **Ràng buộc nghiệp vụ**:
  - Tự động chuẩn hóa và kiểm tra trùng lặp số phòng (`ROOM_EXISTED - 1007`).
  - Quản lý trạng thái thực tế của phòng qua trường `occupied` (mặc định `false`, không cho phép ghi đè tự do qua PATCH).
  - **Chặn xóa phòng đang có khách**: Nếu `occupied == true`, từ chối xóa và trả về mã lỗi nghiệp vụ riêng (`ROOM_CANNOT_DELETE_OCCUPIED - 1022`).

### 🔐 Xác thực & Phân quyền (Authentication & Authorization)
- **Cơ chế xác thực**: Cấp phát và xác thực JSON Web Token (JWT) qua Nimbus JOSE + Spring OAuth2 Resource Server.
- **Phân quyền tại tầng Service (`@PreAuthorize`)**:
  - `POST /rooms`, `PATCH /rooms/{id}`, `DELETE /rooms/{id}`: Giới hạn chỉ tài khoản có quyền `ADMIN` (`hasAuthority('ADMIN')`).
  - Khách hàng thông thường (`USER`) hoặc người dùng chưa đăng nhập chỉ được phép thực hiện tra cứu danh sách phòng.

### 🛡️ Xử lý ngoại lệ tập trung (Global Exception Handling)
- Đồng bộ hóa toàn bộ response lỗi theo format chuẩn:
  ```json
  {
    "code": 1022,
    "message": "Cannot delete room that is currently occupied"
  }
  ```
- **Xử lý phân quyền 403**: Sửa import chuẩn `org.springframework.security.access.AccessDeniedException`, trả về đúng HTTP 403 Forbidden thay vì lỗi 500/400.
- **Bảo mật dữ liệu đầu vào**: Bắt `HttpMessageNotReadableException` khi JSON sai cú pháp hoặc enum sai giá trị (trả về HTTP 400 kèm thông báo an toàn, không echo lại input tùy ý của người dùng).
- **Chuẩn hóa mã HTTP 500**: Ngoại lệ chưa lường trước (`Exception.class`) trả về đúng HTTP 500 Internal Server Error thay vì HTTP 400.

---

## 2. Hướng dẫn kiểm thử nhanh qua Postman

### Tài khoản mặc định (Tự động khởi tạo khi chạy app)
* **Admin**: `admin` / `admin` (Authority: `ADMIN`)
* **User**: `user` / `user` (Authority: `USER`)

---

### Bước 1: Lấy Token đăng nhập
* **Endpoint**: `POST http://localhost:8080/auth/login`
* **Headers**: `Content-Type: application/json`
* **Body** (lấy token Admin):
  ```json
  {
    "username": "admin",
    "password": "admin"
  }
  ```
* Copy chuỗi `result.token` trong phản hồi để sử dụng cho các request sau.

---

### Bước 2: Thiết lập Header xác thực
Với mọi request cần đăng nhập, thêm header:
```text
Authorization: Bearer <TOKEN_VỪA_LẤY>
```

---

### Bước 3: Test các mã trạng thái và mã lỗi chính

| Kịch bản test | Method & URL | Header Auth | Body gửi lên | Kết quả kỳ vọng |
| :--- | :--- | :--- | :--- | :--- |
| **1. User thường gọi CUD phòng** | `POST /rooms` hoặc `DELETE /rooms/{id}` | Token `USER` | Bất kỳ | **HTTP 403 Forbidden**<br>`"code": 1006, "message": "Unauthorized"` |
| **2. Admin tạo phòng thành công** | `POST /rooms` | Token `ADMIN` | `{"number": "101", "noOfPerson": 2, "type": "SINGLE", "price": 100.0}` | **HTTP 200 OK**<br>Trả về thông tin phòng đã tạo |
| **3. Xóa phòng đang có khách** | `DELETE /rooms/{id}` (phòng có `occupied = true`) | Token `ADMIN` | None | **HTTP 400 Bad Request**<br>`"code": 1022, "message": "Cannot delete room that is currently occupied"` |
| **4. Xóa phòng trống bình thường** | `DELETE /rooms/{id}` (phòng có `occupied = false`) | Token `ADMIN` | None | **HTTP 200 OK**<br>`"message": "Room deleted successfully"` |
| **5. Enum sai giá trị** | `POST /rooms` | Token `ADMIN` | `{"number": "102", "noOfPerson": 2, "type": "INVALID_TYPE", "price": 100.0}` | **HTTP 400 Bad Request**<br>`"code": 1001, "message": "Invalid value for enum field"` |
| **6. JSON sai cú pháp** | `POST /rooms` | Token `ADMIN` | `{"number": "103",` (JSON hỏng) | **HTTP 400 Bad Request**<br>`"code": 1001, "message": "Malformed JSON request or invalid field format"` |
| **7. Không truyền Token** | `GET /rooms` hoặc `POST /rooms` | Không có Header | None | **HTTP 401 Unauthorized**<br>`"code": 1006, "message": "Unauthenticated"` |
