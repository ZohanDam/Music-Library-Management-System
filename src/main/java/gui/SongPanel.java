package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import manager.MusicLibrary;
import model.Song;
import service.MetadataInfo;
import service.MetadataReader;

/**
 * SongPanel is the part of the GUI that shows songs and song-related controls.
 *
 * Responsibilities:
 * - display all songs
 * - search songs by title or artist
 * - import one or multiple MP3 files
 * - sort songs by title or artist
 * - read MP3 metadata when possible
 * - ask the user to type missing title/artist manually
 * - edit title/artist for an existing song
 * - delete a song from the library
 * - let the user select one song
 *
 * Beginner note:
 * This class is a GUI class. It should not contain complex business logic.
 * It asks MusicLibrary to store/search/sort songs, then updates the screen.
 */
public class SongPanel extends JPanel {

    private MusicLibrary musicLibrary;
    private MetadataReader metadataReader;

    private JTextField searchField;
    private JButton searchButton;
    private JButton showAllButton;
    private JButton importSongButton;
    private JButton sortByTitleButton;
    private JButton sortByArtistButton;
    private JButton editSongButton;
    private JButton deleteSongButton;

    private DefaultListModel<Song> songListModel;
    private JList<Song> songList;

    private Runnable dataChangeHandler;
    private Consumer<Song> beforeSongDeleteHandler;
    private Runnable selectionChangeHandler;

    public SongPanel(
            MusicLibrary musicLibrary,
            MetadataReader metadataReader,
            Runnable dataChangeHandler,
            Consumer<Song> beforeSongDeleteHandler
    ) {
        this.musicLibrary = musicLibrary;
        this.metadataReader = metadataReader;
        this.dataChangeHandler = dataChangeHandler;
        this.beforeSongDeleteHandler = beforeSongDeleteHandler;

        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        createTopArea();
        createSongListArea();

        refreshSongList(musicLibrary.getAllSongs());
        updateButtonStates();
    }

