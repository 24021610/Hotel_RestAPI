
package com.example.hotel_restapi.dto.response;

import com.example.hotel_restapi.enums.RoomType;

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
public class RoomResponse {

    Long id;
    String number;
    Integer noOfPerson;
    RoomType type;
    double price;
    boolean occupied;
}
