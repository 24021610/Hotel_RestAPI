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
    public ApiResponse<User> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResponse<User> response = new ApiResponse<>();
        response.setResult(userService.createUser(request));
        return response;
    }

    // GET /users: Lấy danh sách user
    @GetMapping
    public ApiResponse<List<User>> getUsers() {
        ApiResponse<List<User>> response = new ApiResponse<>();
        response.setResult(userService.getUsers());
        return response;
    }

    // GET /users/{userId}: Lấy chi tiết user
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        ApiResponse<UserResponse> response = new ApiResponse<>();
        response.setResult(userService.getUser(userId));
        return response;
    }

    // PUT /users/{userId}: Cập nhật user
    @PutMapping("/{userId}")
    public ApiResponse<User> updateUser(
            @PathVariable("userId") String userId,
            @RequestBody @Valid UserUpdateRequest request) {
        ApiResponse<User> response = new ApiResponse<>();
        response.setResult(userService.updateUser(userId, request));
        return response;
    }

    // DELETE /users/{userId}: Xóa user
    @DeleteMapping("/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("User has been deleted successfully");
        return apiResponse;
    }
}