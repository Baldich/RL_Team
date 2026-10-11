package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.ApiError;
import com.tecnocampus.LS2.protube_back.services.DuplicateEmailException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = RegistrationController.class)
public class RegistrationExceptionHandler {
    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> duplicate(DuplicateEmailException exception) {
        return ResponseEntity.status(409).body(new ApiError(exception.getMessage(), Map.of("email", exception.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Check the registration fields.", errors));
    }
}
