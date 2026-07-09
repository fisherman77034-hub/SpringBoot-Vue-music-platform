package com.music.platform.service;

import com.music.platform.config.StorageProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Slf4j
@Service
public class StorageService {

    private final StorageProps props;
    /** 与 save / asResource 共用的绝对根目录，避免仅日志用 toAbsolutePath 而实际读写仍用相对路径导致不一致 */
    private final Path storageRoot;

    public StorageService(StorageProps props) {
        this.props = props;
        Path r = Paths.get(props.getRoot());
        if (!r.isAbsolute()) {
            r = Paths.get(System.getProperty("user.dir", ".")).resolve(r).normalize();
        }
        this.storageRoot = r.toAbsolutePath().normalize();
        log.info("本地文件存储根目录（上传与下载均使用此路径）: {}", this.storageRoot);
    }

    public String saveMusic(MultipartFile file) throws IOException {
        return save(file, props.getMusicDir());
    }

    public String saveCover(MultipartFile file) throws IOException {
        return save(file, props.getCoverDir());
    }

    public String saveLyric(MultipartFile file) throws IOException {
        return save(file, props.getLyricDir());
    }

    public String saveAvatar(MultipartFile file) throws IOException {
        return save(file, props.getAvatarDir());
    }

    public Resource asResource(String relativePath) {
        if (!StringUtils.hasText(relativePath) || relativePath.contains("..")) {
            throw new IllegalArgumentException("非法路径");
        }
        // 必须以相对路径拼到 storageRoot；若带前导 /，resolve 会当成绝对路径（Windows 上易跳出根目录）→ 误判非法
        String normalized = relativePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("非法路径");
        }
        Path full = storageRoot.resolve(normalized).normalize();
        if (!full.startsWith(storageRoot)) {
            throw new IllegalArgumentException("非法路径");
        }
        return new FileSystemResource(full);
    }

    /**
     * 将库内相对路径（如 music/xxx.mp3）解析为 storageRoot 下的绝对路径，规则与 {@link #asResource} 一致。
     */
    public Path resolveStoredPath(String relativePath) {
        if (!StringUtils.hasText(relativePath) || relativePath.contains("..")) {
            throw new IllegalArgumentException("非法路径");
        }
        String normalized = relativePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("非法路径");
        }
        Path full = storageRoot.resolve(normalized).normalize();
        if (!full.startsWith(storageRoot)) {
            throw new IllegalArgumentException("非法路径");
        }
        return full;
    }

    public void tryDeleteRelative(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            Path p = resolveStoredPath(relativePath);
            Files.deleteIfExists(p);
        } catch (Exception e) {
            log.warn("删除存储文件失败: {}", relativePath, e);
        }
    }

    private String save(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        String origin = file.getOriginalFilename();
        String ext = "";
        if (origin != null && origin.contains(".")) {
            ext = origin.substring(origin.lastIndexOf('.'));
        }
        int hash = origin == null ? file.hashCode() : origin.hashCode();
        String name = Instant.now().toEpochMilli() + "-" + Math.abs(hash) + ext;
        Path dir = storageRoot.resolve(subDir).normalize();
        if (!dir.startsWith(storageRoot)) {
            throw new IllegalArgumentException("非法子目录");
        }
        Files.createDirectories(dir);
        Path target = dir.resolve(name).normalize();
        file.transferTo(target);
        return subDir + "/" + name;
    }
}
