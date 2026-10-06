package com.example.hotel_restapi.controller;


import com.example.hotel_restapi.dto.request.ApiResponse;
import com.example.hotel_restapi.dto.request.AuthenticationRequest;
import com.example.hotel_restapi.dto.response.AuthenticationResponse;
import com.example.hotel_restapi.service.AuthenticationService;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationController {
    AuthenticationService authenticationService;
    private final JsonMapper.Builder builder;


    // POST /auth: Login dựa trên tên và mật khẩu
    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest authenticationRequest) {
        boolean auth = authenticationService.authenticate(authenticationRequest);
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .authenticated(auth)
                .build();

        return ApiResponse.<AuthenticationResponse>builder()
                .result(authResponse)
                .build();
    }
}
