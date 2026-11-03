package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.ApiError;
import com.tecnocampus.LS2.protube_back.services.UploadException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import java.util.Map;

/** Multipart parsing may fail before a controller has been selected. */
@RestControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(UploadException.class)
    ResponseEntity<ApiError> uploadError(UploadException exception) {
        return ResponseEntity.status(exception.status()).body(new ApiError(exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(413).body(new ApiError("Video must be at most 100 MB.", Map.of()));
    }

    @ExceptionHandler(MultipartException.class)
    ResponseEntity<ApiError> invalidMultipart(MultipartException exception) {
        return ResponseEntity.badRequest().body(new ApiError("Unable to read the upload. Please try again.", Map.of()));
    }
}
