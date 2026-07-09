package com.music.platform.service;

import com.music.platform.common.ConflictException;
import com.music.platform.common.LoginAuthException;
import com.music.platform.domain.Mood;
import com.music.platform.domain.Role;
import com.music.platform.domain.User;
import com.music.platform.mapper.UserMapper;
import com.music.platform.security.JwtService;
import com.music.platform.web.dto.UpdateProfileRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PlaylistService playlistService;

    public UserService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PlaylistService playlistService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.playlistService = playlistService;
    }

    @Transactional
    public long register(String username, String password, String nickname) {
        String u = username == null ? "" : username.trim();
        String nick = nickname == null ? "" : nickname.trim();
        if (!StringUtils.hasText(u)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(nick)) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (userMapper.findByUsername(u) != null) {
            throw new ConflictException("用户名已存在");
        }
        User user = new User();
        user.setUsername(u);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nick);
        user.setMood(Mood.CALM);
        user.setRole(Role.USER);
        userMapper.insert(user);
        playlistService.ensureDefaultLikedPlaylistId(user.getId());
        return user.getId();
    }

    public String login(String username, String password) {
        String u = username == null ? "" : username.trim();
        if (!StringUtils.hasText(u)) {
            throw new LoginAuthException("用户名不能为空");
        }
        User row = userMapper.findByUsername(u);
        if (row == null) {
            throw new LoginAuthException("账号不存在");
        }
        if (row.getPasswordHash() == null || !passwordEncoder.matches(password, row.getPasswordHash())) {
            throw new LoginAuthException("密码不正确");
        }
        if (row.getRole() == null) {
            throw new LoginAuthException("账号数据异常，请联系管理员");
        }
        return jwtService.issueToken(row.getId(), row.getUsername(), row.getRole().name());
    }

    public User getById(long id) {
        User u = userMapper.findById(id);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        return u;
    }

    @Transactional
    public void updateMood(long userId, Mood mood) {
        userMapper.updateMood(userId, mood.name());
    }

    @Transactional
    public void updateProfile(long userId, UpdateProfileRequest req) {
        getById(userId);
        String genderStr = req.getGender() == null ? null : req.getGender().name();
        userMapper.updateProfile(
                userId,
                req.getNickname().trim(),
                req.getBio(),
                genderStr,
                req.getAge()
        );
    }

    @Transactional
    public void updateAvatarPath(long userId, String relativePath) {
        getById(userId);
        userMapper.updateAvatarPath(userId, relativePath);
    }
}

