package com.music.platform.web.dto;

import com.music.platform.domain.Gender;
import com.music.platform.domain.Mood;
import com.music.platform.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Gender gender;
    private Integer age;
    private Mood mood;
    private Role role;
}

