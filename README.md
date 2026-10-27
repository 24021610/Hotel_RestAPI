# Hotel Booking REST API (Nhánh: TungBeng)

Dự án Backend RESTful API quản lý và đặt phòng khách sạn xây dựng trên nền tảng **Java 17 & Spring Boot 4.1.1**, tích hợp **Spring Security, JWT (Nimbus JOSE), Spring Data JPA và MySQL**.

---

## 1. Tóm tắt các module & tính năng nghiệp vụ

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
  - `GET /rooms`: Yêu cầu người dùng đã đăng nhập (bất kỳ role nào có token hợp lệ).
  - Khách hàng thông thường (`USER`) không có quyền can thiệp vào tài nguyên phòng.

### 🛡️ Xử lý ngoại lệ tập trung (Global Exception Handling)
- Đồng bộ hóa toàn bộ response lỗi theo format chuẩn `ApiResponse<T>`:
  ```json
  {
    "code": 1022,
    "message": "Cannot delete room that is currently occupied"
  }
  ```
- **Xử lý phân quyền 403**: Bắt `AccessDeniedException`, trả về HTTP 403 Forbidden kèm mã nghiệp vụ riêng `UNAUTHORIZED - 1023`.
- **Bảo mật dữ liệu đầu vào**: Bắt `HttpMessageNotReadableException` khi JSON sai cú pháp hoặc enum sai giá trị (trả về HTTP 400 kèm thông báo an toàn, không echo lại input tùy ý của client).
- **Chuẩn hóa mã HTTP 500**: Ngoại lệ chưa lường trước (`Exception.class`) trả về HTTP 500 Internal Server Error (`UNCATEGORIZED_EXCEPTION - 9999`) thay vì lộ chi tiết stack trace.

---

## 2. Nhật ký & Tóm tắt các cải tiến bảo mật đã thực hiện (Security Commits Summary)

Nhánh `TungBeng` đã hoàn thành 4 commit bảo mật trọng tâm giải quyết các lỗ hổng từ kiểm toán mã nguồn:

| Commit | Thông điệp Commit | Các file thay đổi chính | Vấn đề được giải quyết & Hành vi mới |
| :--- | :--- | :--- | :--- |
| **`3bab7d0`** | `fix: use configured JWT signing key consistently` | `AuthenticationService.java`<br>`application.properties` | **Đồng bộ hóa JWT Signing Key**: Trước đây `AuthenticationService` gán cứng chuỗi khóa ký trong thuộc tính lớp, gây lệch cấu hình với `SecurityConfiguration`. Commit đã loại bỏ chuỗi gán cứng, đồng bộ đọc qua `@Value("${jwt.signerKey}")`, hỗ trợ placeholder `${JWT_SIGNER_KEY:...}`. |
| **`7694324`** | `fix: handle invalid validation error codes safely` | `GlobalExceptionHandler.java` | **Xử lý ngoại lệ Validation an toàn**: Khắc phục lỗi `IllegalArgumentException` / HTTP 500 khi message validation không khớp tên enum `ErrorCode`. Hỗ trợ cả `FieldError` lẫn `ObjectError`, bọc `ErrorCode.valueOf()` trong khối try-catch an toàn và fallback về `INVALID_KEY` (1001, HTTP 400). |
| **`5ee3a46`** | `fix: ensure unique error codes` | `ErrorCode.java`<br>`README.md` | **Đảm bảo tính duy nhất của mã lỗi**: Tách mã số của `UNAUTHORIZED` từ `1006` (bị trùng với `UNAUTHENTICATED`) sang mã riêng biệt `1023` (HTTP 403 Forbidden). Xóa bỏ enum `INVALID_DOB` không sử dụng bị trùng mã `1008` với `ROOM_NOT_EXISTED`. |
| **`a0fefee`** | `fix: secure application configuration and initialization` | `application.properties`<br>`application-prod.properties`<br>`SecurityConfiguration.java`<br>`InitConfiguration.java`<br>`AuthenticationService.java`<br>`Dockerfile`<br>`ConfigurationSecurityTest.java` | **Bảo mật CSDL, Môi trường, Seed Accounts & Khóa JWT**: Tách profile `prod` không fallback cho secret; kiểm tra độ dài khóa JWT tối thiểu 32 bytes UTF-8 (256 bits); đồng bộ UTF-8 cho ký/verify/decode JWT; cơ chế fail-fast cho seed accounts trong production; bảo vệ không ghi đè tài khoản hiện có; bổ sung 16 unit tests tự động. |

