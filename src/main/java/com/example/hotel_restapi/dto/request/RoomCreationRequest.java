
package com.example.hotel_restapi.dto.request;

import com.example.hotel_restapi.enums.RoomType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomCreationRequest {

    @NotBlank(message = "ROOM_NUMBER_REQUIRED")
    @Size(max = 20, message = "ROOM_NUMBER_TOO_LONG")
    String number;

    @NotNull(message = "ROOM_CAPACITY_INVALID")
    @Min(value = 1, message = "ROOM_CAPACITY_INVALID")
    Integer noOfPerson;

    @NotNull(message = "ROOM_TYPE_REQUIRED")
    RoomType type;

    @NotNull(message = "ROOM_PRICE_INVALID")
    @Positive(message = "ROOM_PRICE_INVALID")
    Double price;
}
