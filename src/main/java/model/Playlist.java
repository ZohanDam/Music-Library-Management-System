package model;

import java.util.List;

/**
 * Playlist is now an abstract parent class.
 *
 * Beginner idea:
 * - All playlists have some shared information such as a name.
 * - But not all playlists behave the same way.
 * - A manual playlist stores songs that the user adds.
 * - A smart playlist calculates songs automatically from a rule.
 *
 * This is a simple example of inheritance and polymorphism:
 * - ManualPlaylist extends Playlist
 * - SmartPlaylist extends Playlist
 * - Other classes can work with Playlist variables and let each subclass decide
 *   how getSongs(), addSong(), and removeSong() should behave.
 */
public abstract class Playlist {

    private String name;

    public Playlist(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    /**
     * Returns a short save-friendly type name such as MANUAL or SMART.
     */
    public abstract String getPlaylistType();

    /**
     * Returns true if the user can directly add/remove songs in this playlist.
     */
    public abstract boolean isEditable();

    public abstract boolean addSong(Song song);

    public abstract boolean removeSong(Song song);

    public abstract List<Song> getSongs();

    public boolean containsSong(Song song) {
        return getSongs().contains(song);
    }

    /**
     * JList uses this to display playlist names.
     */
    @Override
    public String toString() {
        return name;
    }
}
