-- 用户单曲评分（每人每首歌最多一条，修改评分时同步汇总表）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS music_user_rating
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
