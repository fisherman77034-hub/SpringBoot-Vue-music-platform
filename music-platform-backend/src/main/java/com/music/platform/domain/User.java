package com.music.platform.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatarPath;
    private String bio;
    private Gender gender;
    private Integer age;
    private Mood mood;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

