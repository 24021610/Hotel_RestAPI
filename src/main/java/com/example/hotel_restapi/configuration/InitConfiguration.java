package com.example.hotel_restapi.configuration;

import java.util.Arrays;
import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.enums.Role;
import com.example.hotel_restapi.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InitConfiguration {

    static final String ADMIN_NAME = "admin";

    PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    @NonFinal
    Environment environment;

    void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public ApplicationRunner applicationRunner(
            UserRepository userRepository,
            @Value("${app.init.enabled:true}") boolean initEnabled,
            @Value("${app.init.admin-password:admin}") String adminPassword,
            @Value("${app.init.user-password:user}") String userPassword) {
        return args -> {
            if (!initEnabled) {
                log.info("Default seed accounts initialization is disabled (app.init.enabled=false).");
                return;
            }

            boolean isProd = false;
            if (environment != null && environment.getActiveProfiles() != null) {
                var profiles = Arrays.asList(environment.getActiveProfiles());
                isProd = profiles.contains("prod") || profiles.contains("production");
            }

            if (isProd) {
                if (adminPassword == null || adminPassword.isBlank() || "admin".equalsIgnoreCase(adminPassword.trim())) {
                    throw new IllegalStateException("Production seed initialization cannot use default or blank admin password. Please set APP_INIT_ADMIN_PASSWORD.");
                }
                if (userPassword == null || userPassword.isBlank() || "user".equalsIgnoreCase(userPassword.trim())) {
                    throw new IllegalStateException("Production seed initialization cannot use default or blank user password. Please set APP_INIT_USER_PASSWORD.");
                }
            }

            if (userRepository.findByUsername(ADMIN_NAME).isEmpty()) {
                if (!isProd && "admin".equalsIgnoreCase(adminPassword)) {
                    log.warn("Seeding admin account with default password. Do NOT use default credentials in production!");
                }
                var roles = new HashSet<String>();
                roles.add(Role.ADMIN.name());
                User user = User.builder()
                        .username(ADMIN_NAME)
                        .password(passwordEncoder.encode(adminPassword))
                        .roles(roles)
                        .build();
                userRepository.save(user);
                log.info("Admin account initialized successfully.");
            }

            if (userRepository.findByUsername("user").isEmpty()) {
                var userRoles = new HashSet<String>();
                userRoles.add(Role.USER.name());
                User normalUser = User.builder()
                        .username("user")
                        .password(passwordEncoder.encode(userPassword))
                        .roles(userRoles)
                        .build();
                userRepository.save(normalUser);
                log.info("User account initialized successfully.");
            }
        };
    }
}
