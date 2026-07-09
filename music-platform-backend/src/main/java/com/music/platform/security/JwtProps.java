package com.music.platform.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProps {
    private String issuer;
    private String secret;
    private long expireMinutes;
}

