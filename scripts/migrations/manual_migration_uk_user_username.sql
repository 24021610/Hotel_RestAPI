-- ==============================================================================
-- MIGRATION SCRIPT: Thêm UNIQUE Constraint `uk_user_username` cho bảng `user`
-- Dự án: Hotel_RestAPI (Nhánh: TungBeng)
-- File: scripts/migrations/manual_migration_uk_user_username.sql
-- Mục đích: Đảm bảo tính duy nhất của username ở tầng Database, ngăn chặn race condition.
-- Lưu ý: KHÔNG TỰ ĐỘNG THỰC THI. File này dùng để chạy thủ công hoặc tích hợp CI/CD.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- PHẦN 1: KIỂM TRA ĐIỀU KIỆN TIÊN QUYẾT (PRE-CONDITIONS CHECK)
-- Trước khi thêm constraint, BẮT BUỘC kiểm tra các điều kiện sau:
-- ------------------------------------------------------------------------------

-- Bước 1.1: Kiểm tra xem có username nào đang bị trùng lặp hay không (Kết quả phải = 0 dòng)
SELECT username, COUNT(*) AS duplicate_count
FROM `user`
WHERE username IS NOT NULL
GROUP BY username
HAVING COUNT(*) > 1;

-- Bước 1.2: Kiểm tra các bản ghi đang có username = NULL
-- (Trong MySQL InnoDB, UNIQUE constraint cho phép nhiều NULL nếu cột không có NOT NULL;
--  tuy nhiên về mặt nghiệp vụ cần xác nhận số lượng bản ghi NULL trước khi chạy).
SELECT id, username, first_name, last_name, roles
FROM `user`
WHERE username IS NULL;

-- Bước 1.3: Kiểm tra xem index/constraint `uk_user_username` đã tồn tại trước đó chưa
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user'
  AND CONSTRAINT_NAME = 'uk_user_username';

-- ------------------------------------------------------------------------------
-- PHẦN 2: CÂU LỆNH THỰC THI MIGRATION (EXECUTION)
-- Chỉ thực thi khi Bước 1.1 trả về 0 dòng trùng lặp và Bước 1.3 chưa tồn tại constraint.
-- ------------------------------------------------------------------------------

-- Thêm UNIQUE constraint trên cột `username` (không bắt buộc NOT NULL để tương thích với bản ghi NULL hiện có)
ALTER TABLE `user`
ADD CONSTRAINT `uk_user_username` UNIQUE (`username`);

-- ------------------------------------------------------------------------------
-- PHẦN 3: XÁC MINH SAU KHI CHẠY (POST-VERIFICATION)
-- ------------------------------------------------------------------------------

-- Kiểm tra chỉ mục vừa tạo thành công trong bảng `user`
SHOW INDEX FROM `user` WHERE Key_name = 'uk_user_username';

-- ------------------------------------------------------------------------------
-- PHẦN 4: KỊCH BẢN HOÀN TÁC (ROLLBACK PROCEDURE)
-- Sử dụng khi cần khôi phục lại cấu trúc bảng như trước khi chạy migration
-- ------------------------------------------------------------------------------

-- Lệnh xóa Unique Index:
-- ALTER TABLE `user` DROP INDEX `uk_user_username`;

