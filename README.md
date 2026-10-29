# Hotel Reservation API Service

Hệ thống RESTful API Backend chuyên nghiệp phục vụ nghiệp vụ đặt phòng khách sạn.
---

## Mục lục
- [Kiến trúc Layered Architecture](#kiến-trúc-3-tầng-layered-architecture)
- [Tổ chức mã nguồn và công nghệ](#tổ-chức-mã-nguồn-và-công-nghệ)
- [Bảo mật & Xác thực](#bảo-mật--xác-thực)
- [Chức năng nghiệp vụ](#chức-năng-nghiệp-vụ)
- [Danh sách Endpoint](#danh-sách-endpoint)
- [Chuẩn Format Phản hồi](#chuẩn-format-phản-hồi)
- [Tài liệu API](#tài-liệu-api-swagger-ui)
- [Cài đặt & Triển khai Docker](#cài-đặt--triển-khai-docker)

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

## Kiến trúc Layered Architecture

Ứng dụng được thiết kế phân tầng rõ ràng nhằm đảm bảo tính độc lập, bảo mật và khả năng mở rộng:

```text
[ Tầng Giao tiếp (Presentation) ]
 ├── Security Filter Chain & JWT EntryPoint
 ├── REST Controllers
 └── DTOs & Chuẩn hóa Response
        ↓
[ Tầng Nghiệp vụ (Service Layer) ]
 ├── Use Case Services
 ├── Object Mapping - MapStruct
 └── Kiểm soát lỗi tập trung
        ↓
[ Tầng Dữ liệu & Hạ tầng (Persistence Layer) ]
 ├── Spring Data JPA Repositories
 └── JPA Entities & Enums
        ↓
      Relational Database
```

* **Presentation / API Layer**: Tiếp nhận HTTP request, xác thực token và chuẩn hóa toàn bộ đầu ra qua `ApiResponse`.
* **Business Logic Layer**: Xử lý toàn bộ quy tắc nghiệp vụ cốt lõi, tự động ánh xạ dữ liệu giữa DTO và Entity bằng MapStruct.
* **Persistence Layer**: Quản lý ánh xạ tới cơ sở dữ liệu.

---

## Tổ chức mã nguồn và công nghệ

```text
src/main/java/com/example/hotel_restapi/
├── configuration/          Cấu hình Spring Security, bộ lọc JWT, khởi tạo dữ liệu
├── controller/             4 REST Controller
├── dto/request/            Schema đầu vào, validation và wrapper chuẩn hóa ApiResponse
├── dto/response/           Schema dữ liệu trả về cho Client
├── entity/                 TUser, Room, Booking
├── enums/                  Role, RoomType, BookingStatus
├── exception/              AppException, ErrorCode, GlobalExceptionHandler
├── mapper/                 
├── repository/             Giao tiếp  qua Spring Data JPA Repositories
├── service/                Xác thực/JWT, quản lý người dùng, phòng và đặt phòng
└── HotelRestApiApplication.java  

src/main/resources/
└── application.properties 
src/test/                   Unit test
Dockerfile                  
pom.xml                     
```

Java 17+ · Spring Boot 4.1.1 · Spring Security · Spring Data JPA (Hibernate) · OAuth2 Resource Server / Nimbus JOSE + JWT · BCrypt Password Encoder · MapStruct · Lombok · MySQL · Maven Wrapper· Docker. Các thư viện và phiên bản được quản lý tập trung trong `pom.xml`.


---

## Bảo mật & Xác thực

* **Cơ chế**: Sử dụng JWT cho Authentication và Authorization.
* **Xử lý tập trung**: Kiểm tra tính hợp lệ của Token và quyền hạn người dùng.
* **Mã nguồn**: Controller kiểm tra quyền và bảo vệ tài nguyên hoàn toàn qua Annotation bảo mật (`@PreAuthorize`).

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
  "code": 1005,
  "message": "User not existed"
}
```

## Tài liệu API
* **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) *(khi chạy local)*
* **OpenAPI Spec **: `http://localhost:8080/v3/api-docs`

---

## Cài đặt & Triển khai Docker

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

```
## Hướng phát triển
- Tích hợp cổng thanh toán trực tuyến.
- Bổ sung Redis Cache giúp tăng tốc truy vấn danh sách phòng.
- Tích hợp OAuth2 (Đăng nhập qua Google).
- Bổ sung phân trang và giám sát hệ thống (Prometheus + Grafana).
