
package com.example.hotel_restapi.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.example.hotel_restapi.dto.request.RoomCreationRequest;
import com.example.hotel_restapi.dto.request.RoomUpdateRequest;
import com.example.hotel_restapi.dto.response.RoomResponse;
import com.example.hotel_restapi.entity.Room;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "occupied", ignore = true)
    Room toRoom(RoomCreationRequest request);

    RoomResponse toRoomResponse(Room room);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "occupied", ignore = true)
    void updateRoom(
            @MappingTarget Room room,
            RoomUpdateRequest request);
}