---

## 3. Cấu hình môi trường & Triển khai an toàn (Environment Configuration)

Hệ thống phân tách rõ rệt giữa cấu hình phát triển cục bộ (`default` profile) và cấu hình môi trường sản xuất (`prod` profile).

### Bảng tra cứu các biến môi trường cấu hình

| Tên biến môi trường | Mô tả & Mục đích | Bắt buộc trong `prod`? | Mặc định trong Profile `default` (Dev) | Mặc định trong Profile `prod` (Production) |
| :--- | :--- | :---: | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Xác định profile hoạt động của Spring Boot | Khuyên dùng | `default` | `prod` (Thiết lập mặc định trong `Dockerfile`) |
| `DB_URL` | JDBC URL kết nối MySQL CSDL | **BẮT BUỘC** | `jdbc:mysql://localhost:3307/hotel_db?...` | Không có (Bắt buộc truyền biến môi trường) |
| `DB_USERNAME` | Tên người dùng MySQL | **BẮT BUỘC** | `root` | Không có (Bắt buộc truyền biến môi trường) |
| `DB_PASSWORD` | Mật khẩu truy cập MySQL | **BẮT BUỘC** | Mật khẩu local dev | Không có (Bắt buộc truyền biến môi trường) |
| `JWT_SIGNER_KEY` | Khóa bí mật ký token HMAC-SHA256 (>= 256 bits) | **BẮT BUỘC** | Khóa HMAC lịch sử local dev | Không có (Bắt buộc truyền biến môi trường) |
| `APP_INIT_ENABLED` | Bật/tắt tự động khởi tạo tài khoản mẫu | Tùy chọn | `true` (Tự động tạo admin/user mẫu) | `false` (Mặc định tắt hoàn toàn trên production) |
| `APP_INIT_ADMIN_PASSWORD` | Mật khẩu khởi tạo cho tài khoản `admin` | Tùy chọn | `admin` | Không có (Chỉ dùng khi bootstrap, cấm để mặc định) |
| `APP_INIT_USER_PASSWORD` | Mật khẩu khởi tạo cho tài khoản `user` | Tùy chọn | `user` | Không có (Chỉ dùng khi bootstrap, cấm để mặc định) |

### Cơ chế bảo vệ & Cảnh báo an ninh quan trọng

> [!WARNING]
> **Cảnh báo về Thông tin đăng nhập & Khóa ký lịch sử trong Git**:
> - Các giá trị fallback mặc định trong `application.properties` (URL CSDL cục bộ, mật khẩu local dev và chuỗi khóa ký JWT mặc định) đã tồn tại trong lịch sử commit Git công khai.
> - **Tuyệt đối không sử dụng lại các giá trị này trên môi trường Production**. Nếu các thông tin này từng được áp dụng ngoài môi trường local, quản trị viên bắt buộc phải thực hiện thu hồi và đổi mới (rotate) ngay lập tức trên hệ quản trị CSDL và hệ thống cấp phát token.
> - Không bao giờ commit mật khẩu, khóa bí mật hoặc token thực tế vào Git. Sử dụng cơ chế quản lý secret chuyên dụng (Docker Secrets, Kubernetes Secrets, GitHub Actions Secrets hoặc AWS Secrets Manager).

