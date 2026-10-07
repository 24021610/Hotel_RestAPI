package com.example.hotel_restapi.controller;


import com.example.hotel_restapi.dto.request.ApiResponse;
import com.example.hotel_restapi.dto.request.AuthenticationRequest;
import com.example.hotel_restapi.dto.request.IntrospectRequest;
import com.example.hotel_restapi.dto.response.AuthenticationResponse;
import com.example.hotel_restapi.dto.response.IntrospectResponse;
import com.example.hotel_restapi.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.text.ParseException;

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
        AuthenticationResponse authResponse = authenticationService.authenticate(authenticationRequest);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authResponse)
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }
}
