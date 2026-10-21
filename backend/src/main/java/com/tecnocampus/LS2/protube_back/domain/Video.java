package com.tecnocampus.LS2.protube_back.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "videos", uniqueConstraints = @UniqueConstraint(name = "uk_videos_source_id", columnNames = "source_id"))
public class Video {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String title;
    @Column(nullable = false)
    private String thumbnailPath;
    @Column(nullable = false)
    private String videoPath;
    @Column(name = "source_id")
    private Long sourceId;

    protected Video() {}
    public Video(String title, String thumbnailPath, String videoPath, Long sourceId) {
        this.title = title;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.sourceId = sourceId;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getVideoPath() { return videoPath; }
    public Long getSourceId() { return sourceId; }
}