> [!IMPORTANT]
> **Cơ chế Fail-safe cho Khóa ký JWT trên Production**:
> - File `application-prod.properties` cấu hình `jwt.signerKey=${JWT_SIGNER_KEY}` không có fallback. Nếu thiếu biến môi trường này khi khởi động với profile `prod`, Spring Boot sẽ fail-fast ngay lập tức với lỗi `PlaceholderResolutionException`.
> - Phương thức `@PostConstruct validateJwtSignerKey()` kiểm tra độ dài byte UTF-8 của khóa (`>= 32 bytes`, tương đương tối thiểu 256 bits). Nếu khóa không đạt chuẩn, ứng dụng từ chối khởi động (`IllegalStateException`) mà không in giá trị khóa ra log.
> - **Lưu ý về Entropy**: Độ dài chuỗi `>= 32 bytes` chỉ là ràng buộc thuật toán tối thiểu, không đại diện cho độ hỗn loạn hay tính ngẫu nhiên an toàn. Khóa ký production bắt buộc phải được sinh bằng bộ sinh số ngẫu nhiên mạnh về mật mã (CSPRNG), ví dụ:
>   ```bash
>   openssl rand -base64 32
>   # hoặc
>   openssl rand -hex 32
>   ```

> [!IMPORTANT]
> **Quy tắc an toàn Khởi tạo Tài khoản Mẫu (Seed Accounts)**:
> - Trên Production, seed accounts mặc định bị tắt hoàn toàn (`APP_INIT_ENABLED=false`).
> - **Thao tác Bootstrap tường minh**: Nếu quản trị viên chủ động bật seed trên Production (`APP_INIT_ENABLED=true`) để khởi tạo hệ thống, **bắt buộc phải cung cấp mật khẩu mạnh và độc nhất** qua `APP_INIT_ADMIN_PASSWORD` và `APP_INIT_USER_PASSWORD`.
> - **Chặn đứng mật khẩu mặc định (Fail-fast)**: Nếu bật seed trên `prod` mà mật khẩu admin hoặc user bị để trống hoặc giữ nguyên giá trị mặc định (`"admin"` / `"user"`), hệ thống sẽ ném `IllegalStateException` và dừng ứng dụng ngay lập tức, ngăn ngừa hoàn toàn nguy cơ vô tình để lọt tài khoản mặc định nguy hiểm.
> - **Tính bất biến của tài khoản hiện có**: Hệ thống luôn kiểm tra `findByUsername(...).isEmpty()` trước khi lưu, bảo đảm **không bao giờ ghi đè** dữ liệu hoặc mật khẩu của các tài khoản đã tồn tại trong CSDL.

### Hướng dẫn chạy và triển khai Docker

Image `Dockerfile` thiết lập giá trị mặc định tin cậy `ENV SPRING_PROFILES_ACTIVE=prod`. Khi chạy container, có thể ghi đè biến môi trường thông qua tham số `-e`:

- **Chạy container trên Production (Bắt buộc cung cấp đầy đủ secret)**:
  ```bash
  docker run -d -p 8080:8080 \
    -e DB_URL="jdbc:mysql://<db-host>:3306/hotel_db?useSSL=true&serverTimezone=UTC" \
    -e DB_USERNAME="<YOUR_DB_USERNAME>" \
    -e DB_PASSWORD="<YOUR_DB_PASSWORD>" \
    -e JWT_SIGNER_KEY="<YOUR_MINIMUM_256_BIT_SECRET_KEY>" \
    hotel-restapi:latest
  ```

- **Chạy container thử nghiệm cục bộ (Ghi đè sang profile dev `default`)**:
  ```bash
  docker run -d -p 8080:8080 \
    -e SPRING_PROFILES_ACTIVE=default \
    hotel-restapi:latest
  ```

---

## 4. Hướng dẫn kiểm thử API thực tế (Practical API Testing Guide)

Dưới đây là chuỗi kịch bản kiểm thử từng bước mà đồng đội có thể chạy trực tiếp trên môi trường local (mặc định tại `http://localhost:8080`).

