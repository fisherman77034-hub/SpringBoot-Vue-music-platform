package com.music.platform.web.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderPlayQueueRequest {
    @NotEmpty(message = "orderedIds 不能为空")
    private List<Long> orderedIds;
}
