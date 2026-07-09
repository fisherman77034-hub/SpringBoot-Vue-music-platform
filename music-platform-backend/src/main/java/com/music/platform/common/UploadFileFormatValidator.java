package com.music.platform.common;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class UploadFileFormatValidator {

    private static final Set<String> AUDIO_EXT = Set.of("mp3", "flac", "wav", "m4a", "ogg", "aac", "opus", "webm");
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> LYRIC_EXT = Set.of("lrc", "txt");

    private UploadFileFormatValidator() {
    }

    public static void requireAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择音频文件");
        }
        String ext = extension(file.getOriginalFilename());
        if (!AUDIO_EXT.contains(ext)) {
            throw new IllegalArgumentException("音频格式不正确，仅支持：" + joinCn(AUDIO_EXT));
        }
    }

    public static void optionalImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String ext = extension(file.getOriginalFilename());
        if (!IMAGE_EXT.contains(ext)) {
            throw new IllegalArgumentException("封面格式不正确，仅支持：jpg、jpeg、png、gif、webp");
        }
    }

    public static void optionalLyric(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        String ext = extension(file.getOriginalFilename());
        if (!LYRIC_EXT.contains(ext)) {
            throw new IllegalArgumentException("歌词格式不正确，仅支持 .lrc 与 .txt");
        }
    }

    private static String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        String name = filename.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String joinCn(Set<String> set) {
        return set.stream().sorted().collect(Collectors.joining("、"));
    }
}
