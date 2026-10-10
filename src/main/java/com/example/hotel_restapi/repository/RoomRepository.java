
package com.example.hotel_restapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.hotel_restapi.entity.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByNumber(String number);

    boolean existsByNumber(String number);

    List<Room> findByOccupiedTrue();

    List<Room> findByOccupiedFalse();
}
