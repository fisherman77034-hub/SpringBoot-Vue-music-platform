-- 已有库升级：用户头像、签名、性别、年龄
-- 在 music_platform 库中执行一次即可

ALTER TABLE app_user
    ADD COLUMN avatar_path VARCHAR(512) NULL COMMENT '头像相对存储路径' AFTER nickname,
    ADD COLUMN bio VARCHAR(256) NULL COMMENT '个性签名' AFTER avatar_path,
    ADD COLUMN gender VARCHAR(16) NULL COMMENT 'MALE/FEMALE/OTHER' AFTER bio,
    ADD COLUMN age INT NULL COMMENT '年龄' AFTER gender;

ALTER TABLE app_user
    ADD CONSTRAINT chk_app_user_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    ADD CONSTRAINT chk_app_user_age CHECK (age IS NULL OR (age >= 1 AND age <= 150));
