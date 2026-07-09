package com.music.platform.service;

import com.music.platform.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final PlaylistService playlistService;

    public FavoriteService(FavoriteMapper favoriteMapper, PlaylistService playlistService) {
        this.favoriteMapper = favoriteMapper;
        this.playlistService = playlistService;
    }

    public boolean isFavorited(long userId, long musicId) {
        return favoriteMapper.exists(userId, musicId) > 0;
    }

    /**
     * 喜欢/取消喜欢：与默认「我喜欢的音乐」歌单及收藏表同步。
     */
    @Transactional
    public boolean toggle(long userId, long musicId) {
        long likedPlaylistId = playlistService.ensureDefaultLikedPlaylistId(userId);
        boolean exists = favoriteMapper.exists(userId, musicId) > 0;
        if (exists) {
            favoriteMapper.remove(userId, musicId);
            playlistService.removeSong(userId, likedPlaylistId, musicId);
            return false;
        }
        favoriteMapper.add(userId, musicId);
        playlistService.addSong(userId, likedPlaylistId, musicId);
        return true;
    }

    /**
     * 将歌曲加入指定歌单；若为目标为默认喜欢歌单，则同时标记为「已喜欢」。
     */
    @Transactional
    public void addToPlaylist(long userId, long playlistId, long musicId) {
        long defaultId = playlistService.ensureDefaultLikedPlaylistId(userId);
        playlistService.addSong(userId, playlistId, musicId);
        if (playlistId == defaultId) {
            favoriteMapper.add(userId, musicId);
        }
    }

    /**
     * 从歌单移除；若从默认喜欢歌单移除，则取消「已喜欢」标记。
     */
    @Transactional
    public void removeFromPlaylist(long userId, long playlistId, long musicId) {
        long defaultId = playlistService.ensureDefaultLikedPlaylistId(userId);
        playlistService.removeSong(userId, playlistId, musicId);
        if (playlistId == defaultId) {
            favoriteMapper.remove(userId, musicId);
        }
    }

    /**
     * 将歌曲从一首个人歌单移到另一首（用于把收藏改到其他自建歌单）。
     */
    @Transactional
    public void moveBetweenPlaylists(long userId, long musicId, long fromPlaylistId, long toPlaylistId) {
        if (fromPlaylistId == toPlaylistId) {
            return;
        }
        long defaultId = playlistService.ensureDefaultLikedPlaylistId(userId);
        playlistService.removeSong(userId, fromPlaylistId, musicId);
        playlistService.addSong(userId, toPlaylistId, musicId);
        if (fromPlaylistId == defaultId) {
            favoriteMapper.remove(userId, musicId);
        }
        if (toPlaylistId == defaultId) {
            favoriteMapper.add(userId, musicId);
        }
    }
}
