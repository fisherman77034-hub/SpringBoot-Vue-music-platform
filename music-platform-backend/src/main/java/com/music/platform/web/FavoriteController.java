package com.music.platform.web;

import com.music.platform.common.ApiResponse;
import com.music.platform.security.SecurityUtils;
import com.music.platform.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/status")
    public ApiResponse<Boolean> status(@RequestParam long musicId) {
        var ju = SecurityUtils.currentJwtUser();
        return ApiResponse.ok(favoriteService.isFavorited(ju.userId(), musicId));
    }

    @PostMapping("/toggle")
    public ApiResponse<Boolean> toggle(@RequestParam long musicId) {
        var ju = SecurityUtils.currentJwtUser();
        return ApiResponse.ok(favoriteService.toggle(ju.userId(), musicId));
    }

    @PostMapping("/add-to-playlist")
    public ApiResponse<Void> addToPlaylist(@RequestParam long musicId, @RequestParam long playlistId) {
        var ju = SecurityUtils.currentJwtUser();
        favoriteService.addToPlaylist(ju.userId(), playlistId, musicId);
        return ApiResponse.ok();
    }

    @PostMapping("/remove-from-playlist")
    public ApiResponse<Void> removeFromPlaylist(@RequestParam long musicId, @RequestParam long playlistId) {
        var ju = SecurityUtils.currentJwtUser();
        favoriteService.removeFromPlaylist(ju.userId(), playlistId, musicId);
        return ApiResponse.ok();
    }

    @PostMapping("/move-playlist")
    public ApiResponse<Void> movePlaylist(
            @RequestParam long musicId,
            @RequestParam long fromPlaylistId,
            @RequestParam long toPlaylistId
    ) {
        var ju = SecurityUtils.currentJwtUser();
        favoriteService.moveBetweenPlaylists(ju.userId(), musicId, fromPlaylistId, toPlaylistId);
        return ApiResponse.ok();
    }
}

