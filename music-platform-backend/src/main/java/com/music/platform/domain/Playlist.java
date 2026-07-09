package com.music.platform.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Playlist {
    private Long id;
    private Long userId;
    private String name;
    // 系统默认「我喜欢的音乐」歌单
    private Boolean likedDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

