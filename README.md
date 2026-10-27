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
| **1. User thường gọi CUD phòng** | `POST /rooms` hoặc `DELETE /rooms/{id}` | Token `USER` | Bất kỳ | **HTTP 403 Forbidden**<br>`"code": 1023, "message": "Unauthorized"` |
| **2. Admin tạo phòng thành công** | `POST /rooms` | Token `ADMIN` | `{"number": "101", "noOfPerson": 2, "type": "SINGLE", "price": 100.0}` | **HTTP 200 OK**<br>Trả về thông tin phòng đã tạo |
| **3. Xóa phòng đang có khách** | `DELETE /rooms/{id}` (phòng có `occupied = true`) | Token `ADMIN` | None | **HTTP 400 Bad Request**<br>`"code": 1022, "message": "Cannot delete room that is currently occupied"` |
| **4. Xóa phòng trống bình thường** | `DELETE /rooms/{id}` (phòng có `occupied = false`) | Token `ADMIN` | None | **HTTP 200 OK**<br>`"message": "Room deleted successfully"` |
| **5. Enum sai giá trị** | `POST /rooms` | Token `ADMIN` | `{"number": "102", "noOfPerson": 2, "type": "INVALID_TYPE", "price": 100.0}` | **HTTP 400 Bad Request**<br>`"code": 1001, "message": "Invalid value for enum field"` |
| **6. JSON sai cú pháp** | `POST /rooms` | Token `ADMIN` | `{"number": "103",` (JSON hỏng) | **HTTP 400 Bad Request**<br>`"code": 1001, "message": "Malformed JSON request or invalid field format"` |
| **7. Không truyền Token** | `GET /rooms` hoặc `POST /rooms` | Không có Header | None | **HTTP 401 Unauthorized**<br>`"code": 1006, "message": "Unauthenticated"` |

---

## 3. Cấu hình môi trường & Hướng dẫn bảo mật triển khai (Environment & Security)

### Biến môi trường CSDL (Database Configuration)
Hệ thống hỗ trợ cấu hình thông qua các biến môi trường thay vì gán cứng thông tin kết nối:
- `DB_URL`: JDBC URL kết nối MySQL (Mặc định dev local: `jdbc:mysql://localhost:3307/hotel_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true`).
- `DB_USERNAME`: Tên người dùng CSDL (Mặc định dev local: `root`).
- `DB_PASSWORD`: Mật khẩu người dùng CSDL (Mặc định dev local giữ giá trị cũ để tương thích với môi trường hiện tại của nhóm).

> [!WARNING]
> **Cảnh báo bảo mật mật khẩu CSDL**: Mật khẩu mặc định local đã tồn tại trong lịch sử commit của Git, do đó được coi là đã lộ công khai. Khi triển khai lên máy chủ thật, **bắt buộc phải đổi mật khẩu (rotate)** trên hệ quản trị CSDL và truyền mật khẩu mới an toàn qua biến môi trường `DB_PASSWORD`.
>
> **Yêu cầu TLS cho CSDL**: Cấu hình `useSSL=false` và `allowPublicKeyRetrieval=true` chỉ phục vụ môi trường phát triển cục bộ. Trên môi trường production, kết nối tới CSDL bắt buộc phải bật mã hóa đường truyền (`useSSL=true` trên `DB_URL`) để phòng chống tấn công nghe lén hoặc Man-in-the-Middle (MitM).

### Cấu hình tài khoản khởi tạo (Seed Accounts Configuration)
Hệ thống cho phép kiểm soát việc tự động sinh tài khoản mẫu qua các biến môi trường:
- `APP_INIT_ENABLED`: Bật/tắt việc tạo tài khoản mẫu (Mặc định dev: `true`; Mặc định production profile `prod`: `false`).
- `APP_INIT_ADMIN_PASSWORD`: Mật khẩu khởi tạo tài khoản admin (Mặc định dev: `admin`).
- `APP_INIT_USER_PASSWORD`: Mật khẩu khởi tạo tài khoản user (Mặc định dev: `user`).

