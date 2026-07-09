package com.music.platform.web.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReplacePlayQueueRequest {
    @NotNull(message = "musicIds 不能为空")
    private List<Long> musicIds;
}
