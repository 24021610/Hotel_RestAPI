# Hotel Reservation API Service

Hệ thống RESTful API Backend chuyên nghiệp phục vụ nghiệp vụ đặt phòng khách sạn. Dự án tuân thủ nghiêm ngặt mô hình kiến trúc phân tầng (Layered Architecture) và chú trọng tối ưu các thuộc tính chất lượng (Quality Attributes) của phần mềm: Tính bảo mật (Security), Tính toàn vẹn dữ liệu (Data Integrity) và Khả năng kiểm thử (Testability).

---

## 📑 Mục lục
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Bảo mật & Xác thực](#-bảo-mật--xác-thực)
- [Chức năng nghiệp vụ](#-chức-năng-nghiệp-vụ)
  - [1. Quản lý Tài khoản (Auth & Users)](#1-quản-lý-tài-khoản-auth--users)
  - [2. Quản lý Kho phòng (Rooms)](#2-quản-lý-kho-phòng-rooms)
  - [3. Nghiệp vụ Đặt phòng (Bookings)](#3-nghiệp-vụ-đặt-phòng-bookings)
- [Tài liệu API (Swagger UI)](#-tài-liệu-api-swagger-ui)
- [Cài đặt & Triển khai](#-cài-đặt--triển-khai)
- [Ma trận Kiểm thử & Bằng chứng API](#-ma-trận-kiểm-thử--bằng-chứng-api)
- [Bảng Mã Lỗi Chuẩn Hóa (`ErrorCode`)](#-bảng-mã-lỗi-chuẩn-hóa-errorcode)
- [Giới hạn Kỹ thuật & Khuyến nghị Triển khai](#-giới-hạn-kỹ-thuật--khuyến-nghị-triển-khai)

---

## 🏗️ Kiến trúc hệ thống

Dự án áp dụng mô hình **Layered Architecture** và tổ chức mã nguồn theo khía cạnh kỹ thuật (**Package by Layer**). Cấu trúc trong `com.example.hotel_restapi` gồm các package chính:

* `controller` **(API Layer)**: Tiếp nhận HTTP Request và trả về HTTP Response (`AuthenticationController`, `RoomController`, `UserController`, `BookingController`). Định tuyến luồng dữ liệu, không chứa logic nghiệp vụ.
* `service` **(Business Logic Layer)**: Xử lý toàn bộ nghiệp vụ cốt lõi (`AuthenticationService`, `RoomService`, `UserService`, `BookingService`). Hoàn toàn độc lập, không phụ thuộc vào framework Web hay chi tiết Database.
* `repository` **(Data Access Layer)**: Tương tác với cơ sở dữ liệu MySQL thông qua Spring Data JPA (`RoomRepository`, `UserRepository`, `BookingRepository`).
* `entity` & `enums`: Định nghĩa các đối tượng ánh xạ trực tiếp với bảng trong Database (`Room`, `User`, `Booking`) và các hằng số trạng thái (`Role`, `RoomType`, `BookingStatus`).
* `dto` & `mapper`: Tách biệt dữ liệu nhận vào (Request) và trả về (Response) thông qua Data Transfer Object (DTO). Sử dụng mapper (`RoomMapper`, `UserMapper`) để bảo vệ cấu trúc database khỏi client.
* `exception`: Xử lý lỗi tập trung qua `@ControllerAdvice` (`GlobalExceptionHandler`) cùng mã lỗi chuẩn hóa (`ErrorCode`, `AppException`), đảm bảo format phản hồi đồng nhất và không lộ thông tin nhạy cảm.
* `configuration`: Chứa cấu hình bảo mật (Spring Security, JWT, CORS, CSRF) và khởi tạo các Bean hệ thống (`SecurityConfiguration`, `InitConfiguration`, `JwtAuthenticationEntryPoint`).

---

## 🔒 Bảo mật & Xác thực

* **Cơ chế**: Sử dụng JSON Web Token (JWT) với thuật toán HMAC-SHA256 (`HS256`) cho Authentication và Authorization.
* **Bảo vệ khóa bí mật (Signing Key)**: Sử dụng cấu hình Spring `@Value("${jwt.signerKey}")`, hỗ trợ nạp linh hoạt từ biến môi trường `JWT_SIGNER_KEY`. Toàn bộ quá trình ký và xác thực token được chuẩn hóa mã hóa ký tự `StandardCharsets.UTF_8` với độ dài tối thiểu 32 bytes.
* **Xử lý tập trung**: Kiểm tra tính hợp lệ của Token và quyền hạn người dùng tại tầng Filter của Spring Security.
* **Phân quyền dựa trên Annotation**: Sử dụng `@PreAuthorize` và `@PostAuthorize` để bảo vệ tài nguyên chi tiết:
  - Khách hàng thường (`USER`) chỉ có quyền xem/sửa thông tin của chính mình và quản lý các đơn đặt phòng của bản thân.
  - Quản trị viên (`ADMIN`) có toàn quyền xem/quản lý danh sách người dùng, CRUD kho phòng, xem và hủy mọi đơn đặt phòng trong hệ thống.
  - Ngăn chặn hoàn toàn lỗ hổng leo thang đặc quyền (Privilege Escalation) khi tạo hoặc cập nhật tài khoản.
* **Bảo vệ tính toàn vẹn dữ liệu**: Ràng buộc duy nhất `unique = true` cho username kết hợp xử lý `DataIntegrityViolationException` tại tầng ControllerAdvice, ngăn chặn triệt để hiện tượng race condition khi đăng ký tài khoản trùng lặp.

---

## 🚀 Chức năng nghiệp vụ

### 1. Quản lý Tài khoản (Auth & Users)
* **Đăng ký tài khoản (`POST /users`)**: Tên đăng nhập duy nhất (tối thiểu 3 ký tự), mật khẩu tối thiểu 6 ký tự. Mặc định cấp quyền `USER`, ngăn chặn việc client tự ý gán quyền `ADMIN`.
* **Đăng nhập (`POST /auth/login`)**: Xác thực qua username/password mã hóa BCrypt (cost factor 12), cấp phát Bearer Token JWT chứa thông tin định danh và quyền hạn.
* **Kiểm tra tính hợp lệ của Token (`POST /auth/introspect`)**: Kiểm tra chữ ký và hạn sử dụng của JWT, luôn trả về cấu trúc `{ valid: boolean }` an toàn.
* **Quản lý thông tin cá nhân (`GET /users/{id}`, `GET /users/username/{username}`, `PUT /users/{id}`)**: Chỉ chính chủ hoặc `ADMIN` mới có quyền truy cập và chỉnh sửa.
* **Quản trị người dùng (`GET /users`, `DELETE /users/{id}`)**: Dành riêng cho quyền `ADMIN`.

### 2. Quản lý Kho phòng (Rooms)
* **Tra cứu danh sách phòng (`GET /rooms`)**: Công khai hoặc cho người dùng đã đăng nhập xem danh sách tất cả các phòng.
* **Chi tiết phòng (`GET /rooms/{id}`, `GET /rooms/number/{number}`)**: Tìm kiếm phòng theo định danh hoặc số hiệu phòng.
* **Quản trị kho phòng (`POST /rooms`, `PUT /rooms/{id}`, `PATCH /rooms/{id}`, `DELETE /rooms/{id}`)**:
  - Yêu cầu quyền `ADMIN`.
  - Kiểm tra tính hợp lệ của số phòng, loại phòng (`SINGLE`, `DOUBLE`, `SUITE`, `DELUXE`) và giá phòng > 0.
  - **Quy tắc an toàn**: Không cho phép xóa phòng khi phòng đang có khách lưu trú (`occupied == true`), trả về mã lỗi `ROOM_CANNOT_DELETE_OCCUPIED (1022)`.

### 3. Nghiệp vụ Đặt phòng (Bookings)
* **Tạo đơn đặt phòng (`POST /bookings`)**: Tự động nhận diện thông tin khách hàng từ JWT qua `Authentication.getName()`.
* **Tính toán chi phí tự động**: Tự động tính số đêm lưu trú (`ChronoUnit.DAYS.between`) và tổng tiền dựa trên `room.price * nights`.
* **Chống chồng chéo lịch đặt (Conflict Prevention)**: Kiểm tra thông qua JPQL query `existsOverlappingActiveBooking`, ngăn chặn tuyệt đối tình trạng đặt trùng phòng trong cùng một khoảng thời gian (`checkIn < newCheckOut AND checkOut > newCheckIn`).
* **Tra cứu đơn đặt phòng (`GET /bookings`, `GET /bookings/{id}`)**:
  - Khách hàng thường: chỉ xem được các đơn đặt phòng của chính mình.
  - Quản trị viên (`ADMIN`): xem được toàn bộ danh sách đặt phòng trong toàn hệ thống.
* **Hủy đặt phòng (`DELETE /bookings/{id}`)**:
  - Khách hàng hủy đơn của mình hoặc Quản trị viên hủy đơn bất kỳ.
  - Chuyển trạng thái sang `CANCELLED`, ngăn chặn việc hủy lại đơn đã bị hủy trước đó.

---

## 📖 Tài liệu API (Swagger UI)

Dự án tích hợp sẵn thư viện `springdoc-openapi` để tự động tạo tài liệu tương tác API trực quan:

* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) *(khi chạy local)*
* **OpenAPI Spec (JSON)**: `http://localhost:8080/v3/api-docs`

---

## ⚙️ Cài đặt & Triển khai

### 1. Chạy ứng dụng nội bộ (Local Development)

**Yêu cầu:** JDK 17+, MySQL Server chạy tại cổng 3307.

```powershell
# Chạy ứng dụng thông qua Maven Wrapper
.\mvnw.cmd spring-boot:run
```

Cấu hình mặc định có thể override linh hoạt qua biến môi trường:
- `DB_URL`: JDBC URL kết nối MySQL (Mặc định: `jdbc:mysql://localhost:3307/hotel_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true`)
- `DB_USERNAME`: Tên đăng nhập database (Mặc định: `root`)
- `DB_PASSWORD`: Mật khẩu database (Mặc định fallback: `quangto123` hoặc `root`)
- `JWT_SIGNER_KEY`: Khóa bí mật JWT (Mặc định sẵn có cho dev)

### 2. Triển khai Production Profile

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```
*Lưu ý:* Khi chạy profile `prod`, hệ thống sẽ kích hoạt cơ chế `fail-fast`: bắt buộc phải cung cấp `JWT_SIGNER_KEY` (tối thiểu 32 ký tự) và mật khẩu quản trị viên riêng thông qua biến môi trường.

### 3. Đóng gói Container với Docker

Hệ thống đã chuẩn bị sẵn `Dockerfile` nhiều giai đoạn (multi-stage build) trên nền `eclipse-temurin:17-jre-alpine`:

```bash
docker build -t hotel-restapi:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3307/hotel_db?useSSL=false \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_secure_password \
  -e JWT_SIGNER_KEY=your_at_least_32_bytes_super_secret_jwt_key \
  hotel-restapi:latest
```

---

## 🧪 Ma trận Kiểm thử & Bằng chứng API

Dự án được bảo vệ bởi bộ kiểm thử tự động toàn diện:

### 1. Automated Test Suite (17 Tests PASS)
Chạy bộ kiểm thử tự động:
```powershell
.\mvnw.cmd test
```
- [`ConfigurationSecurityTest`](src/test/java/com/example/hotel_restapi/ConfigurationSecurityTest.java): **16/16 Unit Tests PASS**
  - Kiểm tra tính bảo mật cấu hình, tính hợp lệ của signing key (tối thiểu 32 bytes UTF-8).
  - Kiểm tra fail-fast khi thiếu key trên production profile.
  - Kiểm tra an toàn khởi tạo tài khoản (`InitConfiguration`), không ghi đè tài khoản sẵn có.
- [`HotelRestApiApplicationTests`](src/test/java/com/example/hotel_restapi/HotelRestApiApplicationTests.java): **1/1 Integration Test PASS**
  - Khởi tạo toàn bộ Spring ApplicationContext, HikariCP pool và Hibernate ORM.

### 2. Ma trận Xác minh API Thực tế

| Nhóm API | Endpoint | Method | Quyền yêu cầu | Kết quả thực tế |
| :--- | :--- | :---: | :---: | :--- |
| **Auth** | `/auth/login` | POST | Public | `200 OK`, cấp Bearer JWT token |
| **Auth** | `/auth/introspect` | POST | Public | `200 OK`, trả về `{ "result": { "valid": true/false } }` |
| **User** | `/users` | POST | Public | `200 OK` (Đăng ký mới role USER), `400 Bad Request` khi trùng username |
| **User** | `/users` | GET | `ADMIN` | `200 OK` (Admin xem danh sách), `403 Forbidden` (User bị từ chối) |
| **User** | `/users/{id}` | GET | `ADMIN` / Self | `200 OK` (Đúng người sở hữu), `403 Forbidden` (Xem trộm tài khoản khác) |
| **Room** | `/rooms` | GET | Public/Auth | `200 OK`, trả về danh sách phòng |
| **Room** | `/rooms` | POST/PUT | `ADMIN` | `200 OK` / `201 Created`, từ chối quyền với User thường |
| **Room** | `/rooms/{id}` | DELETE | `ADMIN` | Chặn xóa phòng `occupied = true` với mã `1022` |
| **Booking** | `/bookings` | POST | User/Admin | `201 Created`, tính tiền đúng số đêm, chặn trùng lịch với mã `1019` |
| **Booking** | `/bookings` | GET | User/Admin | User xem đơn của mình, Admin xem tất cả đơn đặt phòng |
| **Booking** | `/bookings/{id}` | DELETE | User/Admin | User hủy đơn của mình, Admin hủy đơn bất kỳ, chặn hủy lại với mã `1021` |

---

## 📋 Bảng Mã Lỗi Chuẩn Hóa (`ErrorCode`)

Toàn bộ các mã lỗi trong hệ thống được định danh duy nhất và phân loại rõ ràng:

| Mã số (`code`) | Tên Hằng Enum | HTTP Status tương ứng | Ý nghĩa & Trường hợp phát sinh |
| :---: | :--- | :--- | :--- |
| `1001` | `INVALID_KEY` | `400 BAD_REQUEST` | Khóa validation không xác định, JSON sai cú pháp hoặc enum không hợp lệ |
| `1002` | `USER_EXISTED` | `400 BAD_REQUEST` / `409 CONFLICT` | Tên người dùng đã tồn tại trong hệ thống |
| `1003` | `USERNAME_INVALID` | `400 BAD_REQUEST` | Tên đăng nhập không đạt yêu cầu độ dài tối thiểu |
| `1004` | `INVALID_PASSWORD` | `400 BAD_REQUEST` | Mật khẩu không đạt yêu cầu độ dài (tối thiểu 6 ký tự) |
| `1005` | `USER_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy tài khoản người dùng tương ứng |
| `1006` | `UNAUTHENTICATED` | `401 UNAUTHORIZED` | Yêu cầu chưa được xác thực hoặc token không hợp lệ |
| `1007` | `ROOM_EXISTED` | `400 BAD_REQUEST` / `409 CONFLICT` | Số phòng đã tồn tại trong CSDL |
| `1008` | `ROOM_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy phòng theo ID hoặc số phòng |
| `1009` | `ROOM_UPDATE_EMPTY` | `400 BAD_REQUEST` | Yêu cầu cập nhật phòng nhưng không truyền trường nào |
| `1010` | `ROOM_NUMBER_REQUIRED` | `400 BAD_REQUEST` | Số phòng bị để trống khi tạo mới |
| `1011` | `ROOM_NUMBER_TOO_LONG` | `400 BAD_REQUEST` | Số phòng vượt quá độ dài tối đa 20 ký tự |
| `1012` | `ROOM_CAPACITY_INVALID` | `400 BAD_REQUEST` | Số lượng khách của phòng nhỏ hơn 1 |
| `1013` | `ROOM_TYPE_REQUIRED` | `400 BAD_REQUEST` | Loại phòng bị để trống hoặc sai kiểu |
| `1014` | `ROOM_PRICE_INVALID` | `400 BAD_REQUEST` | Giá phòng nhỏ hơn hoặc bằng 0 |
| `1015` | `BOOKING_ROOM_REQUIRED` | `400 BAD_REQUEST` | Thông tin phòng bắt buộc khi tạo đơn đặt phòng |
| `1016` | `BOOKING_CHECK_IN_REQUIRED` | `400 BAD_REQUEST` | Ngày check-in bắt buộc khi đặt phòng |
| `1017` | `BOOKING_CHECK_OUT_REQUIRED` | `400 BAD_REQUEST` | Ngày check-out bắt buộc khi đặt phòng |
| `1018` | `BOOKING_DATE_INVALID` | `400 BAD_REQUEST` | Ngày check-out phải sau ngày check-in |
| `1019` | `BOOKING_ROOM_UNAVAILABLE` | `409 CONFLICT` | Phòng đã có người đặt trong khoảng thời gian yêu cầu |
| `1020` | `BOOKING_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy đơn đặt phòng |
| `1021` | `BOOKING_ALREADY_CANCELLED` | `409 CONFLICT` | Đơn đặt phòng đã bị hủy trước đó |
| `1022` | `ROOM_CANNOT_DELETE_OCCUPIED` | `400 BAD_REQUEST` | Từ chối xóa phòng khi phòng đang có khách lưu trú |
| `1023` | `UNAUTHORIZED` | `403 FORBIDDEN` | Đã đăng nhập nhưng không đủ quyền hạn thực hiện hành động |
| `9999` | `UNCATEGORIZED_EXCEPTION` | `500 INTERNAL_SERVER_ERROR` | Lỗi hệ thống chưa được phân loại cụ thể |

---

## ⚠️ Giới hạn Kỹ thuật & Khuyến nghị Triển khai

1. **Ràng buộc duy nhất trên Database**:
   - Entity `User` đã khai báo `@Column(unique = true)` và mã nguồn có sẵn file script di chuyển schema [manual_migration_uk_user_username.sql](scripts/migrations/manual_migration_uk_user_username.sql).
   - Khi triển khai lên cụm máy chủ chính thức, DBA cần chạy script kiểm tra điều kiện tiên quyết và thêm index `uk_user_username` vào CSDL thật.
2. **Quản lý Secrets trên Production**:
   - Khi đưa vào vận hành thực tế, tuyệt đối không sử dụng mật khẩu và signing key mặc định trong `application.properties`. Hãy khai báo giá trị an toàn thông qua biến môi trường hệ thống hoặc Docker Secrets.
