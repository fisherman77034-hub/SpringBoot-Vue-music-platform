package com.music.platform.mapper;

import com.music.platform.domain.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FavoriteMapper {
    int add(@Param("userId") long userId, @Param("musicId") long musicId);
    int remove(@Param("userId") long userId, @Param("musicId") long musicId);
    int exists(@Param("userId") long userId, @Param("musicId") long musicId);
    List<Music> list(@Param("userId") long userId);
}

