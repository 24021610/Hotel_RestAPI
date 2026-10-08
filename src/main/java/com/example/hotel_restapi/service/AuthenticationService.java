package com.example.hotel_restapi.service;


import com.example.hotel_restapi.dto.request.AuthenticationRequest;
import com.example.hotel_restapi.dto.request.IntrospectRequest;
import com.example.hotel_restapi.dto.response.AuthenticationResponse;
import com.example.hotel_restapi.dto.response.IntrospectResponse;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.repository.UserRepository;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.*;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class AuthenticationService {
    private String key = "df31780bf9c65415d7a7de7f614ea6c437551148bd43a858a790e898a078e2e4";

    UserRepository userRepository;



    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();
        JWSVerifier verifier = new MACVerifier(key.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        var verifiedJWT = signedJWT.verify(verifier);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        return IntrospectResponse.builder().
                valid(verifiedJWT && expiryTime.after(new Date())).
                build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        var user = userRepository.findByUsername(authenticationRequest.getUsername()).orElseThrow(()
                -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var password = authenticationRequest.getPassword();
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean authenticated = encoder.matches(password, user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        } else {
            var token = generateJWTToken(user.getUsername());
            return AuthenticationResponse.builder().token(token).authenticated(true).build();
        }
    }

    private String generateJWTToken(String username) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer("Hotel")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                .build();
        Payload payload  = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(key.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
