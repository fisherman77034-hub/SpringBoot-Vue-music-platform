package com.music.platform.web.dto;

import com.music.platform.domain.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    @Size(max = 64)
    private String nickname;

    @NotNull
    @Size(max = 256)
    private String bio;

    private Gender gender;

    @Min(1)
    @Max(150)
    private Integer age;
}
