package com.music.platform.service;

import com.music.platform.domain.MusicComment;
import com.music.platform.mapper.InteractionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InteractionService {
    private final InteractionMapper interactionMapper;

    public InteractionService(InteractionMapper interactionMapper) {
        this.interactionMapper = interactionMapper;
    }

    @Transactional
    public void comment(long userId, long musicId, String content) {
        if (content == null || content.trim().isEmpty()) throw new IllegalArgumentException("评论不能为空");
        interactionMapper.addComment(musicId, userId, content.trim());
    }

    public List<MusicComment> comments(long musicId) {
        return interactionMapper.listComments(musicId);
    }

    public Integer findUserStar(long userId, long musicId) {
        return interactionMapper.findUserStar(userId, musicId);
    }

    /**
     * 每位用户对每首歌仅保留一条评分；首次评分写入汇总，修改评分时替换汇总中的星级分布。
     */
    @Transactional
    public void rate(long userId, long musicId, int star) {
        if (star < 1 || star > 5) throw new IllegalArgumentException("评分范围1-5");
        interactionMapper.ensureRatingRow(musicId);
        Integer old = interactionMapper.findUserStar(userId, musicId);
        if (old == null) {
            interactionMapper.insertUserRating(userId, musicId, star);
            interactionMapper.addRating(musicId, star);
        } else if (old == star) {
            return;
        } else {
            interactionMapper.updateUserRatingStar(userId, musicId, star);
            interactionMapper.replaceRatingStat(musicId, old, star);
        }
    }

    public Map<String, Object> ratingStat(long musicId) {
        Map<String, Object> map = interactionMapper.getRatingStat(musicId);
        if (map == null || map.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("star1Count", 0);
            empty.put("star2Count", 0);
            empty.put("star3Count", 0);
            empty.put("star4Count", 0);
            empty.put("star5Count", 0);
            empty.put("ratingCount", 0);
            empty.put("ratingSum", 0);
            empty.put("avgScore", 0.0);
            return empty;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("star1Count", toIntObject(mapGetIgnoreCase(map, "star1_count")));
        out.put("star2Count", toIntObject(mapGetIgnoreCase(map, "star2_count")));
        out.put("star3Count", toIntObject(mapGetIgnoreCase(map, "star3_count")));
        out.put("star4Count", toIntObject(mapGetIgnoreCase(map, "star4_count")));
        out.put("star5Count", toIntObject(mapGetIgnoreCase(map, "star5_count")));
        out.put("ratingCount", toIntObject(mapGetIgnoreCase(map, "rating_count")));
        out.put("ratingSum", toIntObject(mapGetIgnoreCase(map, "rating_sum")));
        out.put("avgScore", toDoubleObject(mapGetIgnoreCase(map, "avg_score")));
        return out;
    }

    /** MyBatis {@code resultType=map} 下键名大小写因驱动而异，按忽略大小写取值。 */
    private static Object mapGetIgnoreCase(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        if (map.containsKey(key)) {
            return map.get(key);
        }
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String k = e.getKey();
            if (k != null && k.equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static int toIntObject(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s) {
            try {
                return (int) Math.round(Double.parseDouble(s.trim()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static double toDoubleObject(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}

