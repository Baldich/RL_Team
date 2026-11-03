package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.User;
import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.dto.VideoUploadRequest;
import com.tecnocampus.LS2.protube_back.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.nio.file.*;
import java.io.IOException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoUploadServiceTest {
    @TempDir Path store;
    VideoRepository videos = mock(VideoRepository.class);
    UserRepository users = mock(UserRepository.class);
    VideoProcessor processor = mock(VideoProcessor.class);
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    MockMultipartFile file = new MockMultipartFile("file", "../../evil.mp4", "video/mp4", new byte[]{1, 2, 3});
    VideoUploadRequest metadata = new VideoUploadRequest(" Title ", " Description ");

    @BeforeEach void setup() throws Exception {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(7L);
        when(users.findById(7L)).thenReturn(Optional.of(owner));
        when(videos.saveAndFlush(any())).thenAnswer(call -> {
            Video video = call.getArgument(0);
            var id = Video.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(video, 42L);
            return video;
        });
        doAnswer(call -> {
            Path input = call.getArgument(0);
            assertFalse(input.startsWith(store));
            Files.write(call.getArgument(1), new byte[]{4, 5});
            return null;
        }).when(processor).process(any(), any());
    }

    VideoUploadService service() { return new VideoUploadService(store.toString(), processor, videos, users, manager); }

    @Test void storesUniqueFilesAndOwnerOnlyAfterProcessing() throws Exception {
        var result = service().upload(7, file, metadata);
        assertEquals(42L, result.id());
        assertEquals(7L, result.ownerId());
        assertEquals("Title", result.title());
        assertEquals("Description", result.description());
        assertTrue(result.videoPath().matches("[a-f0-9-]{36}\\.mp4"));
        assertArrayEquals(file.getBytes(), Files.readAllBytes(store.resolve(result.videoPath())));
        assertTrue(Files.size(store.resolve(result.thumbnailPath())) > 0);
        verify(videos).saveAndFlush(argThat(video -> video.getSourceId() == null && video.getOwnerId() == 7));
        verify(manager).commit(any());
    }

    @Test void rejectsEmptyAndOversizedFiles() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(UploadException.class,
                () -> service().upload(7, new MockMultipartFile("file", new byte[0]), metadata)).status());
        var huge = mock(org.springframework.web.multipart.MultipartFile.class);
        when(huge.getSize()).thenReturn(VideoUploadService.MAX_BYTES + 1);
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, assertThrows(UploadException.class,
                () -> service().upload(7, huge, metadata)).status());
        verifyNoInteractions(videos);
    }

    @Test void processingFailureRemovesStagingAndPublishesNothing() throws Exception {
        Path[] input = new Path[1];
        doAnswer(call -> { input[0] = call.getArgument(0); throw new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported"); })
                .when(processor).process(any(), any());
        assertThrows(UploadException.class, () -> service().upload(7, file, metadata));
        assertFalse(Files.exists(input[0].getParent()));
        assertEmptyStore();
        verifyNoInteractions(videos);
    }

    @Test void commitFailureRemovesPublishedAssets() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(manager).commit(any());
        assertThrows(UploadException.class, () -> service().upload(7, file, metadata));
        assertEmptyStore();
    }

    @Test void missingOwnerRollsBackAndRemovesAssets() throws Exception {
        when(users.findById(7L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(UploadException.class,
                () -> service().upload(7, file, metadata)).status());
        assertEmptyStore();
        verifyNoInteractions(videos);
    }

    @Test void storageAndReadFailuresPublishNothing() throws Exception {
        Path blocked = store.resolve("blocked");
        Files.writeString(blocked, "file");
        assertThrows(UploadException.class, () -> new VideoUploadService(blocked.toString(), processor, videos, users, manager)
                .upload(7, file, metadata));
        var broken = mock(org.springframework.web.multipart.MultipartFile.class);
        when(broken.getInputStream()).thenThrow(new IOException("disconnected"));
        assertThrows(UploadException.class, () -> service().upload(7, broken, metadata));
        verifyNoInteractions(videos);
    }

    @Test void partialMoveFailureCleansFinalVideo() throws Exception {
        doAnswer(call -> null).when(processor).process(any(), any());
        assertThrows(UploadException.class, () -> service().upload(7, file, metadata));
        assertEmptyStore();
    }

    @Test void refusesTemporaryStorageInsidePublicMediaRoot() {
        String temporaryRoot = System.getProperty("java.io.tmpdir");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, assertThrows(UploadException.class,
                () -> new VideoUploadService(temporaryRoot, processor, videos, users, manager)
                        .upload(7, file, metadata)).status());
        verifyNoInteractions(processor, videos);
    }

    void assertEmptyStore() throws Exception {
        try (var files = Files.list(store)) { assertEquals(0, files.count()); }
    }
}
