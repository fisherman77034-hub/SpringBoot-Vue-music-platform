package com.music.platform.service;

import com.music.platform.domain.Music;
import com.music.platform.domain.PlayQueueItem;
import com.music.platform.mapper.MusicMapper;
import com.music.platform.mapper.PlayQueueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayQueueService {
    private final PlayQueueMapper playQueueMapper;
    private final MusicMapper musicMapper;

    public PlayQueueService(PlayQueueMapper playQueueMapper, MusicMapper musicMapper) {
        this.playQueueMapper = playQueueMapper;
        this.musicMapper = musicMapper;
    }

    public List<PlayQueueItem> list(long userId) {
        return playQueueMapper.listWithMusic(userId);
    }

    @Transactional
    public void add(long userId, long musicId) {
        Music m = musicMapper.findById(musicId);
        if (m == null) throw new IllegalArgumentException("歌曲不存在");
        int next = playQueueMapper.maxPosition(userId) + 1;
        playQueueMapper.insert(userId, musicId, next);
    }

    @Transactional
    public void remove(long userId, long queueItemId) {
        int n = playQueueMapper.deleteByIdAndUser(queueItemId, userId);
        if (n == 0) throw new IllegalArgumentException("队列项不存在");
        normalizePositions(userId);
    }

    @Transactional
    public void clear(long userId) {
        playQueueMapper.deleteAllByUser(userId);
    }

    /**
     * 用给定歌曲顺序整体替换当前用户的播放队列。
     */
    @Transactional
    public void replace(long userId, List<Long> musicIds) {
        if (musicIds == null) {
            throw new IllegalArgumentException("musicIds 不能为空");
        }
        playQueueMapper.deleteAllByUser(userId);
        for (int i = 0; i < musicIds.size(); i++) {
            long mid = musicIds.get(i);
            Music m = musicMapper.findById(mid);
            if (m == null) {
                throw new IllegalArgumentException("歌曲不存在: " + mid);
            }
            playQueueMapper.insert(userId, mid, i);
        }
    }

    @Transactional
    public void reorder(long userId, List<Long> orderedQueueItemIds) {
        if (orderedQueueItemIds == null || orderedQueueItemIds.isEmpty()) {
            throw new IllegalArgumentException("顺序列表不能为空");
        }
        int n = playQueueMapper.countByUser(userId);
        if (orderedQueueItemIds.size() != n) {
            throw new IllegalArgumentException("队列项数量与当前队列不一致");
        }
        for (Long id : orderedQueueItemIds) {
            if (id == null || playQueueMapper.existsItem(userId, id) == 0) {
                throw new IllegalArgumentException("存在无效的队列项 id");
            }
        }
        for (int i = 0; i < orderedQueueItemIds.size(); i++) {
            playQueueMapper.updatePosition(userId, orderedQueueItemIds.get(i), i);
        }
    }

    private void normalizePositions(long userId) {
        List<PlayQueueItem> items = playQueueMapper.listWithMusic(userId);
        for (int i = 0; i < items.size(); i++) {
            playQueueMapper.updatePosition(userId, items.get(i).getId(), i);
        }
    }
}
