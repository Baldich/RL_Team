package com.tecnocampus.LS2.protube_back.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessor {
    private final ObjectMapper mapper;
    private final String ffmpeg;
    private final String ffprobe;
    private final long timeout;
    private final Semaphore slots = new Semaphore(2);

    public VideoProcessor(ObjectMapper mapper,
            @Value("${pro_tube.upload.ffmpeg:ffmpeg}") String ffmpeg,
            @Value("${pro_tube.upload.ffprobe:ffprobe}") String ffprobe,
            @Value("${pro_tube.upload.timeout-seconds:120}") long timeout) {
        this.mapper = mapper;
        this.ffmpeg = ffmpeg;
        this.ffprobe = ffprobe;
        this.timeout = timeout;
    }

    public void process(Path input, Path thumbnail) throws IOException {
        if (!slots.tryAcquire()) throw new UploadException(HttpStatus.SERVICE_UNAVAILABLE, "Video processing is busy. Please try again.");
        Path probe = input.resolveSibling("probe.json");
        try {
            run(List.of(ffprobe, "-v", "error", "-protocol_whitelist", "file", "-show_entries",
                    "format=format_name:format_tags=major_brand:stream=codec_type,codec_name,pix_fmt", "-of", "json", input.toString()), probe);
            if (Files.size(probe) > 65536) throw invalid();
            var info = mapper.readTree(probe.toFile());
            String brand = info.path("format").path("tags").path("major_brand").asText("").trim();
            if (!List.of("isom", "iso2", "mp41", "mp42", "avc1", "M4V").contains(brand)) throw unsupported();
            boolean video = false;
            for (var stream : info.path("streams")) {
                String type = stream.path("codec_type").asText();
                String codec = stream.path("codec_name").asText();
                if ("video".equals(type)) {
                    if (video || !"h264".equals(codec) || !"yuv420p".equals(stream.path("pix_fmt").asText())) throw unsupported();
                    video = true;
                } else if (!"audio".equals(type) || !"aac".equals(codec)) throw unsupported();
            }
            if (!video) throw invalid();
            // Decode the whole input so damaged packets fail before publication.
            run(List.of(ffmpeg, "-nostdin", "-v", "error", "-xerror", "-threads", "2", "-protocol_whitelist", "file",
                    "-i", input.toString(), "-f", "null", "-"), null);
            run(List.of(ffmpeg, "-nostdin", "-v", "error", "-xerror", "-threads", "2", "-protocol_whitelist", "file",
                    "-i", input.toString(), "-frames:v", "1", "-vf", "scale=320:-2", "-c:v", "libwebp", thumbnail.toString()), null);
            if (!Files.isRegularFile(thumbnail) || Files.size(thumbnail) == 0) throw invalid();
        } finally {
            slots.release();
            Files.deleteIfExists(probe);
        }
    }

    void run(List<String> command, Path output) throws IOException {
        Process process = new ProcessBuilder(command)
                .redirectOutput(output == null ? ProcessBuilder.Redirect.DISCARD : ProcessBuilder.Redirect.to(output.toFile()))
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                throw new UploadException(HttpStatus.UNPROCESSABLE_ENTITY, "Video processing timed out.");
            }
            if (process.exitValue() != 0) throw invalid();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Processing interrupted", exception);
        } finally {
            process.destroyForcibly();
        }
    }

    private static UploadException invalid() {
        return new UploadException(HttpStatus.UNPROCESSABLE_ENTITY, "The video is corrupt or cannot be processed.");
    }
    private static UploadException unsupported() {
        return new UploadException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use an MP4 video with H.264 (8-bit 4:2:0) and AAC audio, or no audio.");
    }
}
