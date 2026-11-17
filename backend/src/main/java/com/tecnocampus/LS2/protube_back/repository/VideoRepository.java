package com.tecnocampus.LS2.protube_back.repository;

import com.tecnocampus.LS2.protube_back.domain.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    boolean existsBySourceId(Long sourceId);
    List<Video> findByOwner_IdOrderByIdDesc(Long ownerId);

    @Modifying
    @Transactional
    @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :videoId")
    int incrementViewCount(@Param("videoId") long videoId);

    @Query(value = """
            select v.*
            from videos v
            where to_tsvector('simple', coalesce(v.title, '') || ' ' || coalesce(v.description, ''))
                @@ websearch_to_tsquery('simple', :query)
            order by ts_rank(
                to_tsvector('simple', coalesce(v.title, '') || ' ' || coalesce(v.description, '')),
                websearch_to_tsquery('simple', :query)
            ) desc, v.id desc
            """,
            countQuery = """
            select count(*)
            from videos v
            where to_tsvector('simple', coalesce(v.title, '') || ' ' || coalesce(v.description, ''))
                @@ websearch_to_tsquery('simple', :query)
            """,
            nativeQuery = true)
    Page<Video> search(@Param("query") String query, Pageable pageable);
}
