package service;

/**
 * MetadataInfo is a small data holder for information read from an MP3 file.
 *
 * It is separate from Song because reading metadata can fail or be incomplete.
 * Example: an MP3 file might have a title but no artist.
 */
public class MetadataInfo {

    private String title;
    private String artist;
    private int durationSeconds;

    public MetadataInfo(String title, String artist, int durationSeconds) {
        this.title = cleanText(title);
        this.artist = cleanText(artist);
        this.durationSeconds = Math.max(durationSeconds, 0);
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public boolean hasTitle() {
        return !title.isEmpty();
    }

    public boolean hasArtist() {
        return !artist.isEmpty();
    }

    public boolean hasCompleteTitleAndArtist() {
        return hasTitle() && hasArtist();
    }
}
