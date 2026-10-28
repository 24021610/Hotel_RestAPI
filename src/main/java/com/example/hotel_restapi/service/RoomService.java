package com.example.hotel_restapi.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.example.hotel_restapi.dto.request.RoomCreationRequest;
import com.example.hotel_restapi.dto.request.RoomUpdateRequest;
import com.example.hotel_restapi.dto.response.RoomResponse;
import com.example.hotel_restapi.entity.Room;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.mapper.RoomMapper;
import com.example.hotel_restapi.repository.RoomRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomService {

    RoomRepository roomRepository;
    RoomMapper roomMapper;

    @PreAuthorize("hasAuthority('ADMIN')")
    public RoomResponse createRoom(RoomCreationRequest request) {
        if (request.getNumber() == null || request.getNumber().isBlank()) {
            throw new AppException(ErrorCode.ROOM_NUMBER_REQUIRED);
        }
        String number = request.getNumber().trim();

        if (roomRepository.existsByNumber(number)) {
            throw new AppException(ErrorCode.ROOM_EXISTED);
        }

        Room room = roomMapper.toRoom(request);
        room.setNumber(number);
        room.setOccupied(false);

        return roomMapper.toRoomResponse(
                roomRepository.save(room));
    }

    public List<RoomResponse> getRooms() {
        return roomRepository.findAll()
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    public RoomResponse getRoomById(Long id) {
        return roomMapper.toRoomResponse(findRoom(id));
    }

    public RoomResponse getRoomByNumber(String number) {
        if (number == null || number.isBlank()) {
            throw new AppException(ErrorCode.ROOM_NOT_EXISTED);
        }
        Room room = roomRepository.findByNumber(number.trim())
                .orElseThrow(
                        () -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        return roomMapper.toRoomResponse(room);
    }

    public List<RoomResponse> getOccupiedRooms() {
        return roomRepository.findByOccupiedTrue()
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    public List<RoomResponse> getAvailableRooms() {
        return roomRepository.findByOccupiedFalse()
                .stream()
                .map(roomMapper::toRoomResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public RoomResponse updateRoom(
            Long id,
            RoomUpdateRequest request) {
        Room room = findRoom(id);

        if (request.getNumber() == null
                && request.getNoOfPerson() == null
                && request.getType() == null
                && request.getPrice() == null) {
            throw new AppException(ErrorCode.ROOM_UPDATE_EMPTY);
        }

        if (request.getNumber() != null) {
            String number = request.getNumber().trim();

            roomRepository.findByNumber(number)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new AppException(ErrorCode.ROOM_EXISTED);
                    });

            request.setNumber(number);
        }

        roomMapper.updateRoom(room, request);

        return roomMapper.toRoomResponse(
                roomRepository.save(room));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    public void deleteRoom(Long id) {
        Room room = findRoom(id);
        if (room.isOccupied()) {
            throw new AppException(ErrorCode.ROOM_CANNOT_DELETE_OCCUPIED);
        }
        roomRepository.delete(room);
    }

    private Room findRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(
                        () -> new AppException(ErrorCode.ROOM_NOT_EXISTED));
    }
}
