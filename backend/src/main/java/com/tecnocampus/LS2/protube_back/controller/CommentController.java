package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.*;
import com.tecnocampus.LS2.protube_back.services.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/videos/{videoId}/comments/{commentId}")
public class CommentController {
    private final CommentService comments;

    public CommentController(CommentService comments) { this.comments = comments; }

    @PostMapping("/replies")
    public ResponseEntity<VideoCommentResponse> reply(@PathVariable long videoId, @PathVariable String commentId,
            @AuthenticationPrincipal RegistrationResponse user, @Valid @RequestBody VideoCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comments.reply(videoId, commentId, userId(user), request.text()));
    }

    @PutMapping("/like")
    public CommentLikeResponse like(@PathVariable long videoId, @PathVariable String commentId,
                                     @AuthenticationPrincipal RegistrationResponse user) {
        return comments.setLiked(videoId, commentId, userId(user), true);
    }

    @DeleteMapping("/like")
    public CommentLikeResponse unlike(@PathVariable long videoId, @PathVariable String commentId,
                                       @AuthenticationPrincipal RegistrationResponse user) {
        return comments.setLiked(videoId, commentId, userId(user), false);
    }

    private static long userId(RegistrationResponse user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return user.id();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalidComment(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("Check the comment text.", errors));
    }
}
