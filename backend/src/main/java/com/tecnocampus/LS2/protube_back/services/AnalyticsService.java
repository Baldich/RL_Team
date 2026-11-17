package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.dto.AnalyticsResponse;
import com.tecnocampus.LS2.protube_back.dto.AnalyticsVideoResponse;
import com.tecnocampus.LS2.protube_back.repository.CommentLikeRepository;
import com.tecnocampus.LS2.protube_back.repository.CommentRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final VideoRepository videos;
    private final CommentRepository comments;
    private final CommentLikeRepository likes;

    public AnalyticsService(VideoRepository videos, CommentRepository comments, CommentLikeRepository likes) {
        this.videos = videos;
        this.comments = comments;
        this.likes = likes;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getForUser(long userId) {
        long totalViews = 0;
        long totalComments = 0;
        long totalLikes = 0;
        var metrics = new java.util.ArrayList<AnalyticsVideoResponse>();
        for (var video : videos.findByOwner_IdOrderByIdDesc(userId)) {
            long videoComments = comments.countByVideoId(video.getId());
            long videoLikes = likes.countByVideoId(video.getId());
            metrics.add(new AnalyticsVideoResponse(video.getId(), video.getTitle(), video.getViewCount(),
                    videoComments, videoLikes));
            totalViews += video.getViewCount();
            totalComments += videoComments;
            totalLikes += videoLikes;
        }
        return new AnalyticsResponse(metrics, totalViews, totalComments, totalLikes);
    }
}
