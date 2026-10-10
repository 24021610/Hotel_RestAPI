
package com.example.hotel_restapi.dto.request;

import com.example.hotel_restapi.enums.RoomType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
public class RoomUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "ROOM_NUMBER_REQUIRED")
    @Size(max = 20, message = "ROOM_NUMBER_TOO_LONG")
    String number;

    @Min(value = 1, message = "ROOM_CAPACITY_INVALID")
    Integer noOfPerson;

    RoomType type;

    @Positive(message = "ROOM_PRICE_INVALID")
    Double price;
}
