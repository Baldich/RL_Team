package com.tecnocampus.LS2.protube_back.controller;

import com.tecnocampus.LS2.protube_back.dto.VideoPageResponse;
import com.tecnocampus.LS2.protube_back.services.VideoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
}
