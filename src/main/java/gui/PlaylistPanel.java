package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import manager.PlaylistManager;
import model.Playlist;
import model.Song;

/**
 * PlaylistPanel is the GUI section for playlist actions.
 *
 * Responsibilities:
 * - create playlists
 * - show playlists
 * - show songs inside the selected playlist
 * - add selected library song to selected playlist
 * - remove selected playlist song
 * - tell MainFrame to save data when playlists change
 *
 * UI design note:
 * Version 8 uses a vertical split for playlists and playlist songs.
 * This is easier to read when the right side of the window becomes narrow.
 */
public class PlaylistPanel extends JPanel {

    private PlaylistManager playlistManager;

    // Supplier<Song> means this panel can ask another panel for the selected song.
    // In this project, it asks SongPanel for the selected song.
    private Supplier<Song> selectedSongSupplier;

    // Called when playlist data changes so MainFrame can save the data.
    private Runnable dataChangeHandler;

    private DefaultListModel<Playlist> playlistListModel;
    private JList<Playlist> playlistList;

    private DefaultListModel<Song> playlistSongListModel;
    private JList<Song> playlistSongList;

    private JButton createPlaylistButton;
    private JButton addSongButton;
    private JButton removeSongButton;

    public PlaylistPanel(
            PlaylistManager playlistManager,
            Supplier<Song> selectedSongSupplier,
            Runnable dataChangeHandler
    ) {
        this.playlistManager = playlistManager;
        this.selectedSongSupplier = selectedSongSupplier;
        this.dataChangeHandler = dataChangeHandler;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        createListArea();
        createButtonArea();

        refreshPlaylists();
        updateButtonStates();
    }

    private void createListArea() {
        playlistListModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistListModel);
        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        playlistSongListModel = new DefaultListModel<>();
        playlistSongList = new JList<>(playlistSongListModel);
        playlistSongList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel playlistPanel = new JPanel(new BorderLayout(4, 4));
        playlistPanel.add(new JLabel("Your playlists"), BorderLayout.NORTH);
        playlistPanel.add(new JScrollPane(playlistList), BorderLayout.CENTER);
        playlistPanel.setBorder(BorderFactory.createTitledBorder("Playlists"));

        JPanel playlistSongsPanel = new JPanel(new BorderLayout(4, 4));
        playlistSongsPanel.add(new JLabel("Songs in selected playlist"), BorderLayout.NORTH);
        playlistSongsPanel.add(new JScrollPane(playlistSongList), BorderLayout.CENTER);
        playlistSongsPanel.setBorder(BorderFactory.createTitledBorder("Playlist Songs"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                playlistPanel,
                playlistSongsPanel
        );
        splitPane.setResizeWeight(0.45);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);

        add(splitPane, BorderLayout.CENTER);

        playlistList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                refreshSelectedPlaylistSongs();
                updateButtonStates();
            }
        });

        playlistSongList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
    }

    /** Buttons are stacked vertically so they remain visible on narrow windows. */
    private void createButtonArea() {
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Playlist Actions"));

        createPlaylistButton = new JButton("Create Playlist");
        addSongButton = new JButton("Add Selected Song");
        removeSongButton = new JButton("Remove From Playlist");

        createPlaylistButton.setToolTipText("Create a new empty playlist.");
        addSongButton.setToolTipText("Add the selected library song to the selected playlist.");
        removeSongButton.setToolTipText("Remove the selected song from this playlist only.");

        buttonPanel.add(createPlaylistButton);
        buttonPanel.add(addSongButton);
        buttonPanel.add(removeSongButton);

        add(buttonPanel, BorderLayout.SOUTH);

        createPlaylistButton.addActionListener(event -> createPlaylist());
        addSongButton.addActionListener(event -> addSelectedSongToPlaylist());
        removeSongButton.addActionListener(event -> removeSelectedSongFromPlaylist());
    }

    private void createPlaylist() {
        String name = JOptionPane.showInputDialog(
                this,
                "Enter playlist name:",
                "Create Playlist",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name == null) {
            return;
        }

        boolean created = playlistManager.createPlaylist(name);

        if (!created) {
            JOptionPane.showMessageDialog(
                    this,
                    "Playlist name is empty or already exists.",
                    "Cannot Create Playlist",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        refreshPlaylists();
        saveAfterPlaylistChange();
    }

    private void addSelectedSongToPlaylist() {
        Playlist selectedPlaylist = getSelectedPlaylist();
        Song selectedSong = selectedSongSupplier.get();

        if (selectedPlaylist == null || selectedSong == null) {
            return;
        }

        boolean added = selectedPlaylist.addSong(selectedSong);

        if (!added) {
            JOptionPane.showMessageDialog(
                    this,
                    "This song is already in the selected playlist.",
                    "Duplicate Song",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            saveAfterPlaylistChange();
        }

        refreshSelectedPlaylistSongs();
        updateButtonStates();
    }

    private void removeSelectedSongFromPlaylist() {
        Playlist selectedPlaylist = getSelectedPlaylist();
        Song selectedPlaylistSong = playlistSongList.getSelectedValue();

        if (selectedPlaylist == null || selectedPlaylistSong == null) {
            return;
        }

        boolean removed = selectedPlaylist.removeSong(selectedPlaylistSong);

        if (removed) {
            saveAfterPlaylistChange();
        }

        refreshSelectedPlaylistSongs();
        updateButtonStates();
    }

    private void saveAfterPlaylistChange() {
        if (dataChangeHandler != null) {
            dataChangeHandler.run();
        }
    }

    private void refreshPlaylists() {
        Playlist selectedBeforeRefresh = getSelectedPlaylist();

        playlistListModel.clear();
        List<Playlist> playlists = playlistManager.getAllPlaylists();

        for (Playlist playlist : playlists) {
            playlistListModel.addElement(playlist);
        }

        if (selectedBeforeRefresh != null) {
            playlistList.setSelectedValue(selectedBeforeRefresh, true);
        }

        updateButtonStates();
    }

    private void refreshSelectedPlaylistSongs() {
        playlistSongListModel.clear();

        Playlist selectedPlaylist = getSelectedPlaylist();

        if (selectedPlaylist == null) {
            return;
        }

        for (Song song : selectedPlaylist.getSongs()) {
            playlistSongListModel.addElement(song);
        }
    }

    private Playlist getSelectedPlaylist() {
        return playlistList.getSelectedValue();
    }

    /**
     * Refreshes this panel after song data changes elsewhere.
     *
     * Example:
     * If a song is deleted from the main library, it must disappear from playlist views too.
     */
    public void refreshAll() {
        refreshPlaylists();
        refreshSelectedPlaylistSongs();
        updateButtonStates();
    }

    /**
     * Disables buttons when the action does not make sense.
     * This prevents many beginner-level user errors.
     */
    public void updateButtonStates() {
        boolean hasSelectedPlaylist = getSelectedPlaylist() != null;
        boolean hasSelectedLibrarySong = selectedSongSupplier.get() != null;
        boolean hasSelectedPlaylistSong = playlistSongList.getSelectedValue() != null;

        addSongButton.setEnabled(hasSelectedPlaylist && hasSelectedLibrarySong);
        removeSongButton.setEnabled(hasSelectedPlaylist && hasSelectedPlaylistSong);
    }
}
