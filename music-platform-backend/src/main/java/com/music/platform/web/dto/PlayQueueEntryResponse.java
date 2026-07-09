package com.music.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayQueueEntryResponse {
    private long queueItemId;
    private MusicResponse music;
}
