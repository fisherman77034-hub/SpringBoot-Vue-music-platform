package com.music.platform.service;

import com.music.platform.domain.Music;
import com.music.platform.domain.Playlist;
import com.music.platform.mapper.PlaylistMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class PlaylistService {
    private final PlaylistMapper playlistMapper;

    public PlaylistService(PlaylistMapper playlistMapper) {
        this.playlistMapper = playlistMapper;
    }

    @Transactional
    public long create(long userId, String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("歌单名不能为空");
        Playlist p = new Playlist();
        p.setUserId(userId);
        p.setName(name.trim());
        p.setLikedDefault(false);
        playlistMapper.insert(p);
        return p.getId();
    }

    /**
     * 保证存在默认「我喜欢的音乐」歌单并返回其 id（新用户注册或历史数据补全时自动创建）。
     */
    @Transactional
    public long ensureDefaultLikedPlaylistId(long userId) {
        Playlist existing = playlistMapper.findLikedDefaultByUserId(userId);
        if (existing != null) {
            return existing.getId();
        }
        Playlist p = new Playlist();
        p.setUserId(userId);
        p.setName("我喜欢的音乐");
        p.setLikedDefault(true);
        playlistMapper.insert(p);
        return p.getId();
    }

    public List<Playlist> myPlaylists(long userId) {
        ensureDefaultLikedPlaylistId(userId);
        return playlistMapper.listByUserId(userId);
    }

    public List<Music> listSongs(long userId, long playlistId) {
        Playlist p = playlistMapper.findById(playlistId);
        if (p == null) throw new IllegalArgumentException("歌单不存在");
        if (!Objects.equals(p.getUserId(), userId)) throw new IllegalArgumentException("无权限");
        return playlistMapper.listSongs(playlistId);
    }

    @Transactional
    public void addSong(long userId, long playlistId, long musicId) {
        Playlist p = playlistMapper.findById(playlistId);
        if (p == null) throw new IllegalArgumentException("歌单不存在");
        if (!Objects.equals(p.getUserId(), userId)) throw new IllegalArgumentException("无权限");
        playlistMapper.addSong(playlistId, musicId);
    }

    @Transactional
    public void removeSong(long userId, long playlistId, long musicId) {
        Playlist p = playlistMapper.findById(playlistId);
        if (p == null) throw new IllegalArgumentException("歌单不存在");
        if (!Objects.equals(p.getUserId(), userId)) throw new IllegalArgumentException("无权限");
        playlistMapper.removeSong(playlistId, musicId);
    }

    /**
     * 删除用户自建歌单（默认「我喜欢的音乐」不可删）；关联的 playlist_song 由外键级联删除。
     */
    @Transactional
    public void deletePlaylist(long userId, long playlistId) {
        Playlist p = playlistMapper.findById(playlistId);
        if (p == null) throw new IllegalArgumentException("歌单不存在");
        if (!Objects.equals(p.getUserId(), userId)) throw new IllegalArgumentException("无权限");
        if (Boolean.TRUE.equals(p.getLikedDefault())) {
            throw new IllegalArgumentException("默认收藏歌单不可删除");
        }
        int n = playlistMapper.deleteByIdForUser(playlistId, userId);
        if (n != 1) throw new IllegalStateException("删除失败");
    }
}

