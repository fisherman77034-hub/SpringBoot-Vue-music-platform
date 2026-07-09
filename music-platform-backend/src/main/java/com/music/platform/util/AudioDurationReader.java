package com.music.platform.util;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;

import java.nio.file.Files;
import java.nio.file.Path;


public final class AudioDurationReader {

    private AudioDurationReader() {
    }

    public static int durationMsFromPath(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return 0;
        }
        try {
            AudioFile audio = AudioFileIO.read(path.toFile());
            AudioHeader h = audio.getAudioHeader();
            double precise = h.getPreciseTrackLength();
            if (precise > 0) {

                return (int) Math.round(precise * 1000.0);
            }
            int sec = h.getTrackLength();
            return sec > 0 ? sec * 1000 : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }
}
