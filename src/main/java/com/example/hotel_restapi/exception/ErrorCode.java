package com.example.hotel_restapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Uncategorized error", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1006, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    ROOM_EXISTED(1007, "Room number already exists", HttpStatus.BAD_REQUEST),
    ROOM_NOT_EXISTED(1008, "Room not found", HttpStatus.NOT_FOUND),
    ROOM_UPDATE_EMPTY(1009, "At least one room field must be provided", HttpStatus.BAD_REQUEST),
    ROOM_NUMBER_REQUIRED(1010, "Room number must not be blank", HttpStatus.BAD_REQUEST),
    ROOM_NUMBER_TOO_LONG(1011, "Room number must not exceed 20 characters", HttpStatus.BAD_REQUEST),
    ROOM_CAPACITY_INVALID(1012, "Number of persons must be at least 1", HttpStatus.BAD_REQUEST),
    ROOM_TYPE_REQUIRED(1013, "Room type is required", HttpStatus.BAD_REQUEST),
    ROOM_PRICE_INVALID(1014, "Room price must be greater than 0", HttpStatus.BAD_REQUEST),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}