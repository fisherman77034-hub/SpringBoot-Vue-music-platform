-- 已有库（早期无 CHECK 的版本）可手动执行，为枚举字段增加约束。要求 MySQL 8.0.16+。
-- 若表中已有非法 mood/role/mood_tag 值，请先修正数据再执行。

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_mood CHECK (mood IN ('HAPPY', 'SAD', 'CALM', 'ENERGETIC', 'FOCUS')),
    ADD CONSTRAINT chk_app_user_role CHECK (role IN ('USER', 'ADMIN'));

ALTER TABLE music
    ADD CONSTRAINT chk_music_mood_tag CHECK (mood_tag IN ('HAPPY', 'SAD', 'CALM', 'ENERGETIC', 'FOCUS'));
