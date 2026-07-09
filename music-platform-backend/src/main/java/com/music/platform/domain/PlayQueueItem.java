package com.music.platform.domain;

import lombok.Data;

@Data
public class PlayQueueItem {
    private Long id;
    private Long userId;
    private Long musicId;
    private Integer position;
    private Music music;
}
