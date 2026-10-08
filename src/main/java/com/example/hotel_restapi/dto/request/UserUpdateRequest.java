package com.example.hotel_restapi.dto.request;
import com.example.hotel_restapi.enums.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class UserUpdateRequest {
    String id;
    String username;
    String password;
    String firstName;
    String lastName;
    Set<Role> roles;
}