> [!IMPORTANT]
> - **Mặc định tắt trên Production**: Trên môi trường Production (`prod`), tính năng seed accounts mặc định bị tắt hoàn toàn (`APP_INIT_ENABLED=false`).
> - **Thao tác Bootstrap tường minh**: Việc bật seed accounts trên Production (`APP_INIT_ENABLED=true`) chỉ phục vụ mục đích khởi tạo ban đầu (bootstrap). Khi thực hiện bootstrap, quản trị viên **bắt buộc phải cung cấp mật khẩu mạnh, ngẫu nhiên và duy nhất** qua biến môi trường.
> - **Cơ chế Fail-safe chống tài khoản mặc định**: Nếu bật seed trên Production mà mật khẩu admin hoặc user bị để trống hoặc giữ giá trị mặc định (`"admin"` / `"user"`), ứng dụng sẽ ném ngoại lệ `IllegalStateException` và từ chối khởi động ngay lập tức (fail-fast).
> - **Bảo vệ tài khoản hiện có**: Hệ thống kiểm tra sự tồn tại của tài khoản (`findByUsername(...).isEmpty()`) trước khi tạo, tuyệt đối **không ghi đè** tài khoản đã tồn tại trong CSDL.

### Yêu cầu bắt buộc về Khóa ký JWT (JWT Signing Key)
- `JWT_SIGNER_KEY`: Khóa bí mật dùng cho thuật toán ký HMAC-SHA256 (yêu cầu tối thiểu 256-bit / 32 bytes UTF-8).
- **Đồng bộ mã hóa UTF-8**: Toàn bộ quá trình ký token (`MACSigner`), kiểm tra token (`MACVerifier` trong `/auth/introspect`) và giải mã token xác thực tài nguyên (`NimbusJwtDecoder`) đều được đồng bộ sử dụng chuẩn mã hóa ký tự `StandardCharsets.UTF_8`.
- **Cơ chế Fail-safe trong Production**:
  - Khi kích hoạt profile `prod` (mặc định trong `Dockerfile` hoặc kích hoạt qua `SPRING_PROFILES_ACTIVE=prod`), ứng dụng **bắt buộc phải có `JWT_SIGNER_KEY` hợp lệ**.
  - Nếu thiếu `JWT_SIGNER_KEY`, profile `prod` sẽ từ chối giải quyết placeholder và dừng khởi động ngay lập tức (`PlaceholderResolutionException`), không kế thừa bất kỳ giá trị fallback nào từ file cấu hình mặc định.
  - `@PostConstruct validateJwtSignerKey()` kiểm tra độ dài khóa theo UTF-8 byte length (tối thiểu 32 bytes). Nếu khóa không đạt, ứng dụng sẽ ném `IllegalStateException` an toàn mà không in hay để lộ chuỗi bí mật ra log/exception.
  - **Lưu ý về Entropy và Tính ngẫu nhiên**: Độ dài >= 32 bytes chỉ là điều kiện cần về mặt thuật toán, không tự đảm bảo tính ngẫu nhiên hay độ hỗn loạn (entropy). Trên môi trường thật, khóa ký bắt buộc phải được sinh bằng trình sinh số ngẫu nhiên mạnh về mật mã (CSPRNG, ví dụ `openssl rand -base64 32` hoặc `openssl rand -hex 32`).
  - **Cảnh báo về Khóa lịch sử**: Chuỗi khóa JWT fallback trong `application.properties` chỉ phục vụ mục đích tương thích môi trường local dev của nhóm và đã nằm trong lịch sử Git. **Tuyệt đối không sử dụng khóa lịch sử này trên Production**; nếu khóa này từng được sử dụng ngoài môi trường local, phải thực hiện thu hồi và đổi khóa (rotate) ngay lập tức. Không commit khóa bí mật vào Git mà phải truyền qua cơ chế quản lý secret an toàn (Docker/K8s Secrets, biến môi trường CI/CD hoặc máy chủ).

### Hướng dẫn chạy và triển khai Docker (Local vs Production)
Image `Dockerfile` thiết lập giá trị mặc định tin cậy là `ENV SPRING_PROFILES_ACTIVE=prod`, đồng thời cho phép ghi đè linh hoạt tại runtime thông qua cờ `-e SPRING_PROFILES_ACTIVE`:

- **Khởi chạy trên môi trường Production (Mặc định profile `prod`, bắt buộc cung cấp biến môi trường)**:
  ```bash
  docker run -d -p 8080:8080 \
    -e DB_URL="jdbc:mysql://<db-host>:3306/hotel_db?useSSL=true&serverTimezone=UTC" \
    -e DB_USERNAME="<prod-db-username>" \
    -e DB_PASSWORD="<prod-db-password>" \
    -e JWT_SIGNER_KEY="<chuoi-khoa-bi-mat-ngau-nhien-toi-thieu-256-bits>" \
    hotel-restapi:latest
  ```

- **Khởi chạy container thử nghiệm cục bộ (Ghi đè sang profile `default`)**:
  ```bash
  docker run -d -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=default \
    hotel-restapi:latest
  ```

