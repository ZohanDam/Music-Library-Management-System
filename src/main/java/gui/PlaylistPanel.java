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

import manager.MusicLibrary;
import manager.PlaylistManager;
import model.Playlist;
import model.Song;
import model.SmartPlaylist;

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

    private MusicLibrary musicLibrary;
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
    private JLabel playlistDetailsLabel;

    private JButton createPlaylistButton;
    private JButton deletePlaylistButton;
    private JButton addSongButton;
    private JButton removeSongButton;

    public PlaylistPanel(
            MusicLibrary musicLibrary,
            PlaylistManager playlistManager,
            Supplier<Song> selectedSongSupplier,
            Runnable dataChangeHandler
    ) {
        this.musicLibrary = musicLibrary;
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
        playlistDetailsLabel = new JLabel("Select a playlist to view its details.");
        playlistSongsPanel.add(playlistDetailsLabel, BorderLayout.SOUTH);
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
        deletePlaylistButton = new JButton("Delete Playlist");
        addSongButton = new JButton("Add Selected Song");
        removeSongButton = new JButton("Remove From Playlist");

        createPlaylistButton.setToolTipText("Create a manual playlist or a smart playlist.");
        deletePlaylistButton.setToolTipText("Delete the selected playlist.");
        addSongButton.setToolTipText("Add the selected library song to the selected manual playlist.");
        removeSongButton.setToolTipText("Remove the selected song from this manual playlist only.");

        buttonPanel.add(createPlaylistButton);
        buttonPanel.add(deletePlaylistButton);
        buttonPanel.add(addSongButton);
        buttonPanel.add(removeSongButton);

        add(buttonPanel, BorderLayout.SOUTH);

        createPlaylistButton.addActionListener(event -> createPlaylist());
        deletePlaylistButton.addActionListener(event -> deleteSelectedPlaylist());
        addSongButton.addActionListener(event -> addSelectedSongToPlaylist());
        removeSongButton.addActionListener(event -> removeSelectedSongFromPlaylist());
    }

    private void createPlaylist() {
        Object[] playlistTypes = { "Manual Playlist", "Smart Playlist" };
        Object selectedType = JOptionPane.showInputDialog(
                this,
                "Choose playlist type:",
                "Create Playlist",
                JOptionPane.PLAIN_MESSAGE,
                null,
                playlistTypes,
                playlistTypes[0]
        );

        if (selectedType == null) {
            return;
        }

        if ("Smart Playlist".equals(selectedType)) {
            createSmartPlaylist();
        } else {
            createManualPlaylist();
        }
    }

    private void createManualPlaylist() {
        String name = askForRequiredText("Enter playlist name:", "Create Manual Playlist");

        if (name == null) {
            return;
        }

        boolean created = playlistManager.createManualPlaylist(name);

        if (!created) {
            showCannotCreatePlaylistMessage();
            return;
        }

        refreshPlaylists();
        saveAfterPlaylistChange();
    }

    private void createSmartPlaylist() {
        String name = askForRequiredText("Enter playlist name:", "Create Smart Playlist");

        if (name == null) {
            return;
        }

        SmartPlaylist.RuleField ruleField = askForSmartRuleField();

        if (ruleField == null) {
            return;
        }

        String targetText;

        if (ruleField == SmartPlaylist.RuleField.ARTIST) {
            targetText = "Enter the artist text to match:";
        } else {
            targetText = "Enter the title text to match:";
        }

        String keyword = askForRequiredText(targetText, "Create Smart Playlist");

        if (keyword == null) {
            return;
        }

        boolean created = playlistManager.createSmartPlaylist(name, musicLibrary, ruleField, keyword);

        if (!created) {
            showCannotCreatePlaylistMessage();
            return;
        }

        refreshPlaylists();
        saveAfterPlaylistChange();
    }

    private String askForRequiredText(String message, String title) {
        while (true) {
            String text = JOptionPane.showInputDialog(
                    this,
                    message,
                    title,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (text == null) {
                return null;
            }

            String trimmedText = text.trim();

            if (!trimmedText.isEmpty()) {
                return trimmedText;
            }

            JOptionPane.showMessageDialog(
                    this,
                    "This field cannot be empty.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private SmartPlaylist.RuleField askForSmartRuleField() {
        Object[] ruleOptions = { "Artist contains", "Title contains" };
        Object selectedRule = JOptionPane.showInputDialog(
                this,
                "Choose a smart rule:",
                "Create Smart Playlist",
                JOptionPane.PLAIN_MESSAGE,
                null,
                ruleOptions,
                ruleOptions[0]
        );

        if (selectedRule == null) {
            return null;
        }

        if ("Artist contains".equals(selectedRule)) {
            return SmartPlaylist.RuleField.ARTIST;
        }

        return SmartPlaylist.RuleField.TITLE;
    }

    private void showCannotCreatePlaylistMessage() {
        JOptionPane.showMessageDialog(
                this,
                "Playlist name is empty, keyword is missing, or the name already exists.",
                "Cannot Create Playlist",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void deleteSelectedPlaylist() {
        Playlist selectedPlaylist = getSelectedPlaylist();

        if (selectedPlaylist == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete this playlist?\n\n" + selectedPlaylist.getName(),
                "Delete Playlist",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean deleted = playlistManager.deletePlaylist(selectedPlaylist);

        if (!deleted) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected playlist could not be deleted.",
                    "Delete Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        refreshPlaylists();
        refreshSelectedPlaylistSongs();
        saveAfterPlaylistChange();
    }

    private void addSelectedSongToPlaylist() {
        Playlist selectedPlaylist = getSelectedPlaylist();
        Song selectedSong = selectedSongSupplier.get();

        if (selectedPlaylist == null || selectedSong == null) {
            return;
        }

        if (!selectedPlaylist.isEditable()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Smart playlists update automatically from their rule.\n"
                            + "You cannot add songs manually.",
                    "Smart Playlist",
                    JOptionPane.INFORMATION_MESSAGE
            );
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

        if (!selectedPlaylist.isEditable()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Smart playlists update automatically from their rule.\n"
                            + "You cannot remove songs manually.",
                    "Smart Playlist",
                    JOptionPane.INFORMATION_MESSAGE
            );
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
        updatePlaylistDetailsLabel(selectedPlaylist);

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

    private void updatePlaylistDetailsLabel(Playlist selectedPlaylist) {
        if (selectedPlaylist == null) {
            playlistDetailsLabel.setText("Select a playlist to view its details.");
            return;
        }

        if (selectedPlaylist instanceof SmartPlaylist) {
            SmartPlaylist smartPlaylist = (SmartPlaylist) selectedPlaylist;
            playlistDetailsLabel.setText("Smart rule: " + smartPlaylist.getRuleSummary());
            return;
        }

        playlistDetailsLabel.setText("Manual playlist: songs are added and removed by the user.");
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
        Playlist selectedPlaylist = getSelectedPlaylist();
        boolean hasSelectedPlaylist = selectedPlaylist != null;
        boolean hasSelectedLibrarySong = selectedSongSupplier.get() != null;
        boolean hasSelectedPlaylistSong = playlistSongList.getSelectedValue() != null;
        boolean canEditSelectedPlaylist = hasSelectedPlaylist && selectedPlaylist.isEditable();

        deletePlaylistButton.setEnabled(hasSelectedPlaylist);
        addSongButton.setEnabled(canEditSelectedPlaylist && hasSelectedLibrarySong);
        removeSongButton.setEnabled(canEditSelectedPlaylist && hasSelectedPlaylistSong);
    }
}
