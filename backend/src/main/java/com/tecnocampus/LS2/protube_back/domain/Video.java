package com.tecnocampus.LS2.protube_back.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 120)
    private String category;
    @ElementCollection
    @CollectionTable(name = "video_tags", joinColumns = @JoinColumn(name = "video_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private List<String> tags = new ArrayList<>();

    protected Video() {}
    public Video(String title, String thumbnailPath, String videoPath, Long sourceId) {
        this(title, thumbnailPath, videoPath, sourceId, null, null, null, List.of());
    }
    public Video(String title, String thumbnailPath, String videoPath, Long sourceId, User owner,
                 String description, String category, List<String> tags) {
        this.title = title;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.sourceId = sourceId;
        this.owner = owner;
        this.description = description;
        this.category = category;
        this.tags = new ArrayList<>(tags == null ? List.of() : tags);
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getThumbnailPath() { return thumbnailPath; }
    public String getVideoPath() { return videoPath; }
    public Long getSourceId() { return sourceId; }
    public User getOwner() { return owner; }
    public Long getOwnerId() { return owner == null ? null : owner.getId(); }
    public String getDescription() { return description == null ? "" : description; }
    public boolean hasPersistedDescription() { return description != null; }
    public String getCategory() { return category == null ? "" : category; }
    public List<String> getTags() { return List.copyOf(tags); }

    public void updateMetadata(String description, String category, List<String> tags) {
        this.description = description;
        this.category = category;
        this.tags = new ArrayList<>(tags);
    }
}
