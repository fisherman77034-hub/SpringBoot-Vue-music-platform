package com.music.platform.mapper;

import com.music.platform.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findById(@Param("id") long id);
    User findByUsername(@Param("username") String username);
    int insert(User user);
    int updateMood(@Param("id") long id, @Param("mood") String mood);

    int updatePasswordHash(@Param("id") long id, @Param("passwordHash") String passwordHash);

    int updateProfile(
            @Param("id") long id,
            @Param("nickname") String nickname,
            @Param("bio") String bio,
            @Param("gender") String gender,
            @Param("age") Integer age
    );

    int updateAvatarPath(@Param("id") long id, @Param("avatarPath") String avatarPath);
}

