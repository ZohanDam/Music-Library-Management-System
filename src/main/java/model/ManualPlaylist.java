package model;

import java.util.ArrayList;
import java.util.List;

/**
 * ManualPlaylist is the normal playlist type.
 *
 * Songs are stored directly inside this class.
 * The user can add or remove songs whenever needed.
 */
public class ManualPlaylist extends Playlist {

    private ArrayList<Song> songs;

    public ManualPlaylist(String name) {
        super(name);
        this.songs = new ArrayList<>();
    }

    @Override
    public String getPlaylistType() {
        return "MANUAL";
    }

    @Override
    public boolean isEditable() {
        return true;
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
    @Override
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
    @Override
    public boolean removeSong(Song song) {
        return songs.remove(song);
    }

    /**
     * Returns a copy of the song list.
     *
     * Beginner note:
     * Returning a copy protects the real ArrayList inside this class.
     */
    @Override
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }
}
