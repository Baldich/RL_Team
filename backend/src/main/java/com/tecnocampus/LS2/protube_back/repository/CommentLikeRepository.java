package com.tecnocampus.LS2.protube_back.repository;

import com.tecnocampus.LS2.protube_back.domain.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    List<CommentLike> findByVideoId(Long videoId);
    long countByVideoIdAndCommentKey(long videoId, String commentKey);
    boolean existsByVideoIdAndCommentKeyAndUser_Id(long videoId, String commentKey, long userId);

    // PostgreSQL arbitrates duplicate concurrent requests without aborting the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO comment_likes (video_id, comment_key, user_id)
            VALUES (:videoId, :commentKey, :userId)
            ON CONFLICT (video_id, comment_key, user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("videoId") long videoId, @Param("commentKey") String commentKey,
                       @Param("userId") long userId);

    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.video.id = :videoId AND l.commentKey = :commentKey AND l.user.id = :userId")
    int removeLike(@Param("videoId") long videoId, @Param("commentKey") String commentKey,
                   @Param("userId") long userId);
}
