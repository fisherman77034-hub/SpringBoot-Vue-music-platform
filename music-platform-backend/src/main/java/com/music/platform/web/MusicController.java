package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.common.StoredRelativePath;
import com.music.platform.common.UploadFileFormatValidator;
import com.music.platform.domain.Mood;
import com.music.platform.domain.Music;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.MusicService;
import com.music.platform.service.StorageService;
import com.music.platform.util.AudioDurationReader;
import com.music.platform.web.dto.MusicAdminPageResponse;
import com.music.platform.web.dto.MusicResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/music")
public class MusicController {
    private final MusicService musicService;
    private final StorageService storageService;

    public MusicController(MusicService musicService, StorageService storageService) {
        this.musicService = musicService;
        this.storageService = storageService;
    }

    @GetMapping("/list")
    public ApiResponse<List<MusicResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ApiResponse.ok(musicService.list(page, size).stream().map(this::toResp).toList());
    }

    @GetMapping("/search")
    public ApiResponse<List<MusicResponse>> search(
            @RequestParam String kw,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ApiResponse.ok(musicService.search(kw, page, size).stream().map(this::toResp).toList());
    }

    @GetMapping("/recommend")
    public ApiResponse<List<MusicResponse>> recommend(
            @RequestParam Mood mood,
            @RequestParam(defaultValue = "6") @Min(1) @Max(20) int limit
    ) {
        return ApiResponse.ok(musicService.recommend(mood, limit).stream().map(this::toResp).toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<MusicResponse> detail(@PathVariable long id) {
        return ApiResponse.ok(toResp(musicService.getById(id)));
    }

    @PostMapping("/{id}/play")
    public ApiResponse<Void> played(@PathVariable long id) {
        musicService.incPlayCount(id);
        return ApiResponse.ok();
    }

    /**
     * 后台上传音乐（管理员）
     * form-data: title, artist, album?, moodTag, durationMs?（可选；服务端从音频解析时长，失败时用此值）, musicFile, coverFile?, lyricFile?
     */
    @PostMapping(value = "/admin/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Long> upload(
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam(required = false) String album,
            @RequestParam Mood moodTag,
            @RequestParam(required = false, defaultValue = "0") int durationMs,
            @RequestParam MultipartFile musicFile,
            @RequestParam(required = false) MultipartFile coverFile,
            @RequestParam(required = false) MultipartFile lyricFile
    ) throws IOException {
        requireAdmin();

        UploadFileFormatValidator.requireAudio(musicFile);
        UploadFileFormatValidator.optionalImage(coverFile);
        UploadFileFormatValidator.optionalLyric(lyricFile);

        String musicPath = storageService.saveMusic(musicFile);
        int detectedMs = AudioDurationReader.durationMsFromPath(storageService.resolveStoredPath(musicPath));
        int effectiveDurationMs = detectedMs > 0 ? detectedMs : durationMs;
        String coverPath = (coverFile == null || coverFile.isEmpty()) ? null : storageService.saveCover(coverFile);
        String lyricPath = (lyricFile == null || lyricFile.isEmpty()) ? null : storageService.saveLyric(lyricFile);

        Music m = new Music();
        m.setTitle(title);
        m.setArtist(artist);
        m.setAlbum(album);
        m.setMoodTag(moodTag);
        m.setDurationMs(effectiveDurationMs);
        m.setMusicPath(musicPath);
        m.setCoverPath(coverPath);
        m.setLyricPath(lyricPath);
        m.setPlayCount(0L);

        long id = musicService.create(m);
        return ApiResponse.ok(id);
    }

    /**
     * 管理员分页检索歌曲（需登录且为 ADMIN；关键词可空表示全部）
     */
    @GetMapping("/admin/page")
    public ApiResponse<MusicAdminPageResponse> adminPage(
            @RequestParam(required = false) String kw,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        requireAdmin();
        long total = musicService.adminCount(kw);
        var records = musicService.adminPage(kw, page, size).stream().map(this::toResp).toList();
        return ApiResponse.ok(new MusicAdminPageResponse(records, total));
    }

    /**
     * 管理员更新歌曲：表单字段为基本信息；可选上传新音频/封面/歌词；可勾选移除封面或歌词
     */
    @PostMapping(value = "/admin/{id}/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> adminUpdate(
            @PathVariable long id,
            @RequestParam String title,
            @RequestParam String artist,
            @RequestParam(required = false) String album,
            @RequestParam Mood moodTag,
            @RequestParam(required = false, defaultValue = "0") int durationMs,
            @RequestParam(required = false) MultipartFile musicFile,
            @RequestParam(required = false) MultipartFile coverFile,
            @RequestParam(required = false) MultipartFile lyricFile,
            @RequestParam(required = false, defaultValue = "false") boolean removeCover,
            @RequestParam(required = false, defaultValue = "false") boolean removeLyric
    ) throws IOException {
        requireAdmin();
        musicService.adminUpdate(
                id, title, artist, album, moodTag, durationMs,
                musicFile, coverFile, lyricFile, removeCover, removeLyric
        );
        return ApiResponse.ok();
    }

    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> adminDelete(@PathVariable long id) {
        requireAdmin();
        musicService.adminDelete(id);
        return ApiResponse.ok();
    }

    private void requireAdmin() {
        var ju = SecurityUtils.currentJwtUser();
        if (!"ADMIN".equalsIgnoreCase(ju.role())) {
            throw new AccessDeniedException("需要管理员权限");
        }
    }

    private MusicResponse toResp(Music m) {
        String musicKey = StoredRelativePath.normalizeOrNull(m.getMusicPath(), "music");
        String coverKey = StoredRelativePath.normalizeOrNull(m.getCoverPath(), "covers");
        String lyricKey = StoredRelativePath.normalizeOrNull(m.getLyricPath(), "lyrics");
        String musicUrl = musicKey == null ? null : "/files/" + musicKey;
        String coverUrl = coverKey == null ? null : "/files/" + coverKey;
        String lyricUrl = lyricKey == null ? null : "/files/" + lyricKey;
        return new MusicResponse(
                m.getId(),
                m.getTitle(),
                m.getArtist(),
                m.getAlbum(),
                m.getMoodTag(),
                m.getDurationMs() == null ? 0 : m.getDurationMs(),
                m.getPlayCount() == null ? 0 : m.getPlayCount(),
                musicUrl,
                coverUrl,
                lyricUrl
        );
    }
}

