
package com.example.hotel_restapi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.hotel_restapi.dto.request.ApiResponse;
import com.example.hotel_restapi.dto.request.RoomCreationRequest;
import com.example.hotel_restapi.dto.request.RoomUpdateRequest;
import com.example.hotel_restapi.dto.response.RoomResponse;
import com.example.hotel_restapi.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ApiResponse<RoomResponse> createRoom(
            @RequestBody @Valid RoomCreationRequest request) {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.createRoom(request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<RoomResponse>> getRooms() {
        return ApiResponse.<List<RoomResponse>>builder()
                .result(roomService.getRooms())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomResponse> getRoomById(
            @PathVariable Long id) {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.getRoomById(id))
                .build();
    }

    @GetMapping("/number/{number}")
    public ApiResponse<RoomResponse> getRoomByNumber(
            @PathVariable String number) {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.getRoomByNumber(number))
                .build();
    }

    @GetMapping("/available")
    public ApiResponse<List<RoomResponse>> getAvailableRooms() {
        return ApiResponse.<List<RoomResponse>>builder()
                .result(roomService.getAvailableRooms())
                .build();
    }

    @GetMapping("/occupied")
    public ApiResponse<List<RoomResponse>> getOccupiedRooms() {
        return ApiResponse.<List<RoomResponse>>builder()
                .result(roomService.getOccupiedRooms())
                .build();
    }

    @PatchMapping("/{id}")
    public ApiResponse<RoomResponse> updateRoom(
            @PathVariable Long id,
            @RequestBody @Valid RoomUpdateRequest request) {
        return ApiResponse.<RoomResponse>builder()
                .result(roomService.updateRoom(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteRoom(
            @PathVariable Long id) {
        roomService.deleteRoom(id);

        return ApiResponse.<String>builder()
                .message("Room deleted successfully")
                .result("Room " + id + " has been deleted")
                .build();
    }
}
