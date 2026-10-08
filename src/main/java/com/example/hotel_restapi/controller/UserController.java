package com.example.hotel_restapi.controller;

import com.example.hotel_restapi.dto.request.ApiResponse;
import com.example.hotel_restapi.dto.request.UserCreationRequest;
import com.example.hotel_restapi.dto.request.UserUpdateRequest;
import com.example.hotel_restapi.dto.response.UserResponse;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    // POST /users: Tạo mới user (dùng cho admin để quản lí cơ sở dữ liệu, khác đăng ký)
    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    // GET /users: Lấy danh sách user
    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    // GET /users/{userId}: Lấy chi tiết user
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    // PUT /users/{userId}: Cập nhật user
    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(
            @PathVariable("userId") String userId,
            @RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    // DELETE /users/{userId}: Xóa user
    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .message("User deleted successfully")
                .result("User has been deleted")
                .build();
    }
}