-- 播放队列 + 默认「我喜欢的音乐」歌单标记（MySQL 8+）
-- 在已有库上执行本脚本一次。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_play_queue
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

ALTER TABLE playlist
    ADD COLUMN liked_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=系统默认「我喜欢的音乐」歌单';

-- 为尚无默认收藏歌单的用户补建
INSERT INTO playlist (user_id, name, liked_default)
SELECT u.id, '我喜欢的音乐', 1
FROM app_user u
WHERE NOT EXISTS (
    SELECT 1 FROM playlist p WHERE p.user_id = u.id AND p.liked_default = 1
);
