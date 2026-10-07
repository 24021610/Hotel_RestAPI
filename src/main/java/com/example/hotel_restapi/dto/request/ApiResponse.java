package com.example.hotel_restapi.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Class dùng để chuẩn hóa cấu trúc phản hồi cho mọi REST API trong dự án.
 * Tất cả các API trả về đều sẽ có chung format JSON: { code, message, result }
 *
 * Ý nghĩa các trường:
 * - code: Mã trạng thái nội bộ (mặc định 100 là thành công, khác 100 là mã lỗi tự quy ước trong ErrorCode).
 * - message: Thông báo mô tả (thành công hoặc lý do lỗi).
 * - result: Dữ liệu trả về (kiểu generic T). Nếu null thì tự động không hiển thị trong JSON.
 *
 * Cách dùng trong Controller:
 *
 * 1. API trả về dữ liệu:
 *    return ApiResponse.<UserResponse>builder()
 *            .result(userService.getUser(id))
 *            .build();
 *    -> Output JSON: { "code": 100, "result": { ... } }
 *
 * 2. API không cần trả dữ liệu, chỉ cần message (Xóa, cập nhật...):
 *    return ApiResponse.<Void>builder()
 *            .message("Xóa thành công")
 *            .build();
 *    -> Output JSON: { "code": 100, "message": "Xóa thành công" }
 *
 * 3. Khi bắt lỗi (trong GlobalExceptionHandler):
 *    return ApiResponse.<Void>builder()
 *            .code(1001)
 *            .message("Tài khoản không tồn tại")
 *            .build();
 *    -> Output JSON: { "code": 1001, "message": "Tài khoản không tồn tại" }
 */


@Builder
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor

public class ApiResponse <T> {
    @Builder.Default
    private int code = 100;
    private String message;
    private T result;
}
