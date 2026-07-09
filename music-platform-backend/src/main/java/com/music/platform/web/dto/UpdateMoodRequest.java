package com.music.platform.web.dto;

import com.music.platform.domain.Mood;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMoodRequest {
    @NotNull
    private Mood mood;
}

