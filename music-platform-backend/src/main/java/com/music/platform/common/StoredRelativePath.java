package com.music.platform.common;

import org.springframework.util.StringUtils;


public final class StoredRelativePath {

    private StoredRelativePath() {
    }

    public static String normalizeOrNull(String stored, String subDir) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String p = stored.trim().replace('\\', '/');
        if (p.contains("..")) {
            throw new IllegalArgumentException("非法路径");
        }
        if (!p.contains("/")) {
            return subDir + "/" + p;
        }
        return p;
    }
}
