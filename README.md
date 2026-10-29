# Hotel_RestAPI

Hotel_RestAPI là REST API được xây dựng bằng Spring Boot, phục vụ quản lý
phòng khách sạn và đặt phòng. Đây là phiên bản Phase 1 của đồ án môn Kiến
trúc phần mềm.

Ứng dụng hiện cung cấp:

- Tạo và quản lý tài khoản người dùng.
- Đăng nhập bằng JWT và kiểm tra tính hợp lệ của token.
- Quản lý phòng dành cho quản trị viên.
- Tra cứu phòng và lọc theo cờ `occupied` được lưu trong cơ sở dữ liệu.
- Tạo, xem, hủy booking; kiểm tra quyền sở hữu, tính chi phí và phát hiện
  booking bị trùng thời gian.

## Mục lục

- [Tổng quan dự án](#tổng-quan-dự-án)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Kiến trúc](#kiến-trúc)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Luồng nghiệp vụ](#luồng-nghiệp-vụ)
- [Điều kiện cần thiết](#điều-kiện-cần-thiết)
- [Cài đặt và cấu hình](#cài-đặt-và-cấu-hình)
- [Build, test và chạy ứng dụng](#build-test-và-chạy-ứng-dụng)
- [Tài liệu API](#tài-liệu-api)
- [Xác thực và phân quyền](#xác-thực-và-phân-quyền)
- [Giới hạn hiện tại](#giới-hạn-hiện-tại)

## Tổng quan dự án

### Bài toán

API tập trung các nghiệp vụ cơ bản của khách sạn trong một backend service.
Người dùng có thể đăng nhập và đặt phòng; quản trị viên có thể quản lý người
dùng và phòng.

### Phạm vi Phase 1

Phase 1 sử dụng Spring Boot, kiến trúc phân lớp và MySQL. Miền nghiệp vụ có
ba entity chính:

- `User`: username, password, thông tin tên và roles.
- `Room`: số phòng, sức chứa, loại phòng, giá và cờ `occupied`.
- `Booking`: người dùng, phòng, ngày nhận/trả phòng, tổng tiền và trạng thái.

Trạng thái booking hiện có là `CONFIRMED` và `CANCELLED`. Dự án chưa triển
khai thanh toán, check-in, check-out, tìm phòng theo khoảng ngày hoặc
Swagger/OpenAPI.

## Chức năng

### Quản lý người dùng

- `POST /users` tạo người dùng mới.
- `UserService` mặc định gán role `USER` cho người dùng mới.
- Mật khẩu được mã hóa bằng BCrypt trước khi lưu.
- ADMIN có thể xem danh sách, xem theo ID và xóa người dùng.
- Người dùng đã đăng nhập có thể tìm người dùng theo username.
- Việc cập nhật user được kiểm tra sau khi xử lý: username của kết quả phải
  trùng với username của user đang đăng nhập.

### Xác thực

- `POST /auth/login` kiểm tra username/password và trả về JWT.
- `POST /auth/introspect` kiểm tra chữ ký và thời hạn của token.
- Quyền trong JWT được đọc từ claim `scope`.
- Hai role hiện có là `ADMIN` và `USER`.

### Quản lý phòng

- Người dùng đã đăng nhập có thể xem danh sách phòng và xem phòng theo ID hoặc
  số phòng.
- Người dùng đã đăng nhập có thể lọc phòng theo cờ `occupied` đang lưu.
- ADMIN có thể tạo, cập nhật một phần và xóa phòng.
- Số phòng được kiểm tra trùng khi tạo và cập nhật.
- Request tạo/cập nhật phòng kiểm tra số phòng, sức chứa, loại phòng và giá.

### Quản lý booking

- Người dùng đã đăng nhập có thể tạo booking.
- Người sở hữu booking được lấy từ JWT, không lấy từ request body.
- Ngày trả phòng phải sau ngày nhận phòng.
- Tổng tiền được tính bằng giá phòng nhân với số đêm và làm tròn hai chữ số.
- Booking mới bị từ chối nếu có booking `CONFIRMED` trùng khoảng thời gian.
- Người dùng chỉ xem và hủy booking của mình.
- ADMIN có thể xem và hủy booking của mọi người dùng.

## Công nghệ sử dụng

| Công nghệ | Phiên bản/nguồn | Vai trò |
|---|---|---|
| Java | 17 | Runtime và biên dịch ứng dụng |
| Spring Boot | 4.1.1 | Framework chính |
| Maven Wrapper | Maven 3.9.16 | Build và quản lý dependency |
| Spring Web MVC | Spring Boot starter | REST API và xử lý HTTP |
| Spring Data JPA | Spring Boot starter | Persistence và repository |
| Hibernate | Được Spring Boot quản lý | Triển khai JPA |
| MySQL Connector/J | Runtime dependency | JDBC driver cho MySQL |
| MySQL | 8.0 trong Docker Compose | Cơ sở dữ liệu quan hệ |
| Spring Security | Dependency của dự án | Xác thực và phân quyền |
| OAuth2 Resource Server | Spring Boot starter | Kiểm tra bearer token JWT |
| Nimbus JOSE + JWT | 9.37.3 | Tạo và kiểm tra JWT |
| Spring Boot Validation | Spring Boot starter | Validate request |
| MapStruct | 1.5.5.Final | Mapping Entity/DTO |
| Lombok | Phiên bản do Spring Boot quản lý | Giảm boilerplate code |
| Docker | `Dockerfile`, `docker-compose.yml` | Đóng gói và chạy service tùy chọn |
| JUnit 5 và Mockito | Test dependencies | Kiểm thử tự động |

## Kiến trúc

Ứng dụng sử dụng kiến trúc phân lớp theo package:

```text
Client
  |
  v
Spring Security filter chain
  |
  v
Controller -> Service -> Repository -> MySQL
    |           |            |
    v           v            v
   DTO       Business      Entity
             rules       persistence
```

- **Controller** nhận HTTP request và trả về `ApiResponse<T>`.
- **Service** chứa logic xác thực, người dùng, phòng và booking.
- **Repository** dùng Spring Data JPA để truy cập database.
- **Entity** gồm `User`, `Room` và `Booking`.
- **DTO** định nghĩa payload request/response.
- **Mapper** dùng MapStruct để chuyển đổi user và room.
- **Configuration** định nghĩa security và tài khoản ADMIN khởi tạo.
- **Exception** gồm `AppException`, `ErrorCode` và
  `GlobalExceptionHandler`.

## Cấu trúc mã nguồn

```text
src/
├── main/
│   ├── java/com/example/hotel_restapi/
│   │   ├── HotelRestApiApplication.java
│   │   ├── configuration/
│   │   │   ├── InitConfiguration.java
│   │   │   ├── JwtAuthenticationEntryPoint.java
│   │   │   └── SecurityConfiguration.java
│   │   ├── controller/
│   │   │   ├── AuthenticationController.java
│   │   │   ├── BookingController.java
│   │   │   ├── RoomController.java
│   │   │   └── UserController.java
│   │   ├── dto/request/
│   │   ├── dto/response/
│   │   ├── entity/
│   │   ├── enums/
│   │   ├── exception/
│   │   ├── mapper/
│   │   ├── repository/
│   │   └── service/
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/example/hotel_restapi/
        ├── HotelRestApiApplicationTests.java
        └── service/BookingServiceTest.java
```

Các class tiêu biểu:

- `HotelRestApiApplication`: điểm khởi chạy Spring Boot.
- `SecurityConfiguration`: JWT decoder, password encoder và HTTP security rules.
- `InitConfiguration`: tạo user `admin` nếu user này chưa tồn tại.
- `AuthenticationService`: xác thực user và tạo/kiểm tra JWT.
- `UserService`: xử lý user và các kiểm tra quyền ở cấp service.
- `RoomService`: xử lý phòng và kiểm tra quyền ADMIN.
- `BookingService`: validate booking, ownership, tính tiền và hủy booking.

## Luồng nghiệp vụ

### Luồng đăng nhập

1. Client gửi thông tin đăng nhập tới `POST /auth/login`.
2. `AuthenticationService` tìm user theo username.
3. Password gửi lên được so sánh với password BCrypt đã lưu.
4. Hệ thống tạo JWT dùng HS256, có thời hạn một ngày.
5. Token chứa username trong claim `sub` và roles trong claim `scope`.
6. Client gửi token trong các request cần xác thực:

   ```http
   Authorization: Bearer <JWT_TOKEN>
   ```

### Luồng tạo booking

1. Client gửi `roomId`, `checkIn` và `checkOut` tới `POST /bookings`.
2. Bean Validation kiểm tra các trường bắt buộc.
3. `BookingService` kiểm tra check-out có sau check-in hay không.
4. Service tìm user đang đăng nhập và room được yêu cầu.
5. `BookingRepository.existsOverlappingActiveBooking` kiểm tra booking
   `CONFIRMED` bị trùng theo điều kiện:

   ```text
   check-in hiện tại < check-out mới
   AND
   check-out hiện tại > check-in mới
   ```

6. Service tính số đêm và tổng tiền.
7. Một `Booking` được lưu với trạng thái `CONFIRMED`.
8. Controller trả về HTTP `201 Created` cùng `BookingResponse`.

Khi hủy, trạng thái booking chuyển thành `CANCELLED`. Booking đã hủy không
thể hủy lần thứ hai.

## Điều kiện cần thiết

- JDK 17.
- Git.
- MySQL tương thích với MySQL 8.0, hoặc Docker Desktop nếu dùng Compose.
- Không bắt buộc cài Maven vì repository có `mvnw` và `mvnw.cmd`.
- Postman, cURL hoặc HTTP client khác để kiểm thử thủ công.

## Cài đặt và cấu hình

### Clone repository

```powershell
git clone https://github.com/24021610/Hotel_RestAPI.git
cd Hotel_RestAPI
```

### Cấu hình MySQL local

Thiết lập mặc định nằm trong
[`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/hotel_db?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=<your_mysql_password>
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
jwt.signerKey=<your_local_jwt_key>
```

Tạo database `hotel_db` nếu database chưa tồn tại, hoặc sử dụng tài khoản
MySQL có quyền tạo database. Thay password và JWT key bằng giá trị local.
Không commit secret thật vào repository.

Repository hiện không có file `application-prod.properties` và không có cấu
hình production theo profile. `SecurityConfiguration` đọc khóa từ
`jwt.signerKey`, trong khi `AuthenticationService` hiện chứa trực tiếp một
khóa ký trong source. Hai giá trị này phải thống nhất để login, introspect và
các request cần xác thực hoạt động đúng.

### Cấu hình Docker Compose

Repository có [`docker-compose.yml`](docker-compose.yml), khởi chạy:

- MySQL 8.0 với container `hotel_mysql`, mở host port `3308`.
- API với container `hotel_api`, mở port `8080`.

Chạy bằng lệnh:

```powershell
docker compose up --build
```

Trong Compose, API kết nối tới MySQL tại `mysql:3306`. Khi chạy API trực tiếp
trên máy, cấu hình mặc định dùng `localhost:3307`.

## Build, test và chạy ứng dụng

Windows PowerShell:

```powershell
.\mvnw.cmd clean package
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw clean package
./mvnw test
./mvnw spring-boot:run
```

Port mặc định là `8080`. Có thể xác nhận khởi động
thành công qua log Spring Boot và gọi `POST /auth/login`.

## Tài liệu API

Các response thông thường sử dụng wrapper:

```json
{
  "code": 100,
  "result": {}
}
```

Lỗi được xử lý bởi `GlobalExceptionHandler` và thường chứa `code`, `message`.
HTTP status cụ thể lấy từ `ErrorCode`, ngoại trừ một số runtime exception được
xử lý bởi nhánh lỗi tổng quát hiện tại.

### Authentication

| Method | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/auth/login` | Đăng nhập và nhận JWT | Public |
| `POST` | `/auth/introspect` | Kiểm tra chữ ký và thời hạn token | Public |

### Users

| Method | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/users` | Tạo user và gán role `USER` | Public |
| `GET` | `/users` | Lấy danh sách user | `ADMIN` |
| `GET` | `/users/{userId}` | Lấy user theo ID | `ADMIN` qua method security |
| `GET` | `/users/by-username/{username}` | Lấy user theo username | User đã đăng nhập |
| `PUT` | `/users/{userId}` | Cập nhật user | Post-authorize khi username kết quả trùng user hiện tại |
| `DELETE` | `/users/{userId}` | Xóa user | `ADMIN` |

### Rooms

| Method | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/rooms` | Tạo phòng | `ADMIN` |
| `GET` | `/rooms` | Lấy tất cả phòng | User đã đăng nhập |
| `GET` | `/rooms/{id}` | Lấy phòng theo ID | User đã đăng nhập |
| `GET` | `/rooms/number/{number}` | Lấy phòng theo số phòng | User đã đăng nhập |
| `GET` | `/rooms/available` | Lấy phòng có `occupied=false` | User đã đăng nhập |
| `GET` | `/rooms/occupied` | Lấy phòng có `occupied=true` | User đã đăng nhập |
| `PATCH` | `/rooms/{id}` | Cập nhật một phần thông tin phòng | `ADMIN` |
| `DELETE` | `/rooms/{id}` | Xóa phòng | `ADMIN` |

Các giá trị `RoomType` được hỗ trợ:
`SINGLE`, `DOUBLE`, `TWIN`, `TRIPLE`, `SUITE`, `CONNECTING`, `ADJACENT`,
`ACCESSIBLE`, `PRESIDENTIAL` và `OTHER`.

### Bookings

| Method | Endpoint | Mô tả | Quyền truy cập |
|---|---|---|---|
| `POST` | `/bookings` | Tạo booking `CONFIRMED` | User đã đăng nhập |
| `GET` | `/bookings` | Lấy danh sách booking | User thấy booking của mình; ADMIN thấy tất cả |
| `GET` | `/bookings/{id}` | Xem chi tiết booking | User thấy booking của mình; ADMIN thấy mọi booking |
| `DELETE` | `/bookings/{id}` | Hủy booking | User hủy booking của mình; ADMIN hủy mọi booking |

### Ví dụ request

Đăng nhập:

```http
POST http://localhost:8080/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin"
}
```

Initializer chỉ tạo tài khoản này khi username `admin` chưa tồn tại.

Ví dụ Response:

```json
{
  "code": 100,
  "result": {
    "token": "<JWT_TOKEN>",
    "authenticated": true
  }
}
```

Tạo booking:

```http
POST http://localhost:8080/bookings
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "roomId": 1,
  "checkIn": "2026-11-01",
  "checkOut": "2026-11-04"
}
```

Ví dụ Response booking:

```json
{
  "code": 100,
  "result": {
    "id": 10,
    "userId": "user-id",
    "roomId": 1,
    "checkIn": "2026-11-01",
    "checkOut": "2026-11-04",
    "totalCost": 300.00,
    "status": "CONFIRMED"
  }
}
```

## Xác thực và phân quyền

Với request cần xác thực, gửi header:

```http
Authorization: Bearer <JWT_TOKEN>
```

`SecurityConfiguration` cho phép không cần đăng nhập đối với các request
POST tới:

- `/users`
- `/auth`
- `/auth/introspect`
- `/auth/login`

Các request còn lại yêu cầu bearer token hợp lệ. `@PreAuthorize` và
`@PostAuthorize` bổ sung các giới hạn quyền ở service. Quyền sở hữu booking
được kiểm tra qua repository query sử dụng username đã xác thực.

## Giới hạn hiện tại

Các giới hạn của dự án:

- `Room.occupied` là boolean được lưu trong database và không được cập nhật
  khi tạo hoặc hủy booking. Vì vậy API available/occupied không tính khả dụng
  theo một khoảng ngày cụ thể.
- Chưa có endpoint check-in, check-out, thanh toán hoặc chuyển trạng thái
  booking ngoài `CONFIRMED` và `CANCELLED`.
- Chưa có cấu hình Swagger/OpenAPI.
- Hiện chỉ có Spring context test và `BookingServiceTest`, chưa có MockMvc
  test cho controller hoặc end-to-end API test.
- `spring.jpa.hibernate.ddl-auto=update` và `spring.jpa.show-sql=true` phù hợp
  hơn với môi trường phát triển.
- Datasource và JWT settings mặc định nằm trong `application.properties`,
  chưa có cơ chế quản lý secret riêng cho môi trường triển khai.
- `AuthenticationService` chứa JWT signing key trong source, trong khi decoder
  đọc `jwt.signerKey` từ configuration.
- `UserUpdateRequest` có field `roles` và mapper hiện tại không ignore rõ ràng
  field này.
- Exception handler và error-code mapping cần được kiểm thử integration,
  đặc biệt với JWT sai định dạng, JSON không hợp lệ và runtime exception.
- Concurrency khi đặt phòng, index database, migration, deployment, monitoring
  và performance chưa được đánh giá đầy đủ.
