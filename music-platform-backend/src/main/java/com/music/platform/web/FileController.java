package com.music.platform.web;

import com.music.platform.service.StorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
public class FileController {
    // 超过此大小的非音频 Range 响应才分块，避免单次响应过大
    private static final long LARGE_FILE_CHUNK = 8L * 1024 * 1024;

    private static final long AUDIO_WHOLE_FILE_MAX = 200L * 1024 * 1024;


    private static final Map<String, MediaType> EXTENSION_MEDIA_TYPES = Map.ofEntries(
            Map.entry(".mp3", MediaType.parseMediaType("audio/mpeg")),
            Map.entry(".flac", MediaType.parseMediaType("audio/flac")),
            Map.entry(".wav", MediaType.parseMediaType("audio/wav")),
            Map.entry(".m4a", MediaType.parseMediaType("audio/mp4")),
            Map.entry(".mp4", MediaType.parseMediaType("audio/mp4")),
            Map.entry(".aac", MediaType.parseMediaType("audio/aac")),
            Map.entry(".ogg", MediaType.parseMediaType("audio/ogg")),
            Map.entry(".opus", MediaType.parseMediaType("audio/opus")),
            Map.entry(".webm", MediaType.parseMediaType("audio/webm")),
            Map.entry(".jpg", MediaType.IMAGE_JPEG),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG),
            Map.entry(".png", MediaType.IMAGE_PNG),
            Map.entry(".gif", MediaType.IMAGE_GIF),
            Map.entry(".webp", MediaType.parseMediaType("image/webp")),
            Map.entry(".lrc", MediaType.parseMediaType("text/plain;charset=UTF-8")),
            Map.entry(".txt", MediaType.parseMediaType("text/plain;charset=UTF-8"))
    );
    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/files/{*path}")
    public ResponseEntity<?> file(
            @PathVariable("path") String path,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range
    ) throws IOException {
        Resource resource = storageService.asResource(path);
        if (!resource.exists()) {
            log.warn("文件不存在: path={} -> {}", path, resource);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        MediaType mediaType = resolveMediaType(path, resource);

        long contentLength = resource.contentLength();
        if (contentLength == 0) {
            log.warn("文件大小为 0，无法播放: path={}", path);
        }

        // <audio> 会带 Range；原先把每段截成 1MB 且 206 处理不完整时，Chrome 常报 MEDIA_ELEMENT_ERROR / 无法解码
        if (range == null || shouldReturnWholeAudioFile(path, contentLength)) {
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        }

        List<HttpRange> ranges = HttpRange.parseRanges(range);
        if (ranges.isEmpty()) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        HttpRange r = ranges.get(0);
        long start = r.getRangeStart(contentLength);
        long end = r.getRangeEnd(contentLength);
        long requestedLen = end - start + 1;

        long rangeLength = isAudioPath(path)
                ? requestedLen
                : Math.min(LARGE_FILE_CHUNK, requestedLen);
        ResourceRegion region = new ResourceRegion(resource, start, rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(region);
    }

    private static boolean isAudioPath(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3")
                || lower.endsWith(".flac")
                || lower.endsWith(".wav")
                || lower.endsWith(".m4a")
                || lower.endsWith(".mp4")
                || lower.endsWith(".aac")
                || lower.endsWith(".ogg")
                || lower.endsWith(".opus")
                || lower.endsWith(".webm");
    }

    //中小体积音频这样对解码器最稳
    private static boolean shouldReturnWholeAudioFile(String relativePath, long contentLength) {
        if (contentLength <= 0 || contentLength > AUDIO_WHOLE_FILE_MAX) {
            return false;
        }
        return isAudioPath(relativePath);
    }

    private MediaType resolveMediaType(String relativePath, Resource resource) throws IOException {
        MediaType byExt = mediaTypeByFileName(relativePath);
        if (byExt != null) {
            return byExt;
        }
        if (resource instanceof FileSystemResource fsr) {
            Path filePath = fsr.getFile().toPath();
            String probed = Files.probeContentType(filePath);
            if (probed != null && !MediaType.APPLICATION_OCTET_STREAM_VALUE.equalsIgnoreCase(probed)) {
                return MediaType.parseMediaType(probed);
            }
        }
        String name = relativePath;
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        String guessed = URLConnection.guessContentTypeFromName(name);
        if (guessed != null) {
            return MediaType.parseMediaType(guessed);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static MediaType mediaTypeByFileName(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        return EXTENSION_MEDIA_TYPES.get(lower.substring(dot));
    }
}

