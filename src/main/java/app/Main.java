package app;

import javax.swing.SwingUtilities;

import gui.MainFrame;
import manager.MusicLibrary;
import manager.PlaylistManager;
import player.MusicPlayer;
import service.FileStorage;
import service.MetadataReader;

/**
 * Main is the starting point of the program.
 *
 * When the program runs, Java starts from this method:
 * public static void main(String[] args)
 */
public class Main {

    public static void main(String[] args) {
        /**
         * SwingUtilities.invokeLater is the safe way to start a Swing GUI.
         *
         * Beginner explanation:
         * Swing has its own GUI thread. This line tells Java:
         * "Start the window on the correct GUI thread."
         */
        SwingUtilities.invokeLater(() -> {
            MusicLibrary musicLibrary = new MusicLibrary();
            PlaylistManager playlistManager = new PlaylistManager();
            MusicPlayer musicPlayer = new MusicPlayer();
            FileStorage fileStorage = new FileStorage();
            MetadataReader metadataReader = new MetadataReader();

            /**
             * Load saved data before showing the window.
             *
             * Order matters:
             * 1. Load songs first.
             * 2. Load playlists second, because playlists refer to saved songs.
             *
             * Version 5 note:
             * We no longer add fake sample songs automatically because real playback is enabled.
             * Users should import real MP3 files using the Import MP3 button.
             */
            fileStorage.loadSongs(musicLibrary);
            fileStorage.loadPlaylists(playlistManager, musicLibrary);

            MainFrame mainFrame = new MainFrame(
                    musicLibrary,
                    playlistManager,
                    musicPlayer,
                    fileStorage,
                    metadataReader
            );

            mainFrame.setVisible(true);
        });
    }
}
