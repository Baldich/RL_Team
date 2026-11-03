package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Uses installed FFmpeg/FFprobe, as the upload backend does in production. */
class VideoProcessorRuntimeTest {
    @TempDir Path temp;
    @Test void processesRealH264AndRejectsCorruptInput() throws Exception {
        var processor = new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 30);
        Path video = temp.resolve("video.mp4");
        processor.run(List.of("ffmpeg", "-nostdin", "-v", "error", "-f", "lavfi", "-i", "color=c=blue:s=64x64:d=1",
                "-c:v", "libx264", "-pix_fmt", "yuv420p", video.toString()), null);
        processor.process(video, temp.resolve("thumbnail.webp"));
        assertTrue(Files.size(temp.resolve("thumbnail.webp")) > 0);
        Files.writeString(video, "not a video");
        assertThrows(UploadException.class, () -> processor.process(video, temp.resolve("bad.webp")));
    }
    @Test void terminatesTimedOutProcess() {
        var processor = new VideoProcessor(new ObjectMapper(), "ffmpeg", "ffprobe", 1);
        assertThrows(UploadException.class, () -> processor.run(List.of("ffmpeg", "-nostdin", "-v", "error", "-re",
                "-f", "lavfi", "-i", "color=s=64x64", "-f", "null", "-"), null));
    }
}
