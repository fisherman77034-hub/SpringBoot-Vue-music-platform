package com.music.platform.mapper;

import com.music.platform.domain.Music;
import com.music.platform.domain.Playlist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlaylistMapper {
    int insert(Playlist playlist);
    List<Playlist> listByUserId(@Param("userId") long userId);

    int addSong(@Param("playlistId") long playlistId, @Param("musicId") long musicId);
    int removeSong(@Param("playlistId") long playlistId, @Param("musicId") long musicId);

    List<Music> listSongs(@Param("playlistId") long playlistId);
    Playlist findById(@Param("id") long id);

    Playlist findLikedDefaultByUserId(@Param("userId") long userId);

    int deleteByIdForUser(@Param("id") long id, @Param("userId") long userId);
}

