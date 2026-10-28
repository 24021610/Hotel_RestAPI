# Hotel Reservation API Service

Hệ thống RESTful API Backend phục vụ nghiệp vụ quản lý khách sạn và đặt phòng, được xây dựng trên nền tảng Spring Boot 4 và Spring Security. Dự án áp dụng mô hình kiến trúc phân tầng (Layered Architecture) với trọng tâm là tính bảo mật (Security), tính toàn vẹn dữ liệu (Data Integrity) và cấu hình linh hoạt theo môi trường.

---

## 📑 Mục lục
- [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
- [Bảo mật & Xác thực](#-bảo-mật--xác-thực)
- [Chức năng nghiệp vụ thực tế](#-chức-năng-nghiệp-vụ-thực-tế)
  - [1. Quản lý Tài khoản (Auth & Users)](#1-quản-lý-tài-khoản-auth--users)
  - [2. Quản lý Kho phòng (Rooms)](#2-quản-lý-kho-phòng-rooms)
  - [3. Nghiệp vụ Đặt phòng (Bookings)](#3-nghiệp-vụ-đặt-phòng-bookings)
- [Cài đặt & Triển khai](#-cài-đặt--triển-khai)
- [Kiểm thử & Bằng chứng Xác minh](#-kiểm-thử--bằng-chứng-xác-minh)
  - [1. Bộ Kiểm thử Tự động (17 Automated Tests)](#1-bộ-kiểm-thử-tự-động-17-automated-tests)
  - [2. Bảng Xác minh API Thủ công (Manual API Verification)](#2-bảng-xác-minh-api-thủ-công-manual-api-verification)
- [Bảng Mã Lỗi Chuẩn Hóa (`ErrorCode`)](#-bảng-mã-lỗi-chuẩn-hóa-errorcode)
- [Giới hạn Kỹ thuật & Khuyến nghị Vận hành](#-giới-hạn-kỹ-thuật--khuyến-nghị-vận-hành)

---

## 🏗️ Kiến trúc hệ thống

Dự án tổ chức mã nguồn theo mô hình kiến trúc phân tầng (**Layered Architecture / Package by Layer**) trong package `com.example.hotel_restapi`:

* `controller` **(API Layer)**: Tiếp nhận HTTP Request, điều phối dữ liệu qua DTO và trả về HTTP Response chuẩn `ApiResponse<T>`. Gồm `AuthenticationController`, `UserController`, `RoomController`, `BookingController`.
* `service` **(Business Logic Layer)**: Thực thi logic nghiệp vụ độc lập với giao thức truyền tải (`AuthenticationService`, `UserService`, `RoomService`, `BookingService`).
* `repository` **(Data Access Layer)**: Giao tiếp với cơ sở dữ liệu MySQL qua Spring Data JPA (`UserRepository`, `RoomRepository`, `BookingRepository`).
* `entity` & `enums`: Các thực thể ánh xạ JPA (`User`, `Room`, `Booking`) và các enum trạng thái (`Role`, `RoomType`, `BookingStatus`).
* `dto` & `mapper`: Tách biệt dữ liệu nhận vào (Request) và trả về (Response) thông qua MapStruct (`UserMapper`, `RoomMapper`).
* `exception`: Xử lý ngoại lệ tập trung qua `@ControllerAdvice` (`GlobalExceptionHandler`) với mã lỗi chuẩn hóa (`ErrorCode`, `AppException`), ngăn chặn rò rỉ stack trace hoặc thông tin nội bộ.
* `configuration`: Chứa cấu hình bảo mật Spring Security (`SecurityConfiguration`), khởi tạo tài khoản hệ thống (`InitConfiguration`) và xử lý lỗi xác thực JWT (`JwtAuthenticationEntryPoint`).

---

## 🔒 Bảo mật & Xác thực

* **Cơ chế xác thực**: Sử dụng JSON Web Token (JWT) với thuật toán HMAC-SHA256 (`HS256`).
* **Quản lý khóa ký (Signing Key)**: Khóa ký được nạp thông qua `@Value("${jwt.signerKey}")`, hỗ trợ cấu hình qua biến môi trường `JWT_SIGNER_KEY`. Toàn bộ quá trình ký và giải mã token được ép kiểu tường minh `StandardCharsets.UTF_8` với kiểm tra độ dài tối thiểu 32 bytes (256 bits).
* **Kiểm soát phân quyền (RBAC)**:
  - Phân quyền khai báo tập trung tại Spring Security Filter Chain: chỉ mở công khai `POST /users`, `POST /auth/login` và `POST /auth/introspect`. **Tất cả các endpoint còn lại (kể cả tra cứu phòng) đều yêu cầu Bearer Token hợp lệ.**
  - Phân quyền chi tiết tại tầng Service qua `@PreAuthorize` và `@PostAuthorize`:
    - Khách hàng (`USER`): Chỉ xem và chỉnh sửa thông tin cá nhân của chính mình; chỉ xem và hủy đơn đặt phòng của bản thân.
    - Quản trị viên (`ADMIN`): Có quyền xem toàn bộ danh sách tài khoản, thực hiện CRUD kho phòng, xem và hủy mọi đơn đặt phòng trong hệ thống.
    - Chặn hoàn toàn lỗ hổng leo thang đặc quyền (Privilege Escalation): Khách hàng thường không thể tự gán quyền `ADMIN` khi đăng ký hoặc cập nhật hồ sơ.
* **Toàn vẹn dữ liệu tài khoản**: Entity `User` áp dụng ràng buộc duy nhất trên cột `username` kết hợp với bộ lọc ngoại lệ `DataIntegrityViolationException` tại `GlobalExceptionHandler` (trả về HTTP `409 Conflict`), ngăn chặn triệt để hiện tượng race condition khi đăng ký tài khoản trùng tên.

---

## 🚀 Chức năng nghiệp vụ thực tế

### 1. Quản lý Tài khoản (Auth & Users)
* **Đăng nhập (`POST /auth/login`)**: Xác thực tài khoản qua username/password mã hóa BCrypt, trả về JWT Token chứa danh sách roles.
* **Kiểm tra tính hợp lệ của Token (`POST /auth/introspect`)**: Kiểm tra tính hợp lệ và thời hạn của token, luôn phản hồi HTTP 200 kèm DTO `{ "valid": true/false }` an toàn.
* **Đăng ký tài khoản (`POST /users`)**: Cho phép người dùng mới đăng ký (username tối thiểu 3 ký tự, password tối thiểu 6 ký tự). Hệ thống mặc định chỉ cấp quyền `USER`.
* **Tra cứu danh sách tài khoản (`GET /users`)**: Yêu cầu quyền `ADMIN`.
* **Chi tiết tài khoản theo ID (`GET /users/{userId}`)**: Yêu cầu quyền `ADMIN` hoặc chính chủ tài khoản.
* **Chi tiết tài khoản theo username (`GET /users/by-username/{username}`)**: Yêu cầu quyền `ADMIN` hoặc chính chủ tài khoản.
* **Cập nhật thông tin tài khoản (`PUT /users/{userId}`)**: Yêu cầu quyền `ADMIN` hoặc chính chủ tài khoản. Người dùng thường không thể tự thay đổi vai trò (`roles`).
* **Xóa tài khoản (`DELETE /users/{userId}`)**: Yêu cầu quyền `ADMIN`.

### 2. Quản lý Kho phòng (Rooms)
* **Xem toàn bộ danh sách phòng (`GET /rooms`)**: Yêu cầu người dùng đã đăng nhập (Bearer Token).
* **Xem chi tiết phòng theo ID (`GET /rooms/{id}`)**: Yêu cầu người dùng đã đăng nhập.
* **Tìm phòng theo số phòng (`GET /rooms/number/{number}`)**: Yêu cầu người dùng đã đăng nhập.
* **Danh sách phòng chưa có khách (`GET /rooms/available`)**: Lọc các phòng có trạng thái `occupied == false` trong bảng `rooms`. Yêu cầu đã đăng nhập.
* **Danh sách phòng đang có khách (`GET /rooms/occupied`)**: Lọc các phòng có trạng thái `occupied == true` trong bảng `rooms`. Yêu cầu đã đăng nhập.
* **Tạo phòng mới (`POST /rooms`)**: Yêu cầu quyền `ADMIN`. Ràng buộc: số phòng không trống, tối đa 20 ký tự, sức chứa >= 1, loại phòng hợp lệ (`SINGLE`, `DOUBLE`, `SUITE`, `DELUXE`), giá phòng > 0.
* **Cập nhật thông tin phòng (`PATCH /rooms/{id}`)**: Yêu cầu quyền `ADMIN`. Cập nhật linh hoạt từng trường thông tin phòng.
* **Xóa phòng (`DELETE /rooms/{id}`)**: Yêu cầu quyền `ADMIN`.
  - **Quy tắc an toàn nghiệp vụ**: Hệ thống từ chối xóa nếu phòng đang ở trạng thái có khách (`occupied == true`), trả về lỗi HTTP 400 kèm mã `ROOM_CANNOT_DELETE_OCCUPIED (1022)`.

> [!NOTE]
> Hệ thống hiện **chưa hỗ trợ** tìm kiếm phòng trống theo khoảng thời gian tùy chọn (`checkIn` - `checkOut`) tại API Room. Endpoint `GET /rooms/available` hiện tại chỉ lọc dựa trên cờ boolean `occupied` của phòng.

### 3. Nghiệp vụ Đặt phòng (Bookings)
* **Tạo đơn đặt phòng (`POST /bookings`)**:
  - Yêu cầu người dùng đã đăng nhập. Định danh khách hàng được trích xuất an toàn từ JWT Token thông qua `Authentication.getName()`.
  - Ràng buộc ngày: `checkOut` phải sau ngày `checkIn`.
  - **Chống trùng lịch đặt (Conflict Prevention)**: Sử dụng JPQL query `existsOverlappingActiveBooking` để kiểm tra các đơn đặt phòng có trạng thái `CONFIRMED`. Nếu trùng khoảng ngày (`checkIn < newCheckOut AND checkOut > newCheckIn`), hệ thống trả về HTTP 409 với mã `BOOKING_ROOM_UNAVAILABLE (1019)`.
  - Tự động tính toán tổng chi phí: `totalCost = room.price * số_đêm_lưu_trú`.
  - Đơn đặt mới được lưu ở trạng thái `CONFIRMED`.
* **Xem danh sách đơn đặt phòng (`GET /bookings`)**:
  - Khách hàng (`USER`): Chỉ xem danh sách các đơn đặt phòng của chính mình.
  - Quản trị viên (`ADMIN`): Xem toàn bộ danh sách đơn đặt phòng trong toàn hệ thống.
* **Xem chi tiết đơn đặt phòng (`GET /bookings/{id}`)**:
  - Khách hàng thường: Chỉ xem được đơn đặt phòng nếu thuộc sở hữu của mình.
  - Quản trị viên: Có quyền xem bất kỳ đơn đặt phòng nào theo ID.
* **Hủy đơn đặt phòng (`DELETE /bookings/{id}`)**:
  - Khách hàng tự hủy đơn của mình hoặc Quản trị viên hủy đơn bất kỳ.
  - Đơn đặt phòng được chuyển trạng thái sang `CANCELLED`.
  - Hệ thống từ chối nếu đơn đặt phòng đã ở trạng thái `CANCELLED` trước đó, trả về HTTP 409 với mã `BOOKING_ALREADY_CANCELLED (1021)`.

> [!NOTE]
> Hệ thống hiện **chưa có** các endpoint phục vụ quy trình Check-in thực tế tại quầy, quy trình Check-out hay cổng thanh toán (Payment Gateway). Trạng thái đơn đặt phòng hiện chỉ gồm hai giá trị: `CONFIRMED` và `CANCELLED`.

---

## ⚙️ Cài đặt & Triển khai

### 1. Cấu hình Môi trường Phát triển (Local Dev Profile)

Môi trường local mặc định sử dụng tệp `src/main/resources/application.properties`:
- **Cổng ứng dụng**: `8080`
- **CSDL MySQL**: Cổng `3307`, cơ sở dữ liệu `hotel_db`
- **Cơ chế DDL**: `spring.jpa.hibernate.ddl-auto=update`

**Khởi chạy trên Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Ghi đè cấu hình an toàn qua biến môi trường:**
Bạn có thể ghi đè bất kỳ cấu hình nào mà không cần sửa đổi mã nguồn:
```powershell
$env:DB_URL="jdbc:mysql://localhost:3307/hotel_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="<your_database_password>"
$env:JWT_SIGNER_KEY="<your_at_least_32_bytes_secret_key>"
.\mvnw.cmd spring-boot:run
```

### 2. Triển khai Môi trường Production (`prod` Profile)

Khi chạy với profile `prod` (`src/main/resources/application-prod.properties`):
- `spring.jpa.hibernate.ddl-auto=none` (Không tự động can thiệp schema CSDL).
- `app.init.enabled=false` (Mặc định tắt tự động tạo tài khoản hạt giống).
- **Cơ chế Fail-fast**: Bắt buộc phải cung cấp các biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `JWT_SIGNER_KEY` (tối thiểu 32 ký tự UTF-8). Ứng dụng sẽ dừng khởi động ngay nếu thiếu các giá trị này.

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_URL="jdbc:mysql://production-host:3306/hotel_db?useSSL=true"
$env:DB_USERNAME="prod_user"
$env:DB_PASSWORD="<strong_production_password>"
$env:JWT_SIGNER_KEY="<secure_random_key_with_at_least_32_characters_here>"
.\mvnw.cmd spring-boot:run
```

### 3. Đóng gói Container với Docker

Dự án cung cấp [`Dockerfile`](Dockerfile) đa tầng (multi-stage build) sử dụng `maven:3.9-eclipse-temurin-17` để đóng gói và chạy trên nền `eclipse-temurin:17-jre-alpine` nhẹ gọn. Image được cấu hình mặc định chạy profile `prod`:

```bash
# 1. Build Docker image
docker build -t hotel-restapi:latest .

# 2. Khởi chạy container với biến môi trường bắt buộc
docker run -d -p 8080:8080 --name hotel-api \
  -e DB_URL="jdbc:mysql://host.docker.internal:3307/hotel_db?useSSL=false" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="<your_database_password>" \
  -e JWT_SIGNER_KEY="<your_at_least_32_bytes_secret_key>" \
  hotel-restapi:latest
```

> [!NOTE]
> Repository hiện tại chỉ cung cấp `Dockerfile`. Tệp `docker-compose.yml` không nằm trong mã nguồn và cần được quản trị viên tự cấu hình tương thích với hạ tầng cơ sở dữ liệu của mình.

---

## 🧪 Kiểm thử & Bằng chứng Xác minh

### 1. Bộ Kiểm thử Tự động (17 Automated Tests)

Bộ kiểm thử tự động được thực thi thông qua lệnh:
```powershell
.\mvnw.cmd test
```

**Kết quả thực tế**: **`BUILD SUCCESS`** — **17 / 17 Tests PASS (0 Failures, 0 Errors, 0 Skipped)**.

* **Kiểm thử Đơn vị Cấu hình & Bảo mật** ([`ConfigurationSecurityTest.java`](src/test/java/com/example/hotel_restapi/ConfigurationSecurityTest.java)): **16 tests PASS**
  - `testInitConfiguration_whenDisabled_doesNotCreateAccounts`: Xác minh tắt seed account khi `app.init.enabled=false`.
  - `testInitConfiguration_underDefaultProfile_seedsDefaultAccounts`: Xác minh seed account mặc định trên local profile.
  - `testInitConfiguration_underProdProfile_failsWhenAdminPasswordDefault / Blank`: Xác minh fail-fast trên profile `prod` nếu mật khẩu admin để trống hoặc để mặc định.
  - `testInitConfiguration_underProdProfile_failsWhenUserPasswordDefault / Blank`: Xác minh fail-fast trên profile `prod` nếu mật khẩu user để trống hoặc để mặc định.
  - `testInitConfiguration_underProdProfile_succeedsWithCustomPasswords`: Xác minh seed thành công trên profile `prod` khi cung cấp mật khẩu hợp lệ.
  - `testInitConfiguration_whenAccountsAlreadyExist_doesNotOverwrite`: Đảm bảo không ghi đè tài khoản đã tồn tại trong CSDL.
  - `testSecurityConfiguration_underProdProfile_failsWhenKeyNull / Blank / LessThan32Bytes`: Đảm bảo dừng khởi động nếu khóa JWT thiếu hoặc ngắn hơn 32 bytes UTF-8.
  - `testSecurityConfiguration_underProdProfile_passesWithValidKey`: Đảm bảo khởi động thành công khi khóa JWT >= 32 bytes UTF-8.
  - `testSecurityConfiguration_underDefaultProfile_allowsAnyKey`: Đảm bảo môi trường dev local chấp nhận khóa ký mặc định.
  - `testSecurityConfiguration_exceptionMessageNeverRevealsSignerKey`: Kiểm chứng thông báo lỗi không làm lộ chuỗi khóa bí mật.
  - `testProdProperties_requiresExplicitJwtSignerKeyWithoutFallback`: Kiểm tra file `application-prod.properties` không chứa fallback mặc định.
  - `testDefaultProperties_containsDefaultFallbacksForLocalDev`: Kiểm tra file `application.properties` có sẵn fallback cho dev local.
* **Kiểm thử Tích hợp Context** ([`HotelRestApiApplicationTests.java`](src/test/java/com/example/hotel_restapi/HotelRestApiApplicationTests.java)): **1 test PASS**
  - Khởi tạo toàn bộ Spring ApplicationContext, HikariCP connection pool và xác thực Hibernate ORM mapping cho cả 3 entity (`User`, `Room`, `Booking`).

> [!IMPORTANT]
> **Phạm vi & Giới hạn của Bộ Kiểm thử Tự động:**
> 17 kiểm thử tự động trên tập trung bảo vệ tầng Cấu hình, Bảo mật Khởi tạo và Tính toàn vẹn JPA Context. Hiện tại **chưa có** test tự động kiểu MockMvc cho Controller hoặc Service của các module `User`, `Room`, `Booking` và `Auth`. Các hành vi API dưới đây được kiểm tra độc lập bằng Postman / cURL.

---

### 2. Bảng Xác minh API Thủ công (Manual API Verification)

Bảng dưới đây tổng hợp các hành vi API đã được kiểm chứng độc lập thông qua Postman / cURL trên môi trường chạy thực tế:

| Phân hệ | Endpoint | Method | Quyền yêu cầu | Trạng thái HTTP | Ghi chú & Hành vi quan sát được |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Auth** | `/auth/login` | POST | Public | `200 OK` | Cấp Bearer JWT hợp lệ khi gửi đúng tài khoản/mật khẩu |
| **Auth** | `/auth/login` | POST | Public | `400 Bad Request` | Trả về mã `1005 (USER_NOT_EXISTED)` khi tài khoản không tồn tại |
| **Auth** | `/auth/introspect` | POST | Public | `200 OK` | Luôn trả về `{ "result": { "valid": true/false } }`, không ném 401 |
| **User** | `/users` | POST | Public | `200 OK` | Đăng ký thành công, gán role `USER`, bỏ qua role từ request |
| **User** | `/users` | POST | Public | `400 Bad Request` | Trả về mã `1002 (USER_EXISTED)` khi username đã tồn tại |
| **User** | `/users` | POST | Public | `400 Bad Request` | Trả về mã `1003` khi username trống hoặc dưới 3 ký tự |
| **User** | `/users` | GET | `ADMIN` | `200 OK` | Quản trị viên xem danh sách toàn bộ người dùng |
| **User** | `/users` | GET | `USER` | `403 Forbidden` | Người dùng thường bị từ chối truy cập |
| **User** | `/users/{id}` | GET | `ADMIN` / Self | `200 OK` | Cho phép xem nếu là chính chủ hoặc có quyền ADMIN |
| **User** | `/users/{id}` | GET | User khác | `403 Forbidden` | Chặn truy cập xem thông tin tài khoản người khác (chống IDOR) |
| **User** | `/users/{id}` | PUT | `USER` | `403 Forbidden` | Từ chối nếu user cố tình truyền field `roles` để nâng quyền |
| **Room** | `/rooms` | GET | Authenticated | `200 OK` | Trả về danh sách phòng khi có Bearer Token |
| **Room** | `/rooms` | GET | Không có token | `401 Unauthorized` | Chặn truy cập khi chưa xác thực |
| **Room** | `/rooms` | POST | `ADMIN` | `200 OK` | Tạo phòng mới thành công với các tham số hợp lệ |
| **Room** | `/rooms` | POST | `USER` | `403 Forbidden` | Người dùng thường không có quyền tạo phòng |
| **Room** | `/rooms/{id}` | DELETE | `ADMIN` | `400 Bad Request` | Trả về mã `1022 (ROOM_CANNOT_DELETE_OCCUPIED)` nếu phòng đang có khách |
| **Booking** | `/bookings` | POST | Authenticated | `201 Created` | Tạo đơn đặt thành công, tự động gán username từ token |
| **Booking** | `/bookings` | POST | Authenticated | `409 Conflict` | Trả về mã `1019` khi phòng bị trùng khoảng ngày đã đặt |
| **Booking** | `/bookings` | GET | `USER` | `200 OK` | Người dùng thường chỉ nhận danh sách đơn của chính mình |
| **Booking** | `/bookings` | GET | `ADMIN` | `200 OK` | Quản trị viên xem toàn bộ danh sách đơn trong hệ thống |
| **Booking** | `/bookings/{id}` | DELETE | `USER` / `ADMIN` | `200 OK` | Hủy đơn thành công, cập nhật trạng thái `CANCELLED` |
| **Booking** | `/bookings/{id}` | DELETE | `USER` / `ADMIN` | `409 Conflict` | Trả về mã `1021` nếu đơn đã ở trạng thái hủy trước đó |

---

## 📋 Bảng Mã Lỗi Chuẩn Hóa (`ErrorCode`)

Dự án định nghĩa tập trung toàn bộ mã lỗi trong enum [`ErrorCode.java`](src/main/java/com/example/hotel_restapi/exception/ErrorCode.java), bảo đảm mỗi mã số là duy nhất:

| Mã số (`code`) | Tên Hằng Enum | HTTP Status tương ứng | Ý nghĩa & Trường hợp phát sinh |
| :---: | :--- | :--- | :--- |
| `1001` | `INVALID_KEY` | `400 BAD_REQUEST` | Khóa validation không xác định, JSON sai cú pháp hoặc enum không hợp lệ |
| `1002` | `USER_EXISTED` | `400 BAD_REQUEST` / `409 CONFLICT` | Tên người dùng đã tồn tại trong hệ thống (409 khi vi phạm index CSDL) |
| `1003` | `USERNAME_INVALID` | `400 BAD_REQUEST` | Tên đăng nhập không đạt độ dài tối thiểu (tối thiểu 3 ký tự) |
| `1004` | `INVALID_PASSWORD` | `400 BAD_REQUEST` | Mật khẩu không đạt độ dài quy định (từ 6 đến 20 ký tự) |
| `1005` | `USER_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy tài khoản người dùng tương ứng |
| `1006` | `UNAUTHENTICATED` | `401 UNAUTHORIZED` | Yêu cầu chưa xác thực, thiếu token hoặc token sai/hết hạn |
| `1007` | `ROOM_EXISTED` | `400 BAD_REQUEST` / `409 CONFLICT` | Số phòng đã tồn tại trong hệ thống khi tạo mới |
| `1008` | `ROOM_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy phòng theo ID hoặc số phòng yêu cầu |
| `1009` | `ROOM_UPDATE_EMPTY` | `400 BAD_REQUEST` | Yêu cầu cập nhật phòng nhưng không truyền bất kỳ trường nào |
| `1010` | `ROOM_NUMBER_REQUIRED` | `400 BAD_REQUEST` | Số phòng bị để trống khi tạo mới phòng |
| `1011` | `ROOM_NUMBER_TOO_LONG` | `400 BAD_REQUEST` | Số phòng vượt quá độ dài quy định (tối đa 20 ký tự) |
| `1012` | `ROOM_CAPACITY_INVALID` | `400 BAD_REQUEST` | Sức chứa số lượng khách của phòng nhỏ hơn 1 |
| `1013` | `ROOM_TYPE_REQUIRED` | `400 BAD_REQUEST` | Loại phòng bị để trống hoặc sai kiểu dữ liệu enum |
| `1014` | `ROOM_PRICE_INVALID` | `400 BAD_REQUEST` | Giá phòng nhỏ hơn hoặc bằng 0 |
| `1015` | `BOOKING_ROOM_REQUIRED` | `400 BAD_REQUEST` | ID phòng bị thiếu khi tạo đơn đặt phòng |
| `1016` | `BOOKING_CHECK_IN_REQUIRED` | `400 BAD_REQUEST` | Ngày check-in bị để trống khi tạo đơn đặt phòng |
| `1017` | `BOOKING_CHECK_OUT_REQUIRED` | `400 BAD_REQUEST` | Ngày check-out bị để trống khi tạo đơn đặt phòng |
| `1018` | `BOOKING_DATE_INVALID` | `400 BAD_REQUEST` | Ngày check-out không sau ngày check-in |
| `1019` | `BOOKING_ROOM_UNAVAILABLE` | `409 CONFLICT` | Phòng đã có đơn đặt khác ở trạng thái `CONFIRMED` trùng lịch |
| `1020` | `BOOKING_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy đơn đặt phòng theo ID yêu cầu |
| `1021` | `BOOKING_ALREADY_CANCELLED` | `409 CONFLICT` | Đơn đặt phòng đã ở trạng thái `CANCELLED`, không thể hủy lại |
| `1022` | `ROOM_CANNOT_DELETE_OCCUPIED` | `400 BAD_REQUEST` | Từ chối xóa phòng khi phòng đang có khách lưu trú (`occupied == true`) |
| `1023` | `UNAUTHORIZED` | `403 FORBIDDEN` | Đã xác thực nhưng không đủ quyền hạn thực hiện hành động |
| `9999` | `UNCATEGORIZED_EXCEPTION` | `500 INTERNAL_SERVER_ERROR` | Lỗi hệ thống chưa được phân loại cụ thể |

---

## ⚠️ Giới hạn Kỹ thuật & Khuyến nghị Vận hành

1. **Ràng buộc Duy nhất trên Database (Unique Constraint)**:
   - Entity `User` đã khai báo `@Column(unique = true)` và mã nguồn có sẵn script di chuyển schema [`scripts/migrations/manual_migration_uk_user_username.sql`](scripts/migrations/manual_migration_uk_user_username.sql).
   - Khi triển khai lên môi trường sản xuất thực tế (nơi `ddl-auto=none`), quản trị viên CSDL cần thực hiện script SQL này thủ công sau khi đã kiểm tra không có dữ liệu trùng lặp.
2. **Không commit Secret thực tế lên Git**:
   - Các giá trị cấu hình nhạy cảm như mật khẩu CSDL production và chuỗi `JWT_SIGNER_KEY` tuyệt đối không được đưa vào mã nguồn hoặc tài liệu này. Hãy sử dụng cơ chế truyền biến môi trường hoặc hệ thống quản lý bí mật (Docker Secrets / Kubernetes Secrets).
3. **Phụ thuộc CSDL cho Kiểm thử Tự động**:
   - `HotelRestApiApplicationTests` hiện tại yêu cầu một phiên bản MySQL khả dụng tại cổng `3307` để khởi tạo DataSource. Khuyến nghị trong tương lai nên tích hợp Testcontainers hoặc cơ sở dữ liệu in-memory (H2) phục vụ CI/CD độc lập.
