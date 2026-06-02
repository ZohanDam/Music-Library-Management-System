package model;

import java.util.Objects;

/**
 * The Song class represents ONE song in the music library.
 *
 * Beginner idea:
 * - A class is a blueprint.
 * - A Song object is one real song created from this blueprint.
 *
 * Example Song object:
 * title = "Yellow"
 * artist = "Coldplay"
 * filePath = "C:/Music/yellow.mp3"
 * durationSeconds = 269
 */
public class Song {

    // These fields are private to protect the data.
    // Other classes should use getters/setters instead of directly changing them.
    private String title;
    private String artist;
    private String filePath;
    private int durationSeconds;

    /**
     * Constructor: used when creating a new Song object.
     */
    public Song(String title, String artist, String filePath, int durationSeconds) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.durationSeconds = durationSeconds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    /**
     * Returns a readable format for showing the song in the GUI.
     */
    public String getDisplayText() {
        if (durationSeconds > 0) {
            return title + " - " + artist + " (" + formatDuration(durationSeconds) + ")";
        }

        return title + " - " + artist;
    }

    /**
     * Converts seconds into a simple mm:ss format.
     * Example: 185 seconds becomes 3:05.
     */
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * JList automatically calls toString() when displaying objects.
     * Returning getDisplayText() makes the GUI show title, artist, and duration.
     */
    @Override
    public String toString() {
        return getDisplayText();
    }

    /**
     * Two songs are considered the same if they have the same file path.
     *
     * Why filePath?
     * - The same song file should not be added twice.
     * - Title and artist can sometimes be repeated by different songs.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Song)) {
            return false;
        }

        Song otherSong = (Song) other;
        return Objects.equals(filePath, otherSong.filePath);
    }

    /**
     * When equals() is overridden, hashCode() should also be overridden.
     * This keeps Java collections working correctly.
     */
    @Override
    public int hashCode() {
        return Objects.hash(filePath);
    }
}
