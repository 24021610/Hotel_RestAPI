package com.example.hotel_restapi.repository;

import com.example.hotel_restapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    boolean existsById(String id);
    boolean existsByusername(String userName);
    Optional<User> findByusername(String userName);
}
