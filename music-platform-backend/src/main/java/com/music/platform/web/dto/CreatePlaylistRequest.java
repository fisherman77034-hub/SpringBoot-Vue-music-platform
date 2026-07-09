package com.music.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePlaylistRequest {
    @NotBlank
    @Size(max = 128)
    private String name;
}

