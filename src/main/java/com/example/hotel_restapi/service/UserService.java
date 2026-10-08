package com.example.hotel_restapi.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.example.hotel_restapi.dto.request.UserCreationRequest;
import com.example.hotel_restapi.dto.request.UserUpdateRequest;
import com.example.hotel_restapi.dto.response.UserResponse;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.enums.Role;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.mapper.UserMapper;
import com.example.hotel_restapi.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.HashSet;
import java.util.List;

/*
 * Service xử lý các nghiệp vụ logic liên quan đến tài khoản người dùng trong hệ thống Hotel REST API.
 */


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;


    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByusername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User newUser = userMapper.toUser(request);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<String> roles = new HashSet<String>();
        roles.add(Role.USER.name());
        newUser.setRoles(roles);
        return userMapper.toUserResponse(userRepository.save(newUser));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PostAuthorize("hasAuthority('ADMIN')")
    public UserResponse getUserById(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    @PostAuthorize("hasAuthority('ADMIN')")
    public UserResponse getUserByUsername(String username) {
        return userMapper.toUserResponse(
                userRepository.findByUsername(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    // Đảm bảo user chỉ có thể lấy thông tin của chính mình 
    @PostAuthorize("returnObject.username == authentication.name")
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String id) {
        userRepository.deleteById(id);

    }
}
