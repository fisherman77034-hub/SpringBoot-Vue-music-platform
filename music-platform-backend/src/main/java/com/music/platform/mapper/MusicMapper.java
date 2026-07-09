package com.music.platform.mapper;

import com.music.platform.domain.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MusicMapper {
    Music findById(@Param("id") long id);

    List<Music> list(@Param("offset") int offset, @Param("limit") int limit);

    List<Music> search(@Param("kw") String kw, @Param("offset") int offset, @Param("limit") int limit);

    List<Music> recommendByMood(@Param("mood") String mood, @Param("limit") int limit);

    int insert(Music music);

    int incPlayCount(@Param("id") long id);

    long countAdmin(@Param("kw") String kw);

    List<Music> pageAdmin(@Param("kw") String kw, @Param("offset") int offset, @Param("limit") int limit);

    int updateFull(Music music);

    int deleteById(@Param("id") long id);
}

