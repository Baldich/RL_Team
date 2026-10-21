package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataIntegrityViolationException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoImportServiceTest {
    @TempDir Path store;
    VideoRepository repository = mock(VideoRepository.class);
    VideoImportService importer = new VideoImportService(repository, new ObjectMapper());

    void complete(long id, String metadata) throws Exception {
        Files.writeString(store.resolve(id + ".json"), metadata);
        Files.write(store.resolve(id + ".webp"), new byte[]{1});
        Files.write(store.resolve(id + ".mp4"), new byte[]{1});
    }
    @Test
    void importsSourceZeroAndSkipsAlreadyImportedRecords() throws Exception {
        complete(0, "{\"id\":0,\"title\":\"  Real title  \",\"meta\":{\"description\":\"ignored\"}}");
        when(repository.existsBySourceId(0L)).thenReturn(false, true);
        importer.importVideos(store);
        importer.importVideos(store);
        verify(repository, times(1)).saveAndFlush(argThat(video ->
                video.getTitle().equals("Real title") && video.getThumbnailPath().equals("0.webp")
                && video.getVideoPath().equals("0.mp4") && video.getSourceId() == 0));
    }
    @Test
    void invalidAndIncompleteFilesDoNotPreventValidImport() throws Exception {
        complete(0, "{\"id\":0,\"title\":\"Valid\"}");
        complete(1, "not json");
        complete(2, "{\"id\":2,\"title\":\"   \"}");
        complete(3, "{\"id\":-1,\"title\":\"Invalid\"}");
        complete(4, "{\"id\":4,\"title\":\"Missing image\"}");
        Files.delete(store.resolve("4.webp"));
        complete(5, "{\"id\":5,\"title\":\"Missing video\"}");
        Files.delete(store.resolve("5.mp4"));
        complete(6, "{\"id\":7,\"title\":\"Mismatch\"}");
        complete(8, "{\"title\":\"No ID\"}");
        complete(9, "{\"id\":9,\"title\":42}");
        complete(10, "{\"id\":10.5,\"title\":\"Fractional\"}");
        complete(11, "{\"id\":99999999999999999999999,\"title\":\"Overflow\"}");
        complete(13, "");
        complete(14, "null");
        complete(15, "{\"id\":15,\"title\":\"bad\\u0000title\"}");
        complete(16, "{\"id\":16,\"title\":\"Empty thumbnail\"}");
        Files.write(store.resolve("16.webp"), new byte[0]);
        complete(17, "{\"id\":17,\"title\":\"Empty video\"}");
        Files.write(store.resolve("17.mp4"), new byte[0]);
        Files.writeString(store.resolve("notes.json"), "{}");
        Files.createDirectory(store.resolve("12.json"));
        importer.importVideos(store);
        verify(repository, times(1)).saveAndFlush(any(Video.class));
    }
    @Test
    void missingStoreIsSafe() {
        assertDoesNotThrow(() -> importer.importVideos(store.resolve("missing")));
        verifyNoInteractions(repository);
    }
    @Test
    void concurrentDuplicateIsSafeButOtherDatabaseErrorsAreNotHidden() throws Exception {
        complete(0, "{\"id\":0,\"title\":\"Title\"}");
        when(repository.existsBySourceId(0L)).thenReturn(false, true);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertDoesNotThrow(() -> importer.importVideos(store));
        when(repository.existsBySourceId(0L)).thenReturn(false);
        assertThrows(DataIntegrityViolationException.class, () -> importer.importVideos(store));
    }
}
