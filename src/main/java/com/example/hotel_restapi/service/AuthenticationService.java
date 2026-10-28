package com.example.hotel_restapi.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.hotel_restapi.dto.request.AuthenticationRequest;
import com.example.hotel_restapi.dto.request.IntrospectRequest;
import com.example.hotel_restapi.dto.response.AuthenticationResponse;
import com.example.hotel_restapi.dto.response.IntrospectResponse;
import com.example.hotel_restapi.entity.User;
import com.example.hotel_restapi.exception.AppException;
import com.example.hotel_restapi.exception.ErrorCode;
import com.example.hotel_restapi.repository.UserRepository;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;

    public IntrospectResponse introspect(IntrospectRequest request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            return IntrospectResponse.builder().valid(false).build();
        }

        boolean isValid = false;
        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes(StandardCharsets.UTF_8));
            SignedJWT signedJWT = SignedJWT.parse(request.getToken().trim());
            boolean verified = signedJWT.verify(verifier);
            Date expiryTime = signedJWT.getJWTClaimsSet() != null
                    ? signedJWT.getJWTClaimsSet().getExpirationTime()
                    : null;
            isValid = verified && expiryTime != null && expiryTime.after(new Date());
        } catch (Exception e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        if (authenticationRequest == null
                || authenticationRequest.getUsername() == null
                || authenticationRequest.getUsername().isBlank()
                || authenticationRequest.getPassword() == null
                || authenticationRequest.getPassword().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var user = userRepository.findByUsername(authenticationRequest.getUsername().trim())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var password = authenticationRequest.getPassword();
        boolean authenticated = passwordEncoder.matches(password, user.getPassword());
        if (!authenticated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateJWTToken(user);
        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    private String generateJWTToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes(StandardCharsets.UTF_8)));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(stringJoiner::add);
        }
        return stringJoiner.toString();
    }
}
