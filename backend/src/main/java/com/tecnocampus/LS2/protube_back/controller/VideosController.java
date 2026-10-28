package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.ApiError;
import com.tecnocampus.LS2.protube_back.dto.RegistrationResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoCommentRequest;
import com.tecnocampus.LS2.protube_back.dto.VideoCommentResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoDetailResponse;
import com.tecnocampus.LS2.protube_back.dto.VideoPageResponse;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideosController {
    private final VideoService videos;
    public VideosController(VideoService videos) { this.videos = videos; }

    @GetMapping
    public VideoPageResponse getVideos(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "12") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be nonnegative and size between 1 and 100.");
        }
        return videos.getVideos(page, size);
    }

    @GetMapping("/{id}")
    public VideoDetailResponse getVideo(@PathVariable long id) {
        return videos.getVideoDetails(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<VideoCommentResponse> addComment(@PathVariable long id,
                                                         Authentication authentication,
                                                         @Valid @RequestBody VideoCommentRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RegistrationResponse user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to comment.");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(videos.addComment(id, user.id(), request.text()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidComment(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Check the comment text.", errors));
    }
}
