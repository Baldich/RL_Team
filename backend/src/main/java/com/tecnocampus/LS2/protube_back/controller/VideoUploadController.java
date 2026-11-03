package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.*;
import com.tecnocampus.LS2.protube_back.services.VideoUploadService;
import com.tecnocampus.LS2.protube_back.services.UploadException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class VideoUploadController {
    private final VideoUploadService uploads;
    public VideoUploadController(VideoUploadService uploads) { this.uploads = uploads; }

    @PostMapping(value = "/api/videos", consumes = "multipart/form-data")
    public ResponseEntity<VideoDetailResponse> upload(@AuthenticationPrincipal RegistrationResponse user,
            @RequestPart("file") MultipartFile file, @Valid @RequestPart("metadata") VideoUploadRequest metadata) {
        if (user == null) throw new UploadException(HttpStatus.UNAUTHORIZED, "Please log in to upload.");
        var created = uploads.upload(user.id(), file, metadata);
        return ResponseEntity.created(URI.create("/api/videos/" + created.id())).body(created);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidUpload(Exception exception) {
        return ResponseEntity.badRequest().body(new ApiError("Provide a video file and valid title and description.", Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidMetadata(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Check the upload metadata.", errors));
    }
}
