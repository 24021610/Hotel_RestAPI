package com.example.hotel_restapi.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.example.hotel_restapi.dto.request.UserCreationRequest;
import com.example.hotel_restapi.dto.request.UserUpdateRequest;
import com.example.hotel_restapi.dto.response.UserResponse;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.mapper.UserMapper;
import com.example.hotel_restapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.List;

/*
 * Service xử lý các nghiệp vụ logic liên quan đến tài khoản người dùng trong hệ thống Hotel REST API.
 */


@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    public UserResponse createUser(UserCreationRequest user) {
        User newUser = userMapper.toUser(user);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(8);
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));

        if (userRepository.existsByusername(user.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        return userMapper.toUserResponse(userRepository.save(newUser));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);

    }
}
