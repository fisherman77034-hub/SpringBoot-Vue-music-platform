package com.music.platform.bootstrap;

import com.music.platform.config.AppAdminProperties;
import com.music.platform.domain.Mood;
import com.music.platform.domain.Role;
import com.music.platform.domain.User;
import com.music.platform.mapper.UserMapper;
import com.music.platform.service.PlaylistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
@Order(0)
@ConditionalOnProperty(prefix = "app.admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AdminAccountInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final AppAdminProperties props;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PlaylistService playlistService;

    public AdminAccountInitializer(
            AppAdminProperties props,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            PlaylistService playlistService
    ) {
        this.props = props;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.playlistService = playlistService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = props.getUsername() == null ? "" : props.getUsername().trim();
        String plain = props.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(plain)) {
            log.warn("app.admin.username/password 未配置完整，跳过管理员初始化");
            return;
        }

        User existing = userMapper.findByUsername(username);
        if (existing == null) {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(passwordEncoder.encode(plain));
            u.setNickname(StringUtils.hasText(props.getNickname()) ? props.getNickname().trim() : "管理员");
            u.setMood(Mood.CALM);
            u.setRole(Role.ADMIN);
            userMapper.insert(u);
            playlistService.ensureDefaultLikedPlaylistId(u.getId());
            log.info("已创建默认管理员账号: {}", username);
            return;
        }

        if (props.isSyncPasswordIfNotMatches()
                && existing.getPasswordHash() != null
                && !passwordEncoder.matches(plain, existing.getPasswordHash())) {
            userMapper.updatePasswordHash(existing.getId(), passwordEncoder.encode(plain));
            log.info("已修正管理员 [{}] 的密码哈希（与 app.admin.password 一致）", username);
        }
    }
}
