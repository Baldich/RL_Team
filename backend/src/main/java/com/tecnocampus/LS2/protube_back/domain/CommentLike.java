package com.tecnocampus.LS2.protube_back.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "comment_likes", uniqueConstraints = @UniqueConstraint(
        name = "uk_comment_likes_video_comment_user", columnNames = {"video_id", "comment_key", "user_id"}))
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "comment_key", nullable = false, length = 80)
    private String commentKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected CommentLike() {}

    public String getCommentKey() { return commentKey; }
    public Long getUserId() { return user.getId(); }
}
