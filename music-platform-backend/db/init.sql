
-- 建库：CREATE DATABASE music_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS music_comment;
DROP TABLE IF EXISTS music_user_rating;
DROP TABLE IF EXISTS music_rating_stat;
DROP TABLE IF EXISTS user_favorite_music;
DROP TABLE IF EXISTS playlist_song;
DROP TABLE IF EXISTS playlist;
DROP TABLE IF EXISTS music;
DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL UNIQUE COMMENT '登录名，唯一',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码哈希',
    nickname      VARCHAR(64)  NOT NULL COMMENT '展示昵称',
    avatar_path   VARCHAR(512) NULL COMMENT '头像相对存储路径',
    bio           VARCHAR(256) NULL COMMENT '个性签名',
    gender        VARCHAR(16)  NULL COMMENT 'MALE/FEMALE/OTHER，空表示未填写',
    age           INT          NULL COMMENT '年龄，可选',
    mood          VARCHAR(32)  NOT NULL DEFAULT 'CALM' COMMENT '情绪：HAPPY/SAD/CALM/ENERGETIC/FOCUS',
    role          VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_app_user_mood CHECK (mood IN ('HAPPY', 'SAD', 'CALM', 'ENERGETIC', 'FOCUS')),
    CONSTRAINT chk_app_user_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_app_user_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_app_user_age CHECK (age IS NULL OR (age >= 1 AND age <= 150))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE music
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(128) NOT NULL,
    artist      VARCHAR(128) NOT NULL,
    album       VARCHAR(128) NULL,
    mood_tag    VARCHAR(32)  NOT NULL DEFAULT 'CALM' COMMENT '推荐用情绪标签',
    duration_ms INT          NOT NULL DEFAULT 0,
    music_path  VARCHAR(255) NOT NULL,
    cover_path  VARCHAR(255) NULL,
    lyric_path  VARCHAR(255) NULL,
    play_count  BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_music_title (title),
    INDEX idx_music_artist (artist),
    INDEX idx_music_mood (mood_tag),
    CONSTRAINT chk_music_mood_tag CHECK (mood_tag IN ('HAPPY', 'SAD', 'CALM', 'ENERGETIC', 'FOCUS'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE playlist
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    name           VARCHAR(128) NOT NULL,
    liked_default  TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '1=系统默认「我喜欢的音乐」歌单',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE playlist_song
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    playlist_id BIGINT   NOT NULL,
    music_id    BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_playlist_music (playlist_id, music_id),
    CONSTRAINT fk_ps_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_favorite_music
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    music_id   BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_music (user_id, music_id),
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE user_play_queue
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    music_id   BIGINT NOT NULL,
    position   INT    NOT NULL COMMENT '队列顺序，从 0 递增',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_upq_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_upq_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE,
    INDEX idx_upq_user_pos (user_id, position)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE music_rating_stat
(
    music_id      BIGINT PRIMARY KEY,
    star1_count   INT NOT NULL DEFAULT 0,
    star2_count   INT NOT NULL DEFAULT 0,
    star3_count   INT NOT NULL DEFAULT 0,
    star4_count   INT NOT NULL DEFAULT 0,
    star5_count   INT NOT NULL DEFAULT 0,
    rating_count  INT NOT NULL DEFAULT 0,
    rating_sum    INT NOT NULL DEFAULT 0,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rating_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE music_comment
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    music_id   BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    content    VARCHAR(800) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    INDEX idx_comment_music (music_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE music_user_rating
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    music_id   BIGINT   NOT NULL,
    star       INT      NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_mur_user_music (user_id, music_id),
    CONSTRAINT fk_mur_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_mur_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE,
    CONSTRAINT chk_mur_star CHECK (star >= 1 AND star <= 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 默认管理员账号：admin / admin123（BCrypt 10轮）
INSERT INTO app_user(username, password_hash, nickname, mood, role)
VALUES ('admin', '$2b$10$43TqRl1VUkMeaQnzdwRP8ehz7Vn9EKtj19sgkBfuB1YvYJr5G0QL.', '管理员', 'CALM', 'ADMIN');

INSERT INTO playlist (user_id, name, liked_default)
SELECT u.id, '我喜欢的音乐', 1
FROM app_user u
WHERE NOT EXISTS (
    SELECT 1 FROM playlist p WHERE p.user_id = u.id AND p.liked_default = 1
);

SET FOREIGN_KEY_CHECKS = 1;

