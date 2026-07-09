package com.music.platform.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MusicComment {
    private Long id;
    private Long musicId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private String username;
    private String nickname;
}

