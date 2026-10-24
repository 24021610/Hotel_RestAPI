package com.example.hotel_restapi.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreationRequest {

    @NotNull(message = "BOOKING_ROOM_REQUIRED")
    Long roomId;

    @NotNull(message = "BOOKING_CHECK_IN_REQUIRED")
    LocalDate checkIn;

    @NotNull(message = "BOOKING_CHECK_OUT_REQUIRED")
    LocalDate checkOut;
}