*(Tất cả các endpoint và status code dưới đây đều đã được xác minh thực tế trên mã nguồn và kiểm thử runtime trực tiếp).*

### Bước 1: Khởi động ứng dụng & Xác nhận kết nối CSDL
1. Đảm bảo MySQL local đang chạy trên port `3307` (hoặc container `hotel-mysql`).
2. Khởi động ứng dụng Spring Boot:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Quan sát log console: Xác nhận HikariCP kết nối thành công tới `localhost:3307/hotel_db` và Tomcat lắng nghe tại port `8080`.

---

### Bước 2: Đăng nhập lấy Token xác thực (Authentication)
Gửi yêu cầu đăng nhập với tài khoản hợp lệ để nhận JWT.

- **Method & URL**: `POST http://localhost:8080/auth/login`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "admin",
    "password": "<ADMIN_PASSWORD>"
  }
  ```
- **Lệnh PowerShell**:
  ```powershell
  $loginAdmin = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
    -Method POST -Headers @{"Content-Type"="application/json"} `
    -Body '{"username":"admin","password":"<ADMIN_PASSWORD>"}'
  $adminToken = $loginAdmin.result.token
  Write-Output "Admin Token: $adminToken"
  ```
- **Lệnh cURL**:
  ```bash
  curl -X POST http://localhost:8080/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}'
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "code": 100,
      "result": {
        "token": "<JWT_TOKEN_STRING>",
        "authenticated": true
      }
    }
    ```
- **Ý nghĩa kết quả**: Ứng dụng xác thực mật khẩu qua `BCryptPasswordEncoder` thành công và cấp token ký HMAC-SHA256 chứa scope quyền hạn tương ứng.

---

### Bước 3: Kiểm tra tính hợp lệ của Token qua Introspect Endpoint
Kiểm tra token bằng endpoint nội bộ trước khi gọi các dịch vụ khác.

- **Method & URL**: `POST http://localhost:8080/auth/introspect`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "token": "<YOUR_TOKEN>"
  }
  ```
- **Lệnh PowerShell**:
  ```powershell
  $intro = Invoke-RestMethod -Uri "http://localhost:8080/auth/introspect" `
    -Method POST -Headers @{"Content-Type"="application/json"} `
    -Body (@{"token"=$adminToken} | ConvertTo-Json)
  Write-Output "Token hợp lệ: $($intro.result.valid)"
  ```
- **Lệnh cURL**:
  ```bash
  curl -X POST http://localhost:8080/auth/introspect \
    -H "Content-Type: application/json" \
    -d '{"token":"<YOUR_TOKEN>"}'
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "code": 100,
      "result": {
        "valid": true
      }
    }
    ```
- **Ý nghĩa kết quả**: `AuthenticationService.introspect()` sử dụng `MACVerifier` giải mã chữ ký bằng đúng `StandardCharsets.UTF_8` và kiểm tra thời hạn token còn hiệu lực.

---

### Bước 4: Gọi Endpoint được bảo vệ bằng Token hợp lệ
Gọi endpoint quản lý danh sách phòng với header Authorization chứa Bearer token.

- **Method & URL**: `GET http://localhost:8080/rooms`
- **Headers**:
  ```text
  Authorization: Bearer <YOUR_TOKEN>
  ```
