package com.example.hotel_restapi.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.hotel_restapi.entity.Booking;
import com.example.hotel_restapi.enums.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(String userId);

    Optional<Booking> findByIdAndUserUsername(Long id, String username);

    @Query("""
            select case when count(b) > 0 then true else false end
            from Booking b
            where b.room.id = :roomId
              and b.status = :activeStatus
              and b.checkIn < :checkOut
              and b.checkOut > :checkIn
            """)
    boolean existsOverlappingActiveBooking(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("activeStatus") BookingStatus activeStatus);
}
