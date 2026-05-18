package manager;

import java.util.ArrayList;
import java.util.List;

import model.Playlist;
import model.Song;

/**
 * PlaylistManager stores and manages all playlists.
 *
 * Why separate this from MusicLibrary?
 * - MusicLibrary manages songs.
 * - PlaylistManager manages playlists.
 *
 * This keeps each class focused on one main responsibility.
 */
public class PlaylistManager {

    private ArrayList<Playlist> playlists;

    public PlaylistManager() {
        this.playlists = new ArrayList<>();
    }

    /**
     * Creates a new playlist.
     *
     * Rules:
     * - playlist name cannot be empty
     * - duplicate playlist names are not allowed
     * - comparison is case-insensitive
     */
    public boolean createPlaylist(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        if (findPlaylistByName(name) != null) {
            return false;
        }

        playlists.add(new Playlist(name.trim()));
        return true;
    }

    /**
     * Adds an already-created Playlist object.
     *
     * This is mainly used by FileStorage when loading playlists from disk.
     */
    public boolean addPlaylist(Playlist playlist) {
        if (playlist == null) {
            return false;
        }

        if (findPlaylistByName(playlist.getName()) != null) {
            return false;
        }

        playlists.add(playlist);
        return true;
    }

    public boolean deletePlaylist(Playlist playlist) {
        return playlists.remove(playlist);
    }

    /**
     * Removes one song from every playlist.
     *
     * This is used when a song is deleted from the main library. If the song stayed
     * inside playlists after deletion, saved playlists would point to a song that no
     * longer exists in the library.
     */
    public void removeSongFromAllPlaylists(Song song) {
        if (song == null) {
            return;
        }

        for (Playlist playlist : playlists) {
            playlist.removeSong(song);
        }
    }

    /**
     * Finds a playlist by name.
     * Returns null if no playlist is found.
     */
    public Playlist findPlaylistByName(String name) {
        if (name == null) {
            return null;
        }

        for (Playlist playlist : playlists) {
            if (playlist.getName().equalsIgnoreCase(name.trim())) {
                return playlist;
            }
        }

        return null;
    }

    public List<Playlist> getAllPlaylists() {
        return new ArrayList<>(playlists);
    }
}
