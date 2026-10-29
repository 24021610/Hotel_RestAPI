# Hotel Reservation API Service

Hệ thống RESTful API Backend chuyên nghiệp phục vụ nghiệp vụ đặt phòng khách sạn. Dự án tuân thủ nghiêm ngặt mô hình kiến trúc phân tầng (Layered Architecture) và chú trọng tối ưu các thuộc tính chất lượng (Quality Attributes) của phần mềm.

---

## Mục lục
- [Kiến trúc 3 tầng (Layered Architecture)](#kiến-trúc-3-tầng-layered-architecture)
- [Tổ chức mã nguồn và công nghệ](#tổ-chức-mã-nguồn-và-công-nghệ)
- [Bảo mật & Xác thực](#bảo-mật--xác-thực)
- [Chức năng nghiệp vụ](#chức-năng-nghiệp-vụ)
- [Danh sách Endpoint](#danh-sách-endpoint)
- [Chuẩn Format Phản hồi](#chuẩn-format-phản-hồi)
- [Tài liệu API (Swagger UI)](#tài-liệu-api-swagger-ui)
- [Cài đặt & Triển khai (Docker)](#cài-đặt--triển-khai-docker)

---

## Chức năng nghiệp vụ

### 1. Quản lý tài khoản (Authentication & Users)
- Đăng ký tài khoản, kiểm tra username trùng lặp và mã hóa mật khẩu.
- Đăng nhập, cấp JWT và xác thực token.
- Xem, cập nhật thông tin cá nhân.
- **ADMIN:** Xem danh sách, tìm kiếm, cập nhật, phân quyền và xóa tài khoản người dùng.

### 2. Quản lý phòng (Rooms)
- Xem danh sách, tra cứu phòng theo ID hoặc số phòng.
- Lọc phòng theo loại, giá, sức chứa và tình trạng phòng.
- **ADMIN:** Thêm, sửa, xóa phòng và quản lý trạng thái kinh doanh.

### 3. Quản lý đặt phòng (Bookings)
- Tạo đơn đặt phòng, tự động xác định người đặt qua JWT.
- Tính tổng chi phí theo giá phòng và số đêm lưu trú.
- Kiểm tra ngày lưu trú và tình trạng phòng, ngăn chặn đặt phòng trùng thời gian.
- Xem danh sách, chi tiết và hủy đơn đặt phòng của bản thân.
- **ADMIN:** Xem, tìm kiếm, xác nhận, từ chối, hủy và quản lý trạng thái các đơn đặt phòng.

### 4. Quy tắc nghiệp vụ
- Người dùng chỉ được quản lý thông tin và đơn đặt phòng thuộc quyền của mình.
- Chỉ ADMIN được thực hiện các thao tác quản trị tài khoản, phòng và đặt phòng.
- Không cho phép đặt phòng có ngày không hợp lệ, phòng không khả dụng hoặc đơn đặt phòng bị trùng lịch.
- Không cho phép hủy đơn đã hủy hoặc chuyển trạng thái đơn không hợp lệ.
- Không cho phép xóa dữ liệu làm ảnh hưởng đến tính toàn vẹn của lịch sử đặt phòng.

---

## Kiến trúc 3 tầng (Layered Architecture)

Ứng dụng được thiết kế phân tầng rõ ràng nhằm đảm bảo tính độc lập, bảo mật và khả năng mở rộng:

```text
[ Tầng Giao tiếp (Presentation / API Layer) ]
 ├── Security Filter Chain & JWT EntryPoint (configuration/SecurityConfiguration.java, JwtAuthenticationEntryPoint.java)
 ├── REST Controllers (controller/AuthenticationController.java, UserController.java, RoomController.java, BookingController.java)
 └── DTOs & Chuẩn hóa Response (dto/request/*, dto/response/*, ApiResponse.java)
        ↓
[ Tầng Nghiệp vụ (Business Logic / Service Layer) ]
 ├── Use Case Services (service/AuthenticationService.java, UserService.java, RoomService.java, BookingService.java)
 ├── Object Mapping - MapStruct (mapper/UserMapper.java, RoomMapper.java)
 └── Kiểm soát lỗi tập trung (exception/GlobalExceptionHandler.java, AppException.java, ErrorCode.java)
        ↓
[ Tầng Dữ liệu & Hạ tầng (Data Access / Persistence Layer) ]
 ├── Spring Data JPA Repositories (repository/UserRepository.java, RoomRepository.java, BookingRepository.java)
 └── JPA Entities & Enums (entity/User.java, Room.java, Booking.java, enums/Role.java, RoomType.java, BookingStatus.java)
        ↓
      Relational Database (MySQL)
```

* **Presentation / API Layer**: Tiếp nhận HTTP request, xác thực token qua bộ lọc bảo mật trước khi  vào Controller và chuẩn hóa toàn bộ đầu ra qua `ApiResponse`.
* **Business Logic Layer**: Xử lý toàn bộ quy tắc nghiệp vụ cốt lõi (kiểm tra trùng lịch đặt phòng, phân quyền người dùng, cấp phát/kiểm tra JWT), tự động ánh xạ dữ liệu giữa DTO và Entity ở thời điểm biên dịch (compile-time) bằng MapStruct.
* **Persistence Layer**: Quản lý ánh xạ tới cơ sở dữ liệu và đảm bảo tính toàn vẹn dữ liệu và mối quan hệ giữa các bảng `User`, `Room` và `Booking`.

---

## Tổ chức mã nguồn và công nghệ

```text
src/main/java/com/example/hotel_restapi/
├── configuration/          Cấu hình Spring Security, bộ lọc JWT, khởi tạo dữ liệu nền (InitConfiguration)
├── controller/             4 REST Controller: Authentication, User, Room, Booking
├── dto/request/            Schema đầu vào, validation và wrapper chuẩn hóa ApiResponse
├── dto/response/           Schema dữ liệu trả về cho Client (ẩn thông tin nhạy cảm)
├── entity/                 Thực thể ORM: User, Room, Booking
├── enums/                  Định nghĩa hằng số: Role, RoomType, BookingStatus
├── exception/              AppException, bộ mã lỗi ErrorCode và GlobalExceptionHandler
├── mapper/                 Interface MapStruct chuyển đổi hai chiều giữa Entity và DTO
├── repository/             Giao tiếp cơ sở dữ liệu qua Spring Data JPA Repositories
├── service/                4 service xử lý nghiệp vụ: xác thực/JWT, quản lý người dùng, phòng và đặt phòng
└── HotelRestApiApplication.java  Điểm khởi chạy ứng dụng Spring Boot

src/main/resources/
└── application.properties  Cấu hình kết nối Database, cổng dịch vụ, khóa bí mật JWT (Secret Key)
src/test/                   Unit test và kiểm thử tích hợp cho ứng dụng
Dockerfile                  Đóng gói ứng dụng thành Docker Image độc lập
pom.xml                     Quản lý dependencies, cấu hình build Maven và plugin MapStruct/Lombok
```

Java 17+ · Spring Boot 4.1.1 · Spring Security · Spring Data JPA (Hibernate) · OAuth2 Resource Server / Nimbus JOSE + JWT · BCrypt Password Encoder · MapStruct · Lombok · MySQL · Maven Wrapper (`mvnw`) · Docker. Các thư viện và phiên bản được quản lý tập trung trong `pom.xml`.
* `configuration`: Chứa cấu hình bảo mật (Spring Security, JWT) và khởi tạo các Bean hệ thống.

---

## Bảo mật & Xác thực

* **Cơ chế**: Sử dụng JSON Web Token (JWT) cho Authentication và Authorization.
* **Xử lý tập trung**: Kiểm tra tính hợp lệ của Token và quyền hạn người dùng tại tầng Filter/Middleware của Spring Security.
* **Mã nguồn tinh gọn (Clean Code)**: Tầng Controller sạch bóng code kiểm tra quyền thủ công; bảo vệ tài nguyên hoàn toàn qua các Annotation bảo mật (e.g. `@PreAuthorize`).


---
## Danh sách Endpoint 
### 1. Xác thực (Authentication)
| Method | Endpoint | Quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/token` | Public | Đăng nhập và nhận JWT token |
| `POST` | `/auth/introspect` | Public | Xác thực tính hợp lệ của Token |

### 2. Người dùng (Users)
| Method | Endpoint | Quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/users` | Public | Đăng ký tài khoản người dùng |
| `GET` | `/users` | `ADMIN` | Lấy toàn bộ danh sách người dùng |
| `GET` | `/users/{userId}` | Authenticated | Lấy thông tin người dùng theo ID |
| `PUT` | `/users/{userId}` | Authenticated | Cập nhật thông tin cá nhân |
| `DELETE`| `/users/{userId}` | `ADMIN` | Xóa người dùng |

### 3. Phòng khách sạn (Rooms)
| Method | Endpoint | Quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/rooms` | Public | Xem danh sách các phòng |
| `GET` | `/rooms/{roomId}`| Public | Xem chi tiết phòng theo ID |
| `POST` | `/rooms` | `ADMIN` | Thêm phòng mới |
| `PUT` | `/rooms/{roomId}`| `ADMIN` | Cập nhật thông tin phòng |
| `DELETE`| `/rooms/{roomId}`| `ADMIN` | Xóa phòng |

### 4. Đặt phòng (Bookings)
| Method | Endpoint | Quyền | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/bookings` | Authenticated | Đặt phòng mới |
| `GET` | `/bookings` | Authenticated | Xem danh sách đặt phòng |

---

## Chuẩn Format Phản hồi


```json
{
  "code": 1000,
  "message": "Thành công",
  "result": { ... }
}
```

Trong trường hợp có lỗi nghiệp vụ:
```json
{
  "code": 1001,
  "message": "User not existed"
}
```

## Tài liệu API (Swagger UI)

Dự án tích hợp sẵn thư viện `springdoc-openapi` để tự động tạo tài liệu tương tác API trực quan:

* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) *(khi chạy local)*
* **OpenAPI Spec (JSON)**: `http://localhost:8080/v3/api-docs`

---

## Cài đặt & Triển khai (Docker)

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
