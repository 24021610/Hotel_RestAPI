package com.example.hotel_restapi.configuration;

import java.util.HashSet;

import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.enums.Role;
import com.example.hotel_restapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InitConfiguration {

    static final String ADMIN_NAME = "admin";
    static final String ADMIN_PASSWORD = "admin";

    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            if (userRepository.findByUsername(ADMIN_NAME).isEmpty()) {

                var roles = new HashSet<Role>();
                roles.add(Role.ADMIN);

                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode(ADMIN_PASSWORD))
                        .roles(roles)
                        .build();

                userRepository.save(user);
            }
        };
    }
}