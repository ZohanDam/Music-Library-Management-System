package model;

import java.util.ArrayList;
import java.util.List;

/**
 * The Playlist class represents ONE playlist.
 *
 * A playlist has:
 * - a name
 * - a list of songs
 *
 * Data structure used:
 * - ArrayList<Song>
 */
public class Playlist {

    private String name;
    private ArrayList<Song> songs;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds a song to the playlist.
     *
     * Return value:
     * - true means the song was added
     * - false means the song was not added
     *
     * The song is not added if:
     * - the song is null
     * - the song already exists in the playlist
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

    /**
     * Removes a song from the playlist.
     */
    public boolean removeSong(Song song) {
        return songs.remove(song);
    }

    /**
     * Returns a copy of the song list.
     *
     * Beginner note:
     * Returning a copy protects the real ArrayList inside this class.
     */
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    public boolean containsSong(Song song) {
        return songs.contains(song);
    }

    /**
     * JList uses this to display playlist names.
     */
    @Override
    public String toString() {
        return name;
    }
}
