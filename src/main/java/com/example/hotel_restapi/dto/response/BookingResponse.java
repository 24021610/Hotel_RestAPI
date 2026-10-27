package com.example.hotel_restapi.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.hotel_restapi.enums.BookingStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {

    Long id;
    String userId;
    Long roomId;
    LocalDate checkIn;
    LocalDate checkOut;
    BigDecimal totalCost;
    BookingStatus status;
}
