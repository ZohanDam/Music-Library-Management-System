package model;

import java.util.ArrayList;
import java.util.List;

import manager.MusicLibrary;

/**
 * SmartPlaylist builds its song list automatically from a simple rule.
 *
 * Current supported rules:
 * - title contains some text
 * - artist contains some text
 *
 * Beginner note:
 * SmartPlaylist does not permanently store song objects inside an ArrayList.
 * Instead, each time getSongs() is called, it checks the current music library
 * and returns the songs that match the rule.
 */
public class SmartPlaylist extends Playlist {

    public enum RuleField {
        TITLE,
        ARTIST
    }

    private MusicLibrary musicLibrary;
    private RuleField ruleField;
    private String keyword;

    public SmartPlaylist(String name, MusicLibrary musicLibrary, RuleField ruleField, String keyword) {
        super(name);
        this.musicLibrary = musicLibrary;
        this.ruleField = ruleField == null ? RuleField.TITLE : ruleField;
        this.keyword = keyword == null ? "" : keyword.trim();
    }

    @Override
    public String getPlaylistType() {
        return "SMART";
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    /**
     * Smart playlists update automatically, so direct add is not allowed.
     */
    @Override
    public boolean addSong(Song song) {
        return false;
    }

    /**
     * Smart playlists update automatically, so direct remove is not allowed.
     */
    @Override
    public boolean removeSong(Song song) {
        return false;
    }

    @Override
    public List<Song> getSongs() {
        ArrayList<Song> matchingSongs = new ArrayList<>();

        if (musicLibrary == null || keyword.isEmpty()) {
            return matchingSongs;
        }

        String lowerKeyword = keyword.toLowerCase();

        for (Song song : musicLibrary.getAllSongs()) {
            String textToCheck;

            if (ruleField == RuleField.ARTIST) {
                textToCheck = song.getArtist();
            } else {
                textToCheck = song.getTitle();
            }

            if (textToCheck != null && textToCheck.toLowerCase().contains(lowerKeyword)) {
                matchingSongs.add(song);
            }
        }

        return matchingSongs;
    }

    public RuleField getRuleField() {
        return ruleField;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getRuleSummary() {
        if (ruleField == RuleField.ARTIST) {
            return "Artist contains \"" + keyword + "\"";
        }

        return "Title contains \"" + keyword + "\"";
    }

    /**
     * The list shows that this playlist is smart, so users understand that it
     * works differently from manual playlists.
     */
    @Override
    public String toString() {
        return getName() + " [Smart]";
    }
}