    /**
     * Creates search controls and song action buttons.
     *
     * UI design note:
     * Version 8 splits the controls into small rows instead of one long row.
     * This prevents buttons from disappearing when the window is not wide.
     */
    private void createTopArea() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createTitledBorder("Library Songs"));

        searchField = new JTextField(18);
        searchButton = new JButton("Search");
        showAllButton = new JButton("Clear");
        importSongButton = new JButton("Import MP3 Files");
        sortByTitleButton = new JButton("Sort by Title");
        sortByArtistButton = new JButton("Sort by Artist");
        editSongButton = new JButton("Edit Song");
        deleteSongButton = new JButton("Delete Song");

        JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
        searchPanel.setBorder(new EmptyBorder(0, 0, 6, 0));
        searchPanel.add(new JLabel("Search:"), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtonPanel = new JPanel(new GridLayout(1, 2, 6, 6));
        searchButtonPanel.add(searchButton);
        searchButtonPanel.add(showAllButton);
        searchPanel.add(searchButtonPanel, BorderLayout.EAST);

        JPanel actionPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        actionPanel.add(importSongButton);
        actionPanel.add(editSongButton);
        actionPanel.add(deleteSongButton);
        actionPanel.add(sortByTitleButton);
        actionPanel.add(sortByArtistButton);

        searchField.setToolTipText("Search by song title or artist.");
        importSongButton.setToolTipText("Import one or more MP3 files into the library.");
        editSongButton.setToolTipText("Edit the selected song title or artist.");
        deleteSongButton.setToolTipText("Remove the selected song from the app library only.");
        sortByTitleButton.setToolTipText("Sort the full library alphabetically by title.");
        sortByArtistButton.setToolTipText("Sort the full library alphabetically by artist.");

        topPanel.add(searchPanel);
        topPanel.add(actionPanel);
        add(topPanel, BorderLayout.NORTH);

        searchButton.addActionListener(event -> searchSongs());
        searchField.addActionListener(event -> searchSongs());

        showAllButton.addActionListener(event -> {
            searchField.setText("");
            refreshSongList(musicLibrary.getAllSongs());
        });

        importSongButton.addActionListener(event -> importSongs());
        sortByTitleButton.addActionListener(event -> sortSongsByTitle());
        sortByArtistButton.addActionListener(event -> sortSongsByArtist());
        editSongButton.addActionListener(event -> editSelectedSong());
        deleteSongButton.addActionListener(event -> deleteSelectedSong());
    }

    private void createSongListArea() {
        songListModel = new DefaultListModel<>();
        songList = new JList<>(songListModel);
        songList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        songList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                updateButtonStates();

                if (selectionChangeHandler != null) {
                    selectionChangeHandler.run();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(songList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Songs"));
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Opens a file chooser so the user can select one or many MP3 files.
     *
     * Version 7+ behavior:
     * 1. User can select multiple MP3 files at once.
     * 2. The app loops through the selected files.
     * 3. Each valid, non-duplicate MP3 becomes a Song object.
     * 4. If metadata is missing, the popup appears for that file only.
     */
    private void importSongs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose MP3 file(s)");
        fileChooser.setMultiSelectionEnabled(true);

        FileNameExtensionFilter mp3Filter = new FileNameExtensionFilter("MP3 Files (*.mp3)", "mp3");
        fileChooser.setFileFilter(mp3Filter);

        int result = fileChooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File[] selectedFiles = fileChooser.getSelectedFiles();

        // Some systems return only getSelectedFile() even when multi-select is enabled.
        if (selectedFiles == null || selectedFiles.length == 0) {
            File singleFile = fileChooser.getSelectedFile();
            selectedFiles = singleFile == null ? new File[0] : new File[] { singleFile };
        }

        int importedCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;
        int skippedCount = 0;
        Song lastImportedSong = null;

        for (File selectedFile : selectedFiles) {
            ImportResult importResult = importOneSongFile(selectedFile);

            if (importResult == ImportResult.IMPORTED) {
                importedCount++;
                lastImportedSong = musicLibrary.findSongByFilePath(selectedFile.getAbsolutePath());
            } else if (importResult == ImportResult.DUPLICATE) {
                duplicateCount++;
            } else if (importResult == ImportResult.INVALID_FILE) {
                invalidCount++;
            } else if (importResult == ImportResult.SKIPPED_BY_USER) {
                skippedCount++;
            }
        }

        if (importedCount > 0) {
            notifyDataChanged();
            searchField.setText("");
            refreshSongList(musicLibrary.getAllSongs());

            if (lastImportedSong != null) {
                songList.setSelectedValue(lastImportedSong, true);
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "Import summary:\n"
                        + "Imported: " + importedCount + "\n"
                        + "Duplicates: " + duplicateCount + "\n"
                        + "Invalid files: " + invalidCount + "\n"
                        + "Skipped: " + skippedCount,
                "Import Complete",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /** Imports one MP3 file and returns a result so importSongs() can count outcomes. */
    private ImportResult importOneSongFile(File selectedFile) {
        if (!isMp3File(selectedFile)) {
            return ImportResult.INVALID_FILE;
        }

        String filePath = selectedFile.getAbsolutePath();

        if (musicLibrary.containsSongFilePath(filePath)) {
            return ImportResult.DUPLICATE;
        }

        MetadataInfo metadataInfo = metadataReader.readMetadata(selectedFile);
        Song song = createSongFromMetadataOrPopup(filePath, selectedFile.getName(), metadataInfo);

        if (song == null) {
            return ImportResult.SKIPPED_BY_USER;
        }

        boolean added = musicLibrary.addSong(song);
        return added ? ImportResult.IMPORTED : ImportResult.DUPLICATE;
    }

    private enum ImportResult {
        IMPORTED,
        DUPLICATE,
        INVALID_FILE,
        SKIPPED_BY_USER
    }

    /** Sorts the whole library by title and refreshes the displayed list. */
    private void sortSongsByTitle() {
        musicLibrary.sortByTitle();
        notifyDataChanged();
        searchField.setText("");
        refreshSongList(musicLibrary.getAllSongs());
    }

    /** Sorts the whole library by artist and refreshes the displayed list. */
    private void sortSongsByArtist() {
        musicLibrary.sortByArtist();
        notifyDataChanged();
        searchField.setText("");
        refreshSongList(musicLibrary.getAllSongs());
    }

    private void editSelectedSong() {
        Song selectedSong = getSelectedSong();

        if (selectedSong == null) {
            return;
        }

        JTextField titleField = new JTextField(selectedSong.getTitle(), 20);
        JTextField artistField = new JTextField(selectedSong.getArtist(), 20);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Artist:"));
        formPanel.add(artistField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    formPanel,
                    "Edit Song Information",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            String newTitle = titleField.getText().trim();
            String newArtist = artistField.getText().trim();

            if (newTitle.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Title is required.",
                        "Missing Title",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            if (newArtist.isEmpty()) {
                newArtist = "Unknown Artist";
            }

            selectedSong.setTitle(newTitle);
            selectedSong.setArtist(newArtist);

            notifyDataChanged();
            refreshCurrentSearchView(selectedSong);
            return;
        }
    }

    /** Deletes the selected song from the app library, not from the computer. */
    private void deleteSelectedSong() {
        Song selectedSong = getSelectedSong();

        if (selectedSong == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Remove this song from the library?\n\n" + selectedSong.getDisplayText()
                        + "\n\nThe MP3 file on your computer will not be deleted.",
                "Delete Song",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (beforeSongDeleteHandler != null) {
            beforeSongDeleteHandler.accept(selectedSong);
        }

        boolean removed = musicLibrary.removeSong(selectedSong);

        if (!removed) {
            JOptionPane.showMessageDialog(
                    this,
                    "The selected song could not be removed.",
                    "Delete Failed",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        notifyDataChanged();
        refreshCurrentSearchView(null);
    }

    private Song createSongFromMetadataOrPopup(String filePath, String originalFileName, MetadataInfo metadataInfo) {
        if (metadataInfo.hasCompleteTitleAndArtist()) {
            return new Song(
                    metadataInfo.getTitle(),
                    metadataInfo.getArtist(),
                    filePath,
                    metadataInfo.getDurationSeconds()
            );
        }

        return askUserForSongInfo(filePath, originalFileName, metadataInfo);
    }

    private boolean isMp3File(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        return file.getName().toLowerCase().endsWith(".mp3");
    }

    private Song askUserForSongInfo(String filePath, String originalFileName, MetadataInfo metadataInfo) {
        JTextField titleField = new JTextField(20);
        JTextField artistField = new JTextField(20);

        String defaultTitle = metadataInfo.hasTitle()
                ? metadataInfo.getTitle()
                : removeMp3Extension(originalFileName);

        String defaultArtist = metadataInfo.hasArtist()
                ? metadataInfo.getArtist()
                : "Unknown Artist";

        titleField.setText(defaultTitle);
        artistField.setText(defaultArtist);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Artist:"));
        formPanel.add(artistField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    formPanel,
                    "Enter Missing Song Information",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (choice != JOptionPane.OK_OPTION) {
                return null;
            }

            String title = titleField.getText().trim();
            String artist = artistField.getText().trim();

            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Title is required.",
                        "Missing Title",
                        JOptionPane.WARNING_MESSAGE
                );
                continue;
            }

            if (artist.isEmpty()) {
                artist = "Unknown Artist";
            }

            return new Song(title, artist, filePath, metadataInfo.getDurationSeconds());
        }
    }

    private String removeMp3Extension(String fileName) {
        if (fileName == null) {
            return "Untitled";
        }

        if (fileName.toLowerCase().endsWith(".mp3")) {
            return fileName.substring(0, fileName.length() - 4);
        }

        return fileName;
    }

    private void searchSongs() {
        String keyword = searchField.getText();
        List<Song> results = musicLibrary.searchSongs(keyword);
        refreshSongList(results);
    }

    public void refreshSongList(List<Song> songsToDisplay) {
        songListModel.clear();

        for (Song song : songsToDisplay) {
            songListModel.addElement(song);
        }

        updateButtonStates();

        if (selectionChangeHandler != null) {
            selectionChangeHandler.run();
        }
    }

    private void refreshCurrentSearchView(Song songToSelect) {
        List<Song> songsToDisplay = musicLibrary.searchSongs(searchField.getText());
        refreshSongList(songsToDisplay);

        if (songToSelect != null) {
            songList.setSelectedValue(songToSelect, true);
        }
    }

    private void notifyDataChanged() {
        if (dataChangeHandler != null) {
            dataChangeHandler.run();
        }
    }

    private void updateButtonStates() {
        boolean hasSelectedSong = getSelectedSong() != null;
        editSongButton.setEnabled(hasSelectedSong);
        deleteSongButton.setEnabled(hasSelectedSong);
    }

    public Song getSelectedSong() {
        return songList.getSelectedValue();
    }

    /** Returns songs currently visible in the list. MusicPlayer uses this for Next/Previous. */
    public List<Song> getDisplayedSongs() {
        ArrayList<Song> displayedSongs = new ArrayList<>();

        for (int i = 0; i < songListModel.size(); i++) {
            displayedSongs.add(songListModel.getElementAt(i));
        }

        return displayedSongs;
    }

    public void setSelectionChangeHandler(Runnable selectionChangeHandler) {
        this.selectionChangeHandler = selectionChangeHandler;
    }
}