- **Request Body**: Không có
- **Lệnh PowerShell**:
  ```powershell
  $rooms = Invoke-RestMethod -Uri "http://localhost:8080/rooms" `
    -Method GET -Headers @{"Authorization"="Bearer $adminToken"}
  Write-Output "Tổng số phòng tìm thấy: $($rooms.result.Count)"
  ```
- **Lệnh cURL**:
  ```bash
  curl -X GET http://localhost:8080/rooms \
    -H "Authorization: Bearer <YOUR_TOKEN>"
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "code": 100,
      "result": [
        {
          "id": 1,
          "number": "101",
          "noOfPerson": 2,
          "type": "SINGLE",
          "price": 100.0,
          "occupied": false
        }
      ]
    }
    ```
- **Ý nghĩa kết quả**: `NimbusJwtDecoder` trong `SecurityConfiguration` giải mã token thành công và cho phép truy cập tài nguyên được bảo vệ.

---

### Bước 5: Kiểm tra từ chối khi không truyền Token (Unauthenticated)
Gửi request tới endpoint bảo vệ mà không cung cấp token để kiểm tra cơ chế chặn truy cập trái phép.

- **Method & URL**: `GET http://localhost:8080/rooms`
- **Headers**: Không có Header `Authorization`
- **Lệnh PowerShell**:
  ```powershell
  try {
      Invoke-WebRequest -Uri "http://localhost:8080/rooms" -Method GET
  } catch {
      $stream = $_.Exception.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      Write-Output "HTTP Status: $($_.Exception.Response.StatusCode.value__)"
      Write-Output "Response: $($reader.ReadToEnd())"
  }
  ```
