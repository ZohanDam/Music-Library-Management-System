package manager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import model.Song;

/**
 * MusicLibrary stores and manages all songs in the application.
 *
 * Important responsibility:
 * - This class handles song data.
 * - The GUI should ask this class for songs instead of managing song data directly.
 */
public class MusicLibrary {

    private ArrayList<Song> songs;

    public MusicLibrary() {
        this.songs = new ArrayList<>();
    }

    /**
     * Adds a song to the library.
     *
     * The song is not added if:
     * - the song is null
     * - the same song file already exists
     */
    public boolean addSong(Song song) {
        if (song == null) {
            return false;
        }

        if (songs.contains(song)) {
            return false;
        }

        songs.add(song);
        return true;
    }

    public boolean removeSong(Song song) {
        return songs.remove(song);
    }

    /**
     * Checks if the library already contains a song with the same file path.
     *
     * This is useful when importing MP3 files, because the same file should not
     * be added twice.
     */
    public boolean containsSongFilePath(String filePath) {
        return findSongByFilePath(filePath) != null;
    }

    /**
     * Finds one song by file path.
     *
     * This is used when loading playlists from a file. The playlist file stores
     * song file paths, so we need to match those paths back to actual Song objects.
     */
    public Song findSongByFilePath(String filePath) {
        if (filePath == null) {
            return null;
        }

        for (Song song : songs) {
            if (filePath.equals(song.getFilePath())) {
                return song;
            }
        }

        return null;
    }

    /**
     * Sorts the main library by song title.
     *
     * Beginner explanation:
     * Comparator tells Java which field should be used for ordering Song objects.
     */
    public void sortByTitle() {
        songs.sort(Comparator.comparing(song -> song.getTitle().toLowerCase()));
    }

    /**
     * Sorts the main library by artist name. If two songs have the same artist,
     * their titles are used as a second sorting rule.
     */
    public void sortByArtist() {
        songs.sort(Comparator
                .comparing((Song song) -> song.getArtist().toLowerCase())
                .thenComparing(song -> song.getTitle().toLowerCase()));
    }

    /**
     * Returns a copy of all songs.
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

    /**
     * Searches songs by title OR artist.
     *
     * Search rules:
     * - case-insensitive
     * - partial match
     *
     * Example:
     * Searching "love" can match:
     * - title: "Love Story"
     * - artist: "Lovejoy"
     */
    public List<Song> searchSongs(String keyword) {
        ArrayList<Song> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllSongs();
        }

        String lowerKeyword = keyword.trim().toLowerCase();

        for (Song song : songs) {
            String lowerTitle = song.getTitle().toLowerCase();
            String lowerArtist = song.getArtist().toLowerCase();

            if (lowerTitle.contains(lowerKeyword) || lowerArtist.contains(lowerKeyword)) {
                results.add(song);
            }
        }

        return results;
    }
}
