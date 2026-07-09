package com.music.platform.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


@Component
@Order(-100)
public class SchemaCompatInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SchemaCompatInitializer.class);

    private final DataSource dataSource;

    public SchemaCompatInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            ensurePlaylistLikedDefaultColumn(conn);
            ensureUserPlayQueueTable(conn);
            ensureMusicRatingTables(conn);
            backfillDefaultLikedPlaylists(conn);
        } catch (Exception e) {
            log.error(
                    "数据库结构兼容升级失败，s升级数据库原因: {}",
                    e.getMessage(),
                    e
            );
        }
    }

    private void ensurePlaylistLikedDefaultColumn(Connection conn) throws Exception {
        String checkSql = "SELECT COUNT(*) AS c FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'playlist' AND COLUMN_NAME = 'liked_default'";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(checkSql)) {
            rs.next();
            if (rs.getInt("c") > 0) {
                return;
            }
        }
        String alter = "ALTER TABLE playlist ADD COLUMN liked_default TINYINT(1) NOT NULL DEFAULT 0 "
                + "COMMENT '1=系统默认「我喜欢的音乐」歌单'";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(alter);
            log.info("已为 playlist 表添加 liked_default 列");
        }
    }

    private void ensureUserPlayQueueTable(Connection conn) throws Exception {
        String ddl = "CREATE TABLE IF NOT EXISTS user_play_queue ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "music_id BIGINT NOT NULL,"
                + "position INT NOT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_upq_user_pos (user_id, position),"
                + "CONSTRAINT fk_upq_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,"
                + "CONSTRAINT fk_upq_music FOREIGN KEY (music_id) REFERENCES music(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ddl);
        }
        log.info("已确保 user_play_queue 表存在");
    }

    /**
     * 旧库若未执行完整 init.sql，会缺少评分相关表，导致评分接口 SQL 异常（前端表现为内部错误、统计恒为空）。
     */
    private void ensureMusicRatingTables(Connection conn) throws Exception {
        String ratingStat = "CREATE TABLE IF NOT EXISTS music_rating_stat ("
                + "music_id BIGINT PRIMARY KEY,"
                + "star1_count INT NOT NULL DEFAULT 0,"
                + "star2_count INT NOT NULL DEFAULT 0,"
                + "star3_count INT NOT NULL DEFAULT 0,"
                + "star4_count INT NOT NULL DEFAULT 0,"
                + "star5_count INT NOT NULL DEFAULT 0,"
                + "rating_count INT NOT NULL DEFAULT 0,"
                + "rating_sum INT NOT NULL DEFAULT 0,"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "CONSTRAINT fk_rating_music FOREIGN KEY (music_id) REFERENCES music(id) ON DELETE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        String userRating = "CREATE TABLE IF NOT EXISTS music_user_rating ("
                + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "music_id BIGINT NOT NULL,"
                + "star INT NOT NULL,"
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uk_mur_user_music (user_id, music_id),"
                + "CONSTRAINT fk_mur_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,"
                + "CONSTRAINT fk_mur_music FOREIGN KEY (music_id) REFERENCES music(id) ON DELETE CASCADE,"
                + "CONSTRAINT chk_mur_star CHECK (star >= 1 AND star <= 5)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(ratingStat);
            st.executeUpdate(userRating);
        }
        log.info("已确保 music_rating_stat / music_user_rating 表存在");
    }

    private void backfillDefaultLikedPlaylists(Connection conn) throws Exception {
        String insert = "INSERT INTO playlist (user_id, name, liked_default) "
                + "SELECT u.id, '我喜欢的音乐', 1 FROM app_user u "
                + "WHERE NOT EXISTS (SELECT 1 FROM playlist p WHERE p.user_id = u.id AND p.liked_default = 1)";
        try (Statement st = conn.createStatement()) {
            int n = st.executeUpdate(insert);
            if (n > 0) {
                log.info("已为 {} 个用户补建默认「我喜欢的音乐」歌单", n);
            }
        }
    }
}
