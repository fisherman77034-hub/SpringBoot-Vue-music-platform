package com.music.platform.security;

public record JwtUser(long userId, String username, String role) {
}

