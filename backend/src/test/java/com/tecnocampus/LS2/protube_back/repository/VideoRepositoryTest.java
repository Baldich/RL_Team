package com.tecnocampus.LS2.protube_back.repository;

import com.tecnocampus.LS2.protube_back.PostgresTestSupport;
import com.tecnocampus.LS2.protube_back.domain.Video;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VideoRepositoryTest extends PostgresTestSupport {
    @Autowired VideoRepository videos;
    @Autowired TestEntityManager entityManager;

    @Test
    void persistsMetadataAndPagesWithoutRepeatingEntries() {
        for (long i = 0; i < 13; i++) videos.save(new Video("Video " + i, i + ".webp", i + ".mp4", i));
        videos.flush();
        entityManager.clear();
        var first = videos.findAll(PageRequest.of(0, 12, Sort.by("id")));
        var second = videos.findAll(PageRequest.of(1, 12, Sort.by("id")));
        assertEquals(13, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(12, first.getNumberOfElements());
        assertEquals(1, second.getNumberOfElements());
        assertTrue(first.stream().noneMatch(video -> video.getId().equals(second.getContent().get(0).getId())));
        assertEquals("Video 0", first.getContent().get(0).getTitle());
        assertEquals("0.webp", first.getContent().get(0).getThumbnailPath());
        assertEquals("0.mp4", first.getContent().get(0).getVideoPath());
        assertEquals(0L, first.getContent().get(0).getSourceId());
        assertTrue(videos.existsBySourceId(0L));
        assertTrue(videos.findAll(PageRequest.of(2, 12)).isEmpty());
    }
    @Test
    void databaseRejectsDuplicateGrabberSourceIds() {
        videos.saveAndFlush(new Video("First", "0.webp", "0.mp4", 0L));
        assertThrows(DataIntegrityViolationException.class,
                () -> videos.saveAndFlush(new Video("Duplicate", "0.webp", "0.mp4", 0L)));
    }
}
