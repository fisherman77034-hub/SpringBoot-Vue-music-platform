package com.music.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public class AppAdminProperties {
    //登录即管理员
    private boolean enabled = true;
    //管理员账号密码
    private String username = "admin";
    private String password = "admin123";
    private String nickname = "管理员";

     // 当该用户已存在，但当前密码与 {@link #password} 校验不一致时，是否写入新的哈希。


    private boolean syncPasswordIfNotMatches = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isSyncPasswordIfNotMatches() {
        return syncPasswordIfNotMatches;
    }

    public void setSyncPasswordIfNotMatches(boolean syncPasswordIfNotMatches) {
        this.syncPasswordIfNotMatches = syncPasswordIfNotMatches;
    }
}
