package com.music.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlaylistResponse {
    private long id;
    private String name;
    /** 是否为系统默认「我喜欢的音乐」歌单 */
    private boolean defaultLiked;
}

