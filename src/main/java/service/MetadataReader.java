package service;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

/**
 * MetadataReader reads title, artist, and duration from MP3 files.
 *
 * Library used:
 * - jaudiotagger
 *
 * Beginner explanation:
 * Many MP3 files contain hidden information called metadata.
 * Metadata can include title, artist, album, duration, and more.
 *
 * This class keeps metadata code outside the GUI, so SongPanel stays cleaner.
 */
public class MetadataReader {

    /**
     * Tries to read metadata from one audio file.
     *
     * If reading fails, this method returns empty metadata instead of crashing.
     * That is important because not all MP3 files have clean metadata.
     */
    public MetadataInfo readMetadata(File audioFile) {
        if (audioFile == null || !audioFile.isFile()) {
            return new MetadataInfo("", "", 0);
        }

        try {
            AudioFile file = AudioFileIO.read(audioFile);
            Tag tag = file.getTag();

            String title = "";
            String artist = "";

            if (tag != null) {
                title = tag.getFirst(FieldKey.TITLE);
                artist = tag.getFirst(FieldKey.ARTIST);
            }

            int durationSeconds = 0;

            if (file.getAudioHeader() != null) {
                durationSeconds = file.getAudioHeader().getTrackLength();
            }

            return new MetadataInfo(title, artist, durationSeconds);
        } catch (Exception exception) {
            // Beginner note:
            // We do not show a big error here because missing/broken metadata is common.
            // The GUI will simply ask the user to type the missing information.
            return new MetadataInfo("", "", 0);
        }
    }
}
