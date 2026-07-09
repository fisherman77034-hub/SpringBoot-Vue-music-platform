package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.common.StoredRelativePath;
import com.music.platform.domain.Music;
import com.music.platform.domain.PlayQueueItem;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.PlayQueueService;
import com.music.platform.web.dto.MusicResponse;
import com.music.platform.web.dto.PlayQueueEntryResponse;
import com.music.platform.web.dto.ReplacePlayQueueRequest;
import com.music.platform.web.dto.ReorderPlayQueueRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/play-queue")
public class PlayQueueController {
    private final PlayQueueService playQueueService;

    public PlayQueueController(PlayQueueService playQueueService) {
        this.playQueueService = playQueueService;
    }

    @GetMapping
    public ApiResponse<List<PlayQueueEntryResponse>> list() {
        var ju = SecurityUtils.currentJwtUser();
        List<PlayQueueEntryResponse> list = playQueueService.list(ju.userId()).stream()
                .map(this::toEntry)
                .toList();
        return ApiResponse.ok(list);
    }

    @PostMapping("/add")
    public ApiResponse<Void> add(@RequestParam long musicId) {
        var ju = SecurityUtils.currentJwtUser();
        playQueueService.add(ju.userId(), musicId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/item/{itemId}")
    public ApiResponse<Void> remove(@PathVariable long itemId) {
        var ju = SecurityUtils.currentJwtUser();
        playQueueService.remove(ju.userId(), itemId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/clear")
    public ApiResponse<Void> clear() {
        var ju = SecurityUtils.currentJwtUser();
        playQueueService.clear(ju.userId());
        return ApiResponse.ok();
    }

    @PutMapping("/replace")
    public ApiResponse<Void> replace(@Valid @RequestBody ReplacePlayQueueRequest body) {
        var ju = SecurityUtils.currentJwtUser();
        playQueueService.replace(ju.userId(), body.getMusicIds());
        return ApiResponse.ok();
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@Valid @RequestBody ReorderPlayQueueRequest body) {
        var ju = SecurityUtils.currentJwtUser();
        playQueueService.reorder(ju.userId(), body.getOrderedIds());
        return ApiResponse.ok();
    }

    private PlayQueueEntryResponse toEntry(PlayQueueItem row) {
        return new PlayQueueEntryResponse(row.getId(), toResp(row.getMusic()));
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
