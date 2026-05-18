package service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import manager.MusicLibrary;
import manager.PlaylistManager;
import model.Playlist;
import model.Song;

/**
 * FileStorage saves and loads songs/playlists using simple text files.
 *
 * Files created:
 * - data/songs.txt
 * - data/playlists.txt
 *
 * Beginner explanation:
 * Without saving, every imported song disappears when the app closes.
 * This class writes the important data to files and reads it again on startup.
 */
public class FileStorage {

    private static final Path DATA_FOLDER = Paths.get("data");
    private static final Path SONGS_FILE = DATA_FOLDER.resolve("songs.txt");
    private static final Path PLAYLISTS_FILE = DATA_FOLDER.resolve("playlists.txt");

    /**
     * Saves all songs to data/songs.txt.
     *
     * Each line stores one song:
     * encodedTitle|encodedArtist|encodedFilePath|durationSeconds
     *
     * Why encoded text?
     * Some titles or paths may contain special characters. Base64 encoding helps
     * keep the save file safer. This is NOT encryption; it is only formatting.
     */
    public void saveSongs(MusicLibrary musicLibrary) {
        ensureDataFolderExists();

        ArrayList<String> lines = new ArrayList<>();

        for (Song song : musicLibrary.getAllSongs()) {
            String line = encode(song.getTitle()) + "|"
                    + encode(song.getArtist()) + "|"
                    + encode(song.getFilePath()) + "|"
                    + song.getDurationSeconds();

            lines.add(line);
        }

        writeLinesSafely(SONGS_FILE, lines);
    }

    /**
     * Loads songs from data/songs.txt into MusicLibrary.
     *
     * Return value:
     * - number of songs successfully loaded
     */
    public int loadSongs(MusicLibrary musicLibrary) {
        if (!Files.exists(SONGS_FILE)) {
            return 0;
        }

        int loadedCount = 0;

        try {
            List<String> lines = Files.readAllLines(SONGS_FILE, StandardCharsets.UTF_8);

            for (String line : lines) {
                Song song = parseSongLine(line);

                if (song != null && musicLibrary.addSong(song)) {
                    loadedCount++;
                }
            }
        } catch (IOException exception) {
            System.out.println("Could not load songs: " + exception.getMessage());
        }

        return loadedCount;
    }

    /**
     * Saves all playlists to data/playlists.txt.
     *
     * Each line stores one playlist:
     * encodedPlaylistName|encodedSongPath1|encodedSongPath2|...
     *
     * We store song paths instead of full song details because songs are already
     * saved in songs.txt. When loading, each path is matched back to a Song object.
     */
    public void savePlaylists(PlaylistManager playlistManager) {
        ensureDataFolderExists();

        ArrayList<String> lines = new ArrayList<>();

        for (Playlist playlist : playlistManager.getAllPlaylists()) {
            StringBuilder line = new StringBuilder();
            line.append(encode(playlist.getName()));

            for (Song song : playlist.getSongs()) {
                line.append("|").append(encode(song.getFilePath()));
            }

            lines.add(line.toString());
        }

        writeLinesSafely(PLAYLISTS_FILE, lines);
    }

    /**
     * Loads playlists from data/playlists.txt.
     *
     * Important:
     * Songs must be loaded first. Otherwise the playlist file paths cannot be
     * matched to real Song objects.
     */
    public int loadPlaylists(PlaylistManager playlistManager, MusicLibrary musicLibrary) {
        if (!Files.exists(PLAYLISTS_FILE)) {
            return 0;
        }

        int loadedCount = 0;

        try {
            List<String> lines = Files.readAllLines(PLAYLISTS_FILE, StandardCharsets.UTF_8);

            for (String line : lines) {
                Playlist playlist = parsePlaylistLine(line, musicLibrary);

                if (playlist != null && playlistManager.addPlaylist(playlist)) {
                    loadedCount++;
                }
            }
        } catch (IOException exception) {
            System.out.println("Could not load playlists: " + exception.getMessage());
        }

        return loadedCount;
    }

    /**
     * Saves songs and playlists together.
     *
     * MainFrame calls this after changes and when the app closes.
     */
    public void saveAll(MusicLibrary musicLibrary, PlaylistManager playlistManager) {
        saveSongs(musicLibrary);
        savePlaylists(playlistManager);
    }

    private Song parseSongLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split("\\|", -1);

        if (parts.length != 4) {
            return null;
        }

        try {
            String title = decode(parts[0]);
            String artist = decode(parts[1]);
            String filePath = decode(parts[2]);
            int durationSeconds = Integer.parseInt(parts[3]);

            if (title.isBlank() || filePath.isBlank()) {
                return null;
            }

            if (artist.isBlank()) {
                artist = "Unknown Artist";
            }

            return new Song(title, artist, filePath, durationSeconds);
        } catch (Exception exception) {
            return null;
        }
    }

    private Playlist parsePlaylistLine(String line, MusicLibrary musicLibrary) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split("\\|", -1);

        if (parts.length < 1) {
            return null;
        }

        try {
            String playlistName = decode(parts[0]);

            if (playlistName.isBlank()) {
                return null;
            }

            Playlist playlist = new Playlist(playlistName);

            for (int i = 1; i < parts.length; i++) {
                String filePath = decode(parts[i]);
                Song song = musicLibrary.findSongByFilePath(filePath);

                if (song != null) {
                    playlist.addSong(song);
                }
            }

            return playlist;
        } catch (Exception exception) {
            return null;
        }
    }

    private void ensureDataFolderExists() {
        try {
            Files.createDirectories(DATA_FOLDER);
        } catch (IOException exception) {
            System.out.println("Could not create data folder: " + exception.getMessage());
        }
    }

    private void writeLinesSafely(Path filePath, List<String> lines) {
        try {
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.out.println("Could not save file " + filePath + ": " + exception.getMessage());
        }
    }

    private String encode(String text) {
        if (text == null) {
            text = "";
        }

        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String encodedText) {
        byte[] bytes = Base64.getDecoder().decode(encodedText);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
