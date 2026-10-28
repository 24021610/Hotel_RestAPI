# Booking

## 1. Tóm tắt các module & tính năng đã thực hiện

- **Quản lý đặt phòng:** User đã đăng nhập có thể tạo Booking, xem danh sách/chi tiết Booking của mình và hủy Booking.
- **Kiểm tra thông tin đặt phòng:** `checkOut` phải sau `checkIn`; `roomId`, `checkIn` và `checkOut` là các trường bắt buộc.
- **Kiểm tra tình trạng phòng:** Booking `CONFIRMED` bị trùng khoảng thời gian sẽ bị từ chối. Booking `CANCELLED` không còn chiếm phòng.
- **Tính chi phí đặt phòng:** Tổng chi phí được tính bằng giá phòng nhân số đêm lưu trú; client không gửi `totalCost`.
- **Xác thực & phân quyền:** Các endpoint Booking yêu cầu JWT. USER chỉ xem hoặc hủy được Booking thuộc tài khoản của mình. ADMIN có thể xem danh sách toàn hệ thống, xem chi tiết và hủy Booking của bất kỳ USER nào. Danh tính và role được lấy từ JWT.
- **Xử lý ngoại lệ tập trung:** Lỗi Booking được trả qua `ApiResponse` và xử lý bởi `GlobalExceptionHandler`, gồm Room không tồn tại, ngày không hợp lệ, phòng bị đặt trùng, Booking không tồn tại và Booking đã bị hủy.

## 2. Hướng dẫn kiểm thử nhanh qua Postman

**Tài khoản mặc định:** Khi ứng dụng khởi động, `InitConfiguration` tự tạo tài khoản nếu chưa tồn tại:

```text
Username: admin
Password: admin
```

### Bước 1: Lấy Token đăng nhập

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "admin"
}
```

Lấy giá trị `token` trong response để sử dụng cho các request Booking.

### Bước 2: Thiết lập Header xác thực

Thêm header cho các endpoint Booking:

```http
Authorization: Bearer <JWT>
```

Theo `SecurityConfiguration`, mọi request không thuộc nhóm public đều yêu cầu xác thực. Vì vậy toàn bộ endpoint Booking đều cần JWT.

### Bước 3: Test các API Booking

#### Tạo Booking

```http
POST /bookings
Content-Type: application/json
Authorization: Bearer <JWT>
```

```json
{
  "roomId": 1,
  "checkIn": "2026-11-01",
  "checkOut": "2026-11-04"
}
```

Thành công: `201 Created`.

```json
{
  "code": 100,
  "result": {
    "id": 10,
    "userId": "user-uuid",
    "roomId": 1,
    "checkIn": "2026-11-01",
    "checkOut": "2026-11-04",
    "totalCost": 300.00,
    "status": "CONFIRMED"
  }
}
```

Các trường hợp cần kiểm tra:

- Thiếu `roomId`, `checkIn` hoặc `checkOut`: `400`, mã `1015`, `1016` hoặc `1017`.
- `checkOut` không sau `checkIn`: `400`, mã `1018`.
- Room không tồn tại: `404`, mã `1008`.
- Phòng bị trùng với Booking `CONFIRMED`: `409`, mã `1019`.

#### Xem danh sách Booking

```http
GET /bookings
Authorization: Bearer <JWT>
```

Thành công: `200 OK`, response có dạng `ApiResponse` với `result` là danh sách Booking. USER chỉ nhận Booking của mình; ADMIN nhận Booking của toàn hệ thống.

#### Xem chi tiết Booking

```http
GET /bookings/{id}
Authorization: Bearer <JWT>
```

USER chỉ xem được Booking của mình. ADMIN xem được Booking bất kỳ. Booking không tồn tại hoặc USER không có quyền xem: `404`, mã `1020`.

#### Hủy Booking

```http
DELETE /bookings/{id}
Authorization: Bearer <JWT>
```

Thành công: `200 OK`; bản ghi không bị xóa mà chuyển sang trạng thái `CANCELLED`. USER chỉ hủy Booking của mình; ADMIN có thể hủy Booking bất kỳ.

Hủy Booking không thuộc User hiện tại hoặc không tồn tại: `404`, mã `1020`. Hủy lại Booking đã hủy: `409`, mã `1021`.

Repository hiện chưa có Postman collection cho Booking; các request trên cần được tạo thủ công trong Postman.
