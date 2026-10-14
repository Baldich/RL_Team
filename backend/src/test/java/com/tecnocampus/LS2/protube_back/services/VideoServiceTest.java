package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoServiceTest {
    @Test
    void returnsSummariesAndStablePageMetadata() {
        var repository = mock(VideoRepository.class);
        var request = PageRequest.of(1, 12, Sort.by("id").ascending());
        when(repository.findAll(request)).thenReturn(new PageImpl<>(List.of(new Video("Title", "0.webp", "0.mp4", 0L)), request, 13));
        var result = new VideoService(repository).getVideos(1, 12);
        assertEquals("Title", result.content().get(0).title());
        assertEquals("0.webp", result.content().get(0).thumbnailPath());
        assertEquals(1, result.page());
        assertEquals(12, result.size());
        assertEquals(13, result.totalElements());
        assertEquals(2, result.totalPages());
        verify(repository).findAll(request);
    }
    @Test
    void emptyCatalogIsSuccessful() {
        var repository = mock(VideoRepository.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 12)));
        var result = new VideoService(repository).getVideos(0, 12);
        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalPages());
    }
}
