package com.tecnocampus.LS2.protube_back.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "video_comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private int likeCount;

    // References can point to either a local row or a read-only videoGrabber comment.
    @Column(name = "parent_comment_key", length = 80)
    private String parentCommentKey;

    protected Comment() {}

    public Comment(Video video, User author, String text, long createdAt, int likeCount) {
        this(video, author, text, createdAt, likeCount, null);
    }

    public Comment(Video video, User author, String text, long createdAt, int likeCount, String parentCommentKey) {
        this.video = video;
        this.author = author;
        this.text = text;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.parentCommentKey = parentCommentKey;
    }

    public Long getId() { return id; }
    public Video getVideo() { return video; }
    public User getAuthor() { return author; }
    public String getText() { return text; }
    public long getCreatedAt() { return createdAt; }
    public int getLikeCount() { return likeCount; }
    public String getParentCommentKey() { return parentCommentKey; }
}
