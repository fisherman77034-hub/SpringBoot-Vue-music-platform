package com.music.platform.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Music {
    private Long id;
    private String title;
    private String artist;
    private String album;
    private Mood moodTag;
    private Integer durationMs;
    private String musicPath;
    private String coverPath;
    private String lyricPath;
    private Long playCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

