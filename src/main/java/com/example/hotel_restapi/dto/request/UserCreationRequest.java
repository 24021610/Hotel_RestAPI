package com.example.hotel_restapi.dto.request;

import com.example.hotel_restapi.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)


public class UserCreationRequest {
    String id;
    private String username;
    @Size(min = 6, max = 20, message = "INVALID_PASSWORD")
    String password;
    String firstName;
    String lastName;
    Set<String> roles;
}