- **Lệnh cURL**:
  ```bash
  curl -i -X GET http://localhost:8080/rooms
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `401 Unauthorized`
  - Response Body:
    ```json
    {
      "code": 1006,
      "message": "Unauthenticated"
    }
    ```
- **Ý nghĩa kết quả**: `JwtAuthenticationEntryPoint` bắt được lỗi thiếu xác thực và trả về chuẩn JSON `UNAUTHENTICATED` (1006) kèm HTTP 401.

---

### Bước 6: Kiểm tra từ chối Token bị giả mạo / can thiệp (Tampered Token)
Thay đổi một hoặc vài ký tự trong phần payload hoặc signature của token để kiểm tra tính toàn vẹn chữ ký.

- **Lệnh PowerShell**:
  ```powershell
  # Tạo token giả mạo bằng cách sửa 4 ký tự cuối của chữ ký
  $tamperedToken = $adminToken.Substring(0, $adminToken.Length - 4) + "XXXX"
  try {
      Invoke-WebRequest -Uri "http://localhost:8080/rooms" -Method GET `
        -Headers @{"Authorization"="Bearer $tamperedToken"}
  } catch {
      $stream = $_.Exception.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      Write-Output "HTTP Status: $($_.Exception.Response.StatusCode.value__)"
      Write-Output "Response: $($reader.ReadToEnd())"
  }
  ```
- **Kết quả kỳ vọng**:
  - Khi gọi `GET /rooms`: HTTP Status `401 Unauthorized`, body `{"code":1006,"message":"Unauthenticated"}`.
  - Khi gọi `POST /auth/introspect`: HTTP Status `200 OK`, body `{"code":100,"result":{"valid":false}}`.
- **Ý nghĩa kết quả**: Thuật toán HMAC-SHA256 phát hiện tính không toàn vẹn của chữ ký mật mã và từ chối cấp quyền truy cập ngay lập tức.

---

### Bước 7: Kiểm tra xử lý lỗi Validation đầu vào (Validation Handling)
Gửi dữ liệu vi phạm ràng buộc DTO để kiểm tra `GlobalExceptionHandler`.

#### Trường hợp 7A: Vi phạm độ dài mật khẩu (`POST /users`)
- **Method & URL**: `POST http://localhost:8080/users`
- **Headers**: `Content-Type: application/json`
- **Request Body** (Mật khẩu ít hơn 6 ký tự theo `@Size(min = 6)`):
  ```json
  {
    "username": "testuser",
    "password": "123"
  }
  ```
- **Lệnh PowerShell**:
  ```powershell
  try {
      Invoke-WebRequest -Uri "http://localhost:8080/users" -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body '{"username":"testuser","password":"123"}'
  } catch {
      $stream = $_.Exception.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      Write-Output "HTTP Status: $($_.Exception.Response.StatusCode.value__)"
      Write-Output "Response: $($reader.ReadToEnd())"
  }
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `400 Bad Request`
  - Response Body:
    ```json
    {
      "code": 1004,
      "message": "Password must be at least {min} characters"
    }
    ```
- **Ý nghĩa kết quả**: Message validation khớp với `ErrorCode.INVALID_PASSWORD` (1004) và được chuyển đổi chính xác.

#### Trường hợp 7B: Gửi JSON hỏng cú pháp (Malformed JSON)
- **Lệnh PowerShell**:
  ```powershell
  try {
      Invoke-WebRequest -Uri "http://localhost:8080/users" -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body '{"username":"badjson",'
  } catch {
      $stream = $_.Exception.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      Write-Output "Response: $($reader.ReadToEnd())"
  }
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `400 Bad Request`
  - Response Body:
    ```json
    {
      "code": 1001,
      "message": "Malformed JSON request or invalid field format"
    }
    ```
- **Ý nghĩa kết quả**: Ngoại lệ `HttpMessageNotReadableException` được chặn an toàn, trả về `INVALID_KEY` (1001) thay vì lộ chi tiết cú pháp phân tích JSON.

---

### Bước 8: Kiểm tra Phân quyền truy cập theo Vai trò (Authorization / RBAC)
Kiểm chứng cơ chế phân quyền `@PreAuthorize("hasAuthority('ADMIN')")` trên endpoint tạo phòng (`POST /rooms`).

#### Kịch bản 8A: Người dùng thường (`USER`) cố gắng tạo phòng
- **Lệnh PowerShell**:
  ```powershell
  # Đăng nhập lấy token của user thường
  $userLogin = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" `
    -Method POST -Headers @{"Content-Type"="application/json"} `
    -Body '{"username":"user","password":"<USER_PASSWORD>"}'
  $userToken = $userLogin.result.token

  try {
      Invoke-WebRequest -Uri "http://localhost:8080/rooms" -Method POST `
        -Headers @{"Authorization"="Bearer $userToken"; "Content-Type"="application/json"} `
        -Body '{"number":"901","noOfPerson":2,"type":"SINGLE","price":120.0}'
  } catch {
      $stream = $_.Exception.Response.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($stream)
      Write-Output "HTTP Status: $($_.Exception.Response.StatusCode.value__)"
      Write-Output "Response: $($reader.ReadToEnd())"
  }
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `403 Forbidden`
  - Response Body:
    ```json
    {
      "code": 1023,
      "message": "Unauthorized"
    }
    ```
- **Ý nghĩa kết quả**: `AccessDeniedException` được kích hoạt do người dùng thiếu quyền `ADMIN`. `GlobalExceptionHandler` trả về chuẩn mã lỗi `UNAUTHORIZED` (`1023`) kèm mã HTTP 403 Forbidden.

#### Kịch bản 8B: Quản trị viên (`ADMIN`) tạo phòng thành công
- **Lệnh PowerShell**:
  ```powershell
  $createRoom = Invoke-RestMethod -Uri "http://localhost:8080/rooms" -Method POST `
    -Headers @{"Authorization"="Bearer $adminToken"; "Content-Type"="application/json"} `
    -Body '{"number":"901","noOfPerson":2,"type":"SINGLE","price":120.0}'
  Write-Output "Kết quả tạo phòng: Mã $($createRoom.code), Số phòng: $($createRoom.result.number)"
  ```
- **Kết quả kỳ vọng**:
  - HTTP Status: `200 OK`
  - Response Body:
    ```json
    {
      "code": 100,
      "result": {
        "id": 2,
        "number": "901",
        "noOfPerson": 2,
        "type": "SINGLE",
        "price": 120.0,
        "occupied": false
      }
    }
    ```

---

### Bước 9: Kiểm tra tính bất biến của cấu hình Seed Accounts
Kiểm tra đảm bảo việc khởi động lại ứng dụng không ghi đè hoặc làm thay đổi mật khẩu của tài khoản đã có.

- **Cách thực hiện an toàn**:
  1. Chạy suite unit test tự động để kiểm tra logic mà không cần sửa database:
     ```powershell
     .\mvnw.cmd test -Dtest=ConfigurationSecurityTest#testInitConfiguration_whenAccountsAlreadyExist_doesNotOverwrite
     ```
  2. Xác nhận test case PASS: Mock repository xác nhận `userRepository.save()` không bao giờ được gọi khi tài khoản `admin` và `user` đã tồn tại trong CSDL.

---

## 5. Bảng Danh mục Mã lỗi Chuẩn & Định dạng Phản hồi (Error Codes & API Format)

### Cấu trúc JSON chuẩn (`ApiResponse<T>`)
Mọi phản hồi REST API trong hệ thống đều tuân thủ cấu trúc thống nhất:
```json
{
  "code": 100,
  "message": "Optional descriptive message",
  "result": { ... }
}
```
- `code = 100`: Thao tác thành công.
- `code != 100`: Mã lỗi nghiệp vụ định nghĩa trong enum `ErrorCode`.
- `result`: Dữ liệu trả về (tự động loại bỏ khỏi JSON nếu `null`).

### Phân biệt rõ ràng giữa 401 Unauthorized và 403 Forbidden

- **HTTP 401 Unauthorized (`code: 1006 - Unauthenticated`)**:
  - Xảy ra khi yêu cầu gửi lên **chưa được xác thực danh tính**: Không có token, token hết hạn, token bị chỉnh sửa/sai chữ ký hoặc tài khoản đăng nhập sai.
  - Phản hồi từ: `JwtAuthenticationEntryPoint`.
- **HTTP 403 Forbidden (`code: 1023 - Unauthorized`)**:
  - Xảy ra khi yêu cầu **đã xác thực danh tính thành công**, nhưng tài khoản đó **không có đủ quyền hạn / vai trò** để thực thi hành động (ví dụ user thường cố tình gọi API CUD phòng).
  - Phản hồi từ: `GlobalExceptionHandler` bắt `AccessDeniedException`.

### Bảng tra cứu toàn bộ mã lỗi `ErrorCode` trong dự án

| Mã số (`code`) | Tên Hằng Enum | HTTP Status tương ứng | Ý nghĩa & Trường hợp phát sinh |
| :---: | :--- | :--- | :--- |
| `1001` | `INVALID_KEY` | `400 BAD_REQUEST` | Khóa validation không xác định, JSON sai cú pháp hoặc enum không hợp lệ |
| `1002` | `USER_EXISTED` | `400 BAD_REQUEST` | Tên người dùng đã tồn tại trong hệ thống |
| `1003` | `USERNAME_INVALID` | `400 BAD_REQUEST` | Tên đăng nhập không đạt yêu cầu độ dài tối thiểu |
| `1004` | `INVALID_PASSWORD` | `400 BAD_REQUEST` | Mật khẩu không đạt yêu cầu độ dài (tối thiểu 6 ký tự) |
| `1005` | `USER_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy tài khoản người dùng tương ứng |
| `1006` | `UNAUTHENTICATED` | `401 UNAUTHORIZED` | Yêu cầu chưa được xác thực hoặc token không hợp lệ |
| `1007` | `ROOM_EXISTED` | `400 BAD_REQUEST` | Số phòng đã tồn tại trong CSDL khi tạo mới |
| `1008` | `ROOM_NOT_EXISTED` | `404 NOT_FOUND` | Không tìm thấy phòng theo ID hoặc số phòng yêu cầu |
| `1009` | `ROOM_UPDATE_EMPTY` | `400 BAD_REQUEST` | Yêu cầu PATCH cập nhật phòng nhưng không truyền trường nào |
| `1010` | `ROOM_NUMBER_REQUIRED` | `400 BAD_REQUEST` | Số phòng bị để trống khi tạo mới |
| `1011` | `ROOM_NUMBER_TOO_LONG` | `400 BAD_REQUEST` | Số phòng vượt quá độ dài tối đa 20 ký tự |
| `1012` | `ROOM_CAPACITY_INVALID` | `400 BAD_REQUEST` | Số lượng khách của phòng nhỏ hơn 1 |
| `1013` | `ROOM_TYPE_REQUIRED` | `400 BAD_REQUEST` | Loại phòng bị để trống hoặc sai kiểu |
| `1014` | `ROOM_PRICE_INVALID` | `400 BAD_REQUEST` | Giá phòng nhỏ hơn hoặc bằng 0 |
| `1022` | `ROOM_CANNOT_DELETE_OCCUPIED` | `400 BAD_REQUEST` | Từ chối xóa phòng khi phòng đang có khách (`occupied == true`) |
| `1023` | `UNAUTHORIZED` | `403 FORBIDDEN` | Đã đăng nhập nhưng không đủ quyền hạn thực hiện hành động |
| `9999` | `UNCATEGORIZED_EXCEPTION` | `500 INTERNAL_SERVER_ERROR` | Lỗi hệ thống chưa được phân loại cụ thể |

---

## 6. Giới hạn Kỹ thuật & Danh mục Rủi ro Tồn đọng (Limitations & Outstanding Risks)

Để đảm bảo tính minh bạch trong kiểm toán bảo mật, dưới đây là danh mục phân định rõ ràng giữa những gì đã hoàn thành và những điểm còn tồn đọng:

### ✅ Các nội dung đã hoàn thành & Đã kiểm chứng thực tế
- [x] Đồng bộ hóa JWT signing key thông qua cấu hình Spring `@Value("${jwt.signerKey}")`.
- [x] Bắt lỗi validation an toàn, không để lộ thông tin nhạy cảm hoặc gây HTTP 500 ngoài ý muốn.
- [x] Chuẩn hóa toàn bộ mã lỗi `ErrorCode` thành các giá trị số duy nhất (tách 1006 và 1023).
- [x] Tạo profile `prod` không fallback cho toàn bộ secret nhạy cảm; kích hoạt kiểm tra fail-fast.
- [x] Kiểm tra độ dài khóa JWT tối thiểu 32 bytes UTF-8 và đồng bộ hóa `StandardCharsets.UTF_8` trên toàn bộ luồng JWT.
- [x] Khóa chặn tài khoản seed mặc định trên Production; kiểm chứng tính bất biến không ghi đè tài khoản hiện có.
- [x] Suite 17 kiểm thử tự động (16 unit tests + 1 integration test) đạt kết quả 100% PASS.

### ⚠️ Các rủi ro chấp nhận tạm thời cho môi trường Local
- **Mật khẩu CSDL local trong Git**: Thuộc tính fallback `spring.datasource.password=${DB_PASSWORD:...}` trong `application.properties` được giữ lại để đảm bảo ứng dụng chạy bình thường trên máy tính của các thành viên. Mật khẩu này được xem như đã lộ công khai và **bắt buộc phải rotate khi triển khai production**.
- **Khóa ký JWT lịch sử trong Git**: Chuỗi fallback trong `application.properties` được giữ lại cho local dev; môi trường production tuyệt đối cấm dùng lại chuỗi này.

### ⏳ Các hạng mục kiểm thử triển khai chưa thực hiện
- **Kiểm thử runtime Container Docker**: Cú pháp `Dockerfile` đã được chuẩn hóa, nhưng image Docker chưa được build và chạy thử nghiệm thực tế trong môi trường Docker daemon.
- **Kết nối TLS tới CSDL Production**: Cấu hình `useSSL=true` đã được khuyến nghị trong tài liệu, nhưng chưa được kiểm thử kết nối với một cụm MySQL server có cài đặt chứng chỉ SSL/TLS thực tế.
- **Cấp phát biến môi trường trên nền tảng Cloud/CI-CD**: Việc nạp các biến môi trường bí mật qua Docker Secrets / K8s Secrets phụ thuộc vào hạ tầng triển khai thực tế của đội ngũ vận hành.
