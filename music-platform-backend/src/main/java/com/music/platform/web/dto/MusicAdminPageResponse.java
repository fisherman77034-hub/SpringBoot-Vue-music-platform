package com.music.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MusicAdminPageResponse {
    private List<MusicResponse> records;
    private long total;
}
