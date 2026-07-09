package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.PlaylistService;
import com.music.platform.web.dto.CreatePlaylistRequest;
import com.music.platform.web.dto.MusicResponse;
import com.music.platform.web.dto.PlaylistResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {
    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping("/create")
    public ApiResponse<Long> create(@Valid @RequestBody CreatePlaylistRequest req) {
        var ju = SecurityUtils.currentJwtUser();
        long id = playlistService.create(ju.userId(), req.getName());
        return ApiResponse.ok(id);
    }

    @GetMapping("/my")
    public ApiResponse<List<PlaylistResponse>> my() {
        var ju = SecurityUtils.currentJwtUser();
        var list = playlistService.myPlaylists(ju.userId()).stream()
                .map(p -> new PlaylistResponse(
                        p.getId(),
                        p.getName(),
                        Boolean.TRUE.equals(p.getLikedDefault())
                ))
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}/songs")
    public ApiResponse<List<MusicResponse>> songs(@PathVariable("id") long playlistId) {
        var ju = SecurityUtils.currentJwtUser();
        return ApiResponse.ok(playlistService.listSongs(ju.userId(), playlistId).stream()
                .map(m -> new MusicResponse(
                        m.getId(),
                        m.getTitle(),
                        m.getArtist(),
                        m.getAlbum(),
                        m.getMoodTag(),
                        m.getDurationMs() == null ? 0 : m.getDurationMs(),
                        m.getPlayCount() == null ? 0 : m.getPlayCount(),
                        "/files/" + m.getMusicPath(),
                        m.getCoverPath() == null ? null : "/files/" + m.getCoverPath(),
                        m.getLyricPath() == null ? null : "/files/" + m.getLyricPath()
                ))
                .toList());
    }

    @PostMapping("/{id}/add")
    public ApiResponse<Void> add(@PathVariable("id") long playlistId, @RequestParam long musicId) {
        var ju = SecurityUtils.currentJwtUser();
        playlistService.addSong(ju.userId(), playlistId, musicId);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/remove")
    public ApiResponse<Void> remove(@PathVariable("id") long playlistId, @RequestParam long musicId) {
        var ju = SecurityUtils.currentJwtUser();
        playlistService.removeSong(ju.userId(), playlistId, musicId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") long playlistId) {
        var ju = SecurityUtils.currentJwtUser();
        playlistService.deletePlaylist(ju.userId(), playlistId);
        return ApiResponse.ok();
    }
}

