package com.example.hotel_restapi.service;

import java.util.HashSet;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        String username = request.getUsername() != null ? request.getUsername().trim() : null;
        if (username != null && userRepository.existsByusername(username)) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User newUser = userMapper.toUser(request);
        newUser.setId(null);
        newUser.setUsername(username);
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

    @PostAuthorize("hasAuthority('ADMIN') or returnObject.username == authentication.name")
    public UserResponse getUserById(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    @PreAuthorize("hasAuthority('ADMIN') or #username == authentication.name")
    public UserResponse getUserByUsername(String username) {
        return userMapper.toUserResponse(
                userRepository.findByUsername(username)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));
        boolean isSelf = authentication != null && user.getUsername().equals(authentication.getName());

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("Unauthorized");
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            if (!isAdmin) {
                throw new AccessDeniedException("Unauthorized");
            }
            user.setRoles(request.getRoles());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userMapper.updateUser(user, request);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteUser(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userRepository.delete(user);
    }
}
