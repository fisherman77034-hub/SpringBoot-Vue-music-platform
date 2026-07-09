package com.music.platform.service;

import com.music.platform.common.UploadFileFormatValidator;
import com.music.platform.domain.Mood;
import com.music.platform.domain.Music;
import com.music.platform.mapper.MusicMapper;
import com.music.platform.util.AudioDurationReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class MusicService {
    private final MusicMapper musicMapper;
    private final StorageService storageService;

    public MusicService(MusicMapper musicMapper, StorageService storageService) {
        this.musicMapper = musicMapper;
        this.storageService = storageService;
    }

    public Music getById(long id) {
        Music m = musicMapper.findById(id);
        if (m == null) throw new IllegalArgumentException("歌曲不存在");
        return m;
    }

    public List<Music> list(int page, int size) {
        int limit = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, page) * limit;
        return musicMapper.list(offset, limit);
    }

    public List<Music> search(String kw, int page, int size) {
        int limit = Math.max(1, Math.min(size, 50));
        int offset = Math.max(0, page) * limit;
        String q = (kw == null) ? "" : kw.trim();
        if (q.isEmpty()) return list(page, size);
        return musicMapper.search(q, offset, limit);
    }

    public List<Music> recommend(Mood mood, int limit) {
        int lim = Math.max(1, Math.min(limit, 20));
        return musicMapper.recommendByMood(mood.name(), lim);
    }

    @Transactional
    public long create(Music music) {
        musicMapper.insert(music);
        return music.getId();
    }

    @Transactional
    public void incPlayCount(long id) {
        musicMapper.incPlayCount(id);
    }

    /** 管理员分页：关键词可空（查全部），单页最多 100 条 */
    public List<Music> adminPage(String kw, int page, int size) {
        int limit = Math.max(1, Math.min(size, 100));
        int offset = Math.max(0, page) * limit;
        String q = kw == null ? "" : kw.trim();
        return musicMapper.pageAdmin(q.isEmpty() ? null : q, offset, limit);
    }

    public long adminCount(String kw) {
        String q = kw == null ? "" : kw.trim();
        return musicMapper.countAdmin(q.isEmpty() ? null : q);
    }

    @Transactional
    public void adminUpdate(
            long id,
            String title,
            String artist,
            String album,
            Mood moodTag,
            int durationMs,
            MultipartFile musicFile,
            MultipartFile coverFile,
            MultipartFile lyricFile,
            boolean removeCover,
            boolean removeLyric
    ) throws IOException {
        Music m = getById(id);
        if (musicFile != null && !musicFile.isEmpty()) {
            UploadFileFormatValidator.requireAudio(musicFile);
        }
        if (coverFile != null && !coverFile.isEmpty()) {
            UploadFileFormatValidator.optionalImage(coverFile);
        }
        if (lyricFile != null && !lyricFile.isEmpty()) {
            UploadFileFormatValidator.optionalLyric(lyricFile);
        }

        m.setTitle(title.trim());
        m.setArtist(artist.trim());
        m.setAlbum(StringUtils.hasText(album) ? album.trim() : null);
        m.setMoodTag(moodTag);

        if (musicFile != null && !musicFile.isEmpty()) {
            String path = storageService.saveMusic(musicFile);
            m.setMusicPath(path);
            int detected = AudioDurationReader.durationMsFromPath(storageService.resolveStoredPath(path));
            m.setDurationMs(detected > 0 ? detected : durationMs);
        } else {
            m.setDurationMs(durationMs);
        }
        if (removeCover) {
            m.setCoverPath(null);
        } else if (coverFile != null && !coverFile.isEmpty()) {
            m.setCoverPath(storageService.saveCover(coverFile));
        }
        if (removeLyric) {
            m.setLyricPath(null);
        } else if (lyricFile != null && !lyricFile.isEmpty()) {
            m.setLyricPath(storageService.saveLyric(lyricFile));
        }

        musicMapper.updateFull(m);
    }

    @Transactional
    public void adminDelete(long id) {
        Music m = getById(id);
        final String musicPath = m.getMusicPath();
        final String coverPath = m.getCoverPath();
        final String lyricPath = m.getLyricPath();
        int n = musicMapper.deleteById(id);
        if (n == 0) {
            throw new IllegalArgumentException("歌曲不存在");
        }
        Runnable cleanup = () -> {
            storageService.tryDeleteRelative(musicPath);
            storageService.tryDeleteRelative(coverPath);
            storageService.tryDeleteRelative(lyricPath);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }
}

