package com.music.platform.web.dto;

import com.music.platform.domain.Mood;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MusicResponse {
    private long id;
    private String title;
    private String artist;
    private String album;
    private Mood moodTag;
    private int durationMs;
    private long playCount;
    private String musicUrl;
    private String coverUrl;
    private String lyricUrl;
}

