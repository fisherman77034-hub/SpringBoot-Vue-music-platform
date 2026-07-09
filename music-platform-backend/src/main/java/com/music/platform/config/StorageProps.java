package com.music.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.storage")
public class StorageProps {
    private String root;
    private String musicDir;
    private String coverDir;
    private String lyricDir;
    private String avatarDir;
}

