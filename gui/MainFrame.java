package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import manager.MusicLibrary;
import manager.PlaylistManager;
import model.Song;
import player.MusicPlayer;
import service.FileStorage;
import service.MetadataReader;

/**
 * MainFrame is the main application window.
 *
 * It connects the main parts of the program:
 * - SongPanel
 * - PlaylistPanel
 * - ControlPanel
 *
 * Important beginner idea:
 * MainFrame does not store all the logic itself.
 * It connects GUI panels to manager classes and services.
 */
public class MainFrame extends JFrame {

    private MusicLibrary musicLibrary;
    private PlaylistManager playlistManager;
    private MusicPlayer musicPlayer;
    private FileStorage fileStorage;
    private MetadataReader metadataReader;

    private SongPanel songPanel;
    private PlaylistPanel playlistPanel;
    private ControlPanel controlPanel;

    public MainFrame(
            MusicLibrary musicLibrary,
            PlaylistManager playlistManager,
            MusicPlayer musicPlayer,
            FileStorage fileStorage,
            MetadataReader metadataReader
    ) {
        this.musicLibrary = musicLibrary;
        this.playlistManager = playlistManager;
        this.musicPlayer = musicPlayer;
        this.fileStorage = fileStorage;
        this.metadataReader = metadataReader;

        setTitle("Music Library Management System - V8");
        setSize(1060, 700);
        setMinimumSize(new Dimension(880, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        saveDataWhenWindowCloses();
        createPanels();
        connectPanelUpdates();
    }

    private void createPanels() {
        songPanel = new SongPanel(
                musicLibrary,
                metadataReader,
                () -> saveDataAndRefreshPanels(),
                song -> prepareForSongDeletion(song)
        );

        playlistPanel = new PlaylistPanel(
                musicLibrary,
                playlistManager,
                () -> songPanel.getSelectedSong(),
                () -> saveDataAndRefreshPanels()
        );

        controlPanel = new ControlPanel(
                musicPlayer,
                () -> songPanel.getSelectedSong(),
                () -> songPanel.getDisplayedSongs()
        );

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                songPanel,
                playlistPanel
        );

        splitPane.setResizeWeight(0.58);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(610);

        add(splitPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
    }

    /**
     * Whenever selected song changes, buttons in other panels may need to change.
     *
     * Example:
     * - If no song is selected, Add to Playlist should be disabled.
     * - If a song is selected, Play should be enabled.
     */
    private void connectPanelUpdates() {
        songPanel.setSelectionChangeHandler(() -> {
            playlistPanel.updateButtonStates();
            controlPanel.updateButtonStates();
        });
    }

    /**
     * Runs before a song is removed from the main library.
     *
     * Important:
     * A deleted song must also be removed from playlists and from the current playback queue.
     */
    private void prepareForSongDeletion(Song song) {
        playlistManager.removeSongFromAllPlaylists(song);
        musicPlayer.removeSongFromQueue(song);
    }

    /** Saves both songs and playlists. */
    private void saveData() {
        fileStorage.saveAll(musicLibrary, playlistManager);
    }

    /**
     * Saves data and refreshes panels that depend on song information.
     *
     * Example:
     * If a song title changes, playlists and the Now Playing label should show the new title too.
     */
    private void saveDataAndRefreshPanels() {
        saveData();

        if (playlistPanel != null) {
            playlistPanel.refreshAll();
        }

        if (controlPanel != null) {
            controlPanel.refreshPlaybackDisplay();
        }
    }

    private void saveDataWhenWindowCloses() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                // Stop audio before the window closes so no playback thread keeps running.
                musicPlayer.stop();
                saveData();
            }
        });
    }
}
