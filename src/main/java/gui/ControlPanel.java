package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import model.Song;
import player.MusicPlayer;

/**
 * ControlPanel is the bottom part of the GUI.
 *
 * Responsibilities:
 * - Play or resume the current song
 * - Pause playback
 * - Move to previous or next song
 * - Turn shuffle on or off
 * - Cycle repeat mode: Off -> One -> All -> Off
 * - Show the current song and approximate playback time
 *
 * UI design note:
 * Version 8 removes the separate Resume button.
 * The Play button changes to Resume only when the player is paused.
 * This avoids confusing users with two resume choices.
 */
public class ControlPanel extends JPanel {

    private MusicPlayer musicPlayer;
    private Supplier<Song> selectedSongSupplier;
    private Supplier<List<Song>> displayedSongsSupplier;
    private Runnable playbackStateChangeHandler;

    private JButton previousButton;
    private JButton playButton;
    private JButton pauseButton;
    private JButton nextButton;
    private JCheckBox shuffleCheckBox;
    private JButton repeatButton;
    private JLabel nowPlayingLabel;
    private JLabel timeLabel;
    private Timer timeUpdateTimer;

    public ControlPanel(
            MusicPlayer musicPlayer,
            Supplier<Song> selectedSongSupplier,
            Supplier<List<Song>> displayedSongsSupplier,
            Runnable playbackStateChangeHandler
    ) {
        this.musicPlayer = musicPlayer;
        this.selectedSongSupplier = selectedSongSupplier;
        this.displayedSongsSupplier = displayedSongsSupplier;
        this.playbackStateChangeHandler = playbackStateChangeHandler;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Playback Controls"),
                new EmptyBorder(6, 8, 8, 8)
        ));

        createControls();
        createStatusLabels();
        createTimeUpdateTimer();

        /**
         * MusicPlayer runs audio on a background thread.
         * GUI updates must run on Swing's GUI thread, so SwingUtilities.invokeLater is used.
         */
        musicPlayer.setStateChangeHandler(() -> SwingUtilities.invokeLater(() -> {
            updateStatusLabels();
            updateButtonStates();

            if (playbackStateChangeHandler != null) {
                playbackStateChangeHandler.run();
            }
        }));

        updateButtonStates();
        updateStatusLabels();
    }

    /** Creates buttons in two rows so they remain readable on smaller windows. */
    private void createControls() {
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));

        JPanel transportPanel = new JPanel(new GridLayout(1, 4, 8, 8));
        previousButton = new JButton("Previous");
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        nextButton = new JButton("Next");

        transportPanel.add(previousButton);
        transportPanel.add(playButton);
        transportPanel.add(pauseButton);
        transportPanel.add(nextButton);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        shuffleCheckBox = new JCheckBox("Shuffle");
        repeatButton = new JButton("Repeat: Off");

        modePanel.add(shuffleCheckBox);
        modePanel.add(repeatButton);

        controlsPanel.add(transportPanel);
        controlsPanel.add(modePanel);
        add(controlsPanel, BorderLayout.CENTER);

        previousButton.setToolTipText("Play the previous song in the current list.");
        playButton.setToolTipText("Play the selected song, or resume if playback is paused.");
        pauseButton.setToolTipText("Pause the currently playing song.");
        nextButton.setToolTipText("Play the next song in the current list.");
        shuffleCheckBox.setToolTipText("When enabled, Next chooses a random song.");
        repeatButton.setToolTipText("Cycles between Repeat Off, Repeat One, and Repeat All.");

        previousButton.addActionListener(event -> previousSong());
        playButton.addActionListener(event -> playOrResumeSong());
        pauseButton.addActionListener(event -> pauseSong());
        nextButton.addActionListener(event -> nextSong());

        shuffleCheckBox.addActionListener(event -> {
            musicPlayer.setShuffleEnabled(shuffleCheckBox.isSelected());
            updateButtonStates();
        });

        repeatButton.addActionListener(event -> {
            musicPlayer.cycleRepeatMode();
            repeatButton.setText(musicPlayer.getRepeatModeLabel());
            updateButtonStates();
        });
    }

    private void createStatusLabels() {
        JPanel statusPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        statusPanel.setBorder(new EmptyBorder(4, 0, 0, 0));

        nowPlayingLabel = new JLabel("Now Playing: None");
        timeLabel = new JLabel("Time: 00:00 / --:--");

        statusPanel.add(nowPlayingLabel);
        statusPanel.add(timeLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }

    /** Swing Timer updates only the displayed time label once per second. */
    private void createTimeUpdateTimer() {
        timeUpdateTimer = new Timer(1000, event -> updateTimeLabel());
        timeUpdateTimer.start();
    }

    /**
     * Single main playback button behavior:
     * - If paused on the current song, resume.
     * - Otherwise, play the selected song from the beginning.
     */
    private void playOrResumeSong() {
        Song selectedSong = selectedSongSupplier.get();
        Song currentSong = musicPlayer.getCurrentSong();

        boolean success;

        if (musicPlayer.isPaused() && currentSong != null
                && (selectedSong == null || selectedSong.equals(currentSong))) {
            success = musicPlayer.resume();
        } else if (selectedSong != null) {
            success = musicPlayer.playSong(selectedSong, displayedSongsSupplier.get());
        } else {
            success = false;
        }

        handlePlaybackResult(success);
        refreshPlaybackDisplay();
    }

    private void pauseSong() {
        musicPlayer.pause();
        refreshPlaybackDisplay();
    }

    private void nextSong() {
        boolean success = musicPlayer.next();
        handlePlaybackResult(success);
        refreshPlaybackDisplay();
    }

    private void previousSong() {
        boolean success = musicPlayer.previous();
        handlePlaybackResult(success);
        refreshPlaybackDisplay();
    }

    private void handlePlaybackResult(boolean success) {
        if (success) {
            return;
        }

        String message = musicPlayer.getLastErrorMessage();

        if (message == null || message.isBlank()) {
            message = "Playback could not start.";
        }

        JOptionPane.showMessageDialog(
                this,
                message,
                "Playback Problem",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void updateStatusLabels() {
        updateNowPlayingLabel();
        updateTimeLabel();
        repeatButton.setText(musicPlayer.getRepeatModeLabel());
        shuffleCheckBox.setSelected(musicPlayer.isShuffleEnabled());
    }

    private void updateNowPlayingLabel() {
        Song currentSong = musicPlayer.getCurrentSong();

        if (currentSong == null) {
            nowPlayingLabel.setText("Now Playing: None");
            return;
        }

        if (musicPlayer.isPlaying()) {
            nowPlayingLabel.setText("Now Playing: " + currentSong.getDisplayText());
        } else if (musicPlayer.isPaused()) {
            nowPlayingLabel.setText("Paused: " + currentSong.getDisplayText());
        } else {
            nowPlayingLabel.setText("Stopped: " + currentSong.getDisplayText());
        }
    }

    /** Displays approximate current playback time. */
    private void updateTimeLabel() {
        Song currentSong = musicPlayer.getCurrentSong();

        if (currentSong == null) {
            timeLabel.setText("Time: 00:00 / --:--");
            return;
        }

        int elapsedSeconds = musicPlayer.getElapsedSeconds();
        int durationSeconds = currentSong.getDurationSeconds();

        String totalText = durationSeconds > 0 ? formatTime(durationSeconds) : "--:--";
        timeLabel.setText("Time: " + formatTime(elapsedSeconds) + " / " + totalText);
    }

    private String formatTime(int totalSeconds) {
        int safeSeconds = Math.max(0, totalSeconds);
        int minutes = safeSeconds / 60;
        int seconds = safeSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /** Used by MainFrame after song names are edited or deleted. */
    public void refreshPlaybackDisplay() {
        updateStatusLabels();
        updateButtonStates();
    }

    /** Disables playback buttons when they cannot be used. */
    public void updateButtonStates() {
        Song selectedSong = selectedSongSupplier.get();
        Song currentSong = musicPlayer.getCurrentSong();

        boolean hasSelectedSong = selectedSong != null;
        boolean hasCurrentSong = currentSong != null;
        boolean selectedSongIsCurrentSong = hasSelectedSong && selectedSong.equals(currentSong);
        boolean canResume = hasCurrentSong && musicPlayer.isPaused();
        boolean canPlayNewSelection = hasSelectedSong && (!musicPlayer.isPlaying() || !selectedSongIsCurrentSong);

        previousButton.setEnabled(musicPlayer.hasPrevious());
        playButton.setEnabled(canResume || canPlayNewSelection);
        pauseButton.setEnabled(hasCurrentSong && musicPlayer.isPlaying());
        nextButton.setEnabled(musicPlayer.hasNext());
        shuffleCheckBox.setEnabled(hasCurrentSong || hasSelectedSong);
        repeatButton.setEnabled(hasCurrentSong || hasSelectedSong);

        // There is only one resume option now: the main button changes text.
        if (canResume && (!hasSelectedSong || selectedSongIsCurrentSong)) {
            playButton.setText("Resume");
        } else {
            playButton.setText("Play");
        }
    }
}
