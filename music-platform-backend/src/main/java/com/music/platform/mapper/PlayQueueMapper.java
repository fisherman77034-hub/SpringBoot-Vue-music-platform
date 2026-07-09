package com.music.platform.mapper;

import com.music.platform.domain.PlayQueueItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlayQueueMapper {
    List<PlayQueueItem> listWithMusic(@Param("userId") long userId);

    int maxPosition(@Param("userId") long userId);

    int insert(@Param("userId") long userId, @Param("musicId") long musicId, @Param("position") int position);

    int deleteByIdAndUser(@Param("id") long id, @Param("userId") long userId);

    int deleteAllByUser(@Param("userId") long userId);

    int updatePosition(@Param("userId") long userId, @Param("id") long id, @Param("position") int position);

    int countByUser(@Param("userId") long userId);

    int existsItem(@Param("userId") long userId, @Param("id") long id);
}
