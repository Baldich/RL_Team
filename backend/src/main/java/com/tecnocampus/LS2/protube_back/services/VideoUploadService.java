package com.tecnocampus.LS2.protube_back.services;

import com.tecnocampus.LS2.protube_back.domain.Video;
import com.tecnocampus.LS2.protube_back.dto.VideoUploadRequest;
import com.tecnocampus.LS2.protube_back.dto.VideoDetailResponse;
import com.tecnocampus.LS2.protube_back.repository.UserRepository;
import com.tecnocampus.LS2.protube_back.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class VideoUploadService {
    public static final long MAX_BYTES = 100L * 1024 * 1024;
    private final Path store;
    private final VideoProcessor processor;
    private final VideoRepository videos;
    private final UserRepository users;
    private final TransactionTemplate transactions;

    public VideoUploadService(@Value("${pro_tube.store.dir}") String store, VideoProcessor processor,
            VideoRepository videos, UserRepository users, org.springframework.transaction.PlatformTransactionManager manager) {
        this.store = Path.of(store).toAbsolutePath().normalize();
        this.processor = processor;
        this.videos = videos;
        this.users = users;
        this.transactions = new TransactionTemplate(manager);
    }

    public VideoDetailResponse upload(long ownerId, MultipartFile file, VideoUploadRequest metadata) {
        if (file.isEmpty()) throw new UploadException(HttpStatus.BAD_REQUEST, "Select a non-empty video file.");
        if (file.getSize() > MAX_BYTES) throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE, "Video must be at most 100 MB.");
        String stem = UUID.randomUUID().toString();
        Path video = store.resolve(stem + ".mp4");
        Path thumbnail = store.resolve(stem + ".webp");
        Path staging = null;
        boolean movedVideo = false;
        boolean movedThumbnail = false;
        boolean published = false;
        try {
            Files.createDirectories(store);
            // OS-private temporary directory; never located under the public media root.
            staging = Files.createTempDirectory("protube-upload-");
            if (staging.toRealPath().startsWith(store.toRealPath())) {
                throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Private temporary storage must be outside the media store.");
            }
            Path input = staging.resolve("video.mp4");
            Path image = staging.resolve("thumbnail.webp");
            try (var source = file.getInputStream(); var target = Files.newOutputStream(input)) {
                byte[] buffer = new byte[65536];
                long total = 0;
                int count;
                while ((count = source.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_BYTES) throw new UploadException(HttpStatus.PAYLOAD_TOO_LARGE, "Video must be at most 100 MB.");
                    target.write(buffer, 0, count);
                }
                if (total == 0) throw new UploadException(HttpStatus.BAD_REQUEST, "Select a non-empty video file.");
            }
            processor.process(input, image);
            // Reserve without replacement so collisions cannot overwrite existing assets.
            Files.createFile(video);
            movedVideo = true;
            Files.copy(input, video, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Files.createFile(thumbnail);
            movedThumbnail = true;
            Files.copy(image, thumbnail, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            var result = transactions.execute(status -> {
                var owner = users.findById(ownerId).orElseThrow(() -> new UploadException(HttpStatus.UNAUTHORIZED, "Please log in again."));
                Video saved = videos.saveAndFlush(new Video(metadata.title(), thumbnail.getFileName().toString(),
                        video.getFileName().toString(), null, owner, metadata.description(), "", List.of()));
                return new VideoDetailResponse(saved.getId(), saved.getTitle(), saved.getDescription(), saved.getVideoPath(),
                        saved.getThumbnailPath(), List.of(), saved.getTags(), saved.getCategory(), saved.getOwnerId());
            });
            published = true;
            return result;
        } catch (UploadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new UploadException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store or process the video. Please try again.");
        } finally {
            if (!published) {
                if (movedVideo) delete(video);
                if (movedThumbnail) delete(thumbnail);
            }
            if (staging != null) {
                delete(staging.resolve("video.mp4"));
                delete(staging.resolve("thumbnail.webp"));
                delete(staging.resolve("probe.json"));
                delete(staging);
            }
        }
    }

    private static void delete(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException exception) {
            org.slf4j.LoggerFactory.getLogger(VideoUploadService.class).warn("Upload cleanup failed; storage maintenance required.");
        }
    }
}
