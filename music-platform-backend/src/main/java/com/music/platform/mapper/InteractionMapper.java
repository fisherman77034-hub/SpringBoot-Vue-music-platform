package com.music.platform.mapper;

import com.music.platform.domain.MusicComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface InteractionMapper {
    int addComment(@Param("musicId") long musicId, @Param("userId") long userId, @Param("content") String content);
    List<MusicComment> listComments(@Param("musicId") long musicId);

    int ensureRatingRow(@Param("musicId") long musicId);
    int addRating(@Param("musicId") long musicId, @Param("star") int star);
    Map<String, Object> getRatingStat(@Param("musicId") long musicId);

    Integer findUserStar(@Param("userId") long userId, @Param("musicId") long musicId);

    int insertUserRating(@Param("userId") long userId, @Param("musicId") long musicId, @Param("star") int star);

    int updateUserRatingStar(@Param("userId") long userId, @Param("musicId") long musicId, @Param("star") int star);

    int replaceRatingStat(@Param("musicId") long musicId, @Param("oldStar") int oldStar, @Param("newStar") int newStar);
}

