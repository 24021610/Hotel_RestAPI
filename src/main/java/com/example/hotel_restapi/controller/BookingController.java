package com.example.hotel_restapi.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotel_restapi.dto.request.ApiResponse;
import com.example.hotel_restapi.dto.request.BookingCreationRequest;
import com.example.hotel_restapi.dto.response.BookingResponse;
import com.example.hotel_restapi.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @RequestBody @Valid BookingCreationRequest request,
            Authentication authentication) {
        BookingResponse booking = bookingService.createBooking(
                request,
                authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<BookingResponse>builder()
                        .result(booking)
                        .build());
    }

    @GetMapping
    public ApiResponse<List<BookingResponse>> getMyBookings(
            Authentication authentication) {
        return ApiResponse.<List<BookingResponse>>builder()
                .result(bookingService.getMyBookings(authentication.getName()))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.getBookingById(id, authentication.getName()))
                .build();
    }
}
