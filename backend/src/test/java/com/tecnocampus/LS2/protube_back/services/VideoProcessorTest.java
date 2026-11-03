package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import java.nio.file.*;
import java.util.List;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoProcessorTest {
    @TempDir Path temp;
    VideoProcessor processor(String streams, String brand, boolean thumbnail) throws Exception {
        var processor = spy(new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 5));
        doAnswer(call -> {
            Path output = call.getArgument(1);
            if (output != null) Files.writeString(output, "{\"format\":{\"tags\":{\"major_brand\":\"" + brand + "\"}},\"streams\":" + streams + "}");
            else if (thumbnail) Files.writeString(temp.resolve("thumbnail.webp"), "image");
            return null;
        }).when(processor).run(anyList(), any());
        return processor;
    }
    static final String H264 = "[{\"codec_type\":\"video\",\"codec_name\":\"h264\",\"pix_fmt\":\"yuv420p\"}]";

    @Test void acceptsH264AndGeneratesThumbnail() throws Exception {
        processor(H264, "isom", true).process(temp.resolve("video.mp4"), temp.resolve("thumbnail.webp"));
        assertTrue(Files.exists(temp.resolve("thumbnail.webp")));
        assertFalse(Files.exists(temp.resolve("probe.json")));
    }
    @Test void rejectsOtherContainerAndCodecAndMissingVideo() throws Exception {
        for (String streams : List.of(H264.replace("h264", "vp9"), H264.replace("yuv420p", "yuv444p"),
                "[{\"codec_type\":\"audio\",\"codec_name\":\"opus\"}]", "[]")) {
            assertThrows(UploadException.class, () -> processor(streams, "isom", true)
                    .process(temp.resolve("video.mp4"), temp.resolve("thumbnail.webp")));
        }
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, assertThrows(UploadException.class,
                () -> processor(H264, "qt", true).process(temp.resolve("video.mp4"), temp.resolve("thumbnail.webp"))).status());
    }
    @Test void rejectsMissingThumbnailAndMalformedProbe() throws Exception {
        assertThrows(UploadException.class, () -> processor(H264, "isom", false).process(temp.resolve("video.mp4"), temp.resolve("thumbnail.webp")));
        var processor = processor(H264, "isom", false);
        doAnswer(call -> { Files.writeString(call.getArgument(1), "broken"); return null; }).when(processor).run(anyList(), any());
        assertThrows(IOException.class, () -> processor.process(temp.resolve("video.mp4"), temp.resolve("thumbnail.webp")));
    }
    @Test void unavailableExecutableFailsSafely() {
        assertThrows(IOException.class, () -> new VideoProcessor(new ObjectMapper(), "missing-protube-ffmpeg", "missing-protube-ffprobe", 1)
                .run(List.of("missing-protube-ffprobe"), null));
    }
}
