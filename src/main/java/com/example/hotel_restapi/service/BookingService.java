package com.example.hotel_restapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hotel_restapi.dto.request.BookingCreationRequest;
import com.example.hotel_restapi.dto.response.BookingResponse;
import com.example.hotel_restapi.entity.Booking;
import com.example.hotel_restapi.entity.Room;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.enums.BookingStatus;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.repository.BookingRepository;
import com.example.hotel_restapi.repository.RoomRepository;
import com.example.hotel_restapi.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {

    BookingRepository bookingRepository;
    RoomRepository roomRepository;
    UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(
            BookingCreationRequest request,
            String username) {
        validateDateRange(request.getCheckIn(), request.getCheckOut());

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_EXISTED));

        if (bookingRepository.existsOverlappingActiveBooking(
                room.getId(),
                request.getCheckIn(),
                request.getCheckOut(),
                BookingStatus.CONFIRMED)) {
            throw new AppException(ErrorCode.BOOKING_ROOM_UNAVAILABLE);
        }

        long numberOfNights = ChronoUnit.DAYS.between(
                request.getCheckIn(),
                request.getCheckOut());
        BigDecimal totalCost = BigDecimal.valueOf(room.getPrice())
                .multiply(BigDecimal.valueOf(numberOfNights))
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoom(room);
        booking.setCheckIn(request.getCheckIn());
        booking.setCheckOut(request.getCheckOut());
        booking.setTotalCost(totalCost);
        booking.setStatus(BookingStatus.CONFIRMED);

        return toBookingResponse(bookingRepository.save(booking));
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            throw new AppException(ErrorCode.BOOKING_DATE_INVALID);
        }
    }

    private BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .roomId(booking.getRoom().getId())
                .checkIn(booking.getCheckIn())
                .checkOut(booking.getCheckOut())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .build();
    }
}
