package player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import model.Song;

/**
 * MusicPlayer handles real MP3 playback.
 *
 * Beginner explanation:
 * - The GUI should not know how MP3 files are played.
 * - The GUI only calls methods such as playSong(), pause(), resume(), next(), and previous().
 * - This class does the difficult work: opening the MP3 file, starting a background thread,
 *   pausing, resuming, stopping playback, shuffle, repeat, and approximate playback time.
 *
 * Important technical idea:
 * Swing uses one special GUI thread. If we play audio on that same thread, the window can freeze.
 * Therefore, MP3 playback runs on a separate Thread.
 *
 * Resume note:
 * MP3 resume is frame-based, not millisecond-perfect. It should continue very close to the paused
 * position, but it may not be exact to the millisecond.
 */
public class MusicPlayer {

    /** These states describe what the player is currently doing. */
    public enum PlayerState {
        STOPPED,
        PLAYING,
        PAUSED
    }

    /**
     * Repeat modes used by the Repeat button.
     * OFF = stop at the end of the queue.
     * ONE = play the current song again when it finishes.
     * ALL = after the last song finishes, go back to the first song.
     */
    public enum RepeatMode {
        OFF,
        ONE,
        ALL
    }

    private ArrayList<Song> currentQueue;
    private int currentIndex;
    private PlayerState state;
    private RepeatMode repeatMode;
    private boolean shuffleEnabled;

    /**
     * pausedFrame stores the MP3 frame where playback was paused.
     * MP3 files are stored in small blocks called frames.
     */
    private int pausedFrame;
    private int currentFrame;

    /** Approximate current playback time. Used for the time label in the GUI. */
    private long elapsedMilliseconds;

    private Thread playbackThread;
    private Bitstream activeBitstream;
    private AudioDevice activeAudioDevice;

    private boolean pauseRequested;
    private boolean stopRequested;

    /** sessionId prevents old playback threads from changing the state of a newer song. */
    private int sessionId;

    private String lastErrorMessage;
    private Runnable stateChangeHandler;
    private Random random;

    public MusicPlayer() {
        this.currentQueue = new ArrayList<>();
        this.currentIndex = -1;
        this.state = PlayerState.STOPPED;
        this.repeatMode = RepeatMode.OFF;
        this.shuffleEnabled = false;
        this.pausedFrame = 0;
        this.currentFrame = 0;
        this.elapsedMilliseconds = 0;
        this.sessionId = 0;
        this.lastErrorMessage = null;
        this.random = new Random();
    }

    /**
     * Plays a selected song inside a queue.
     * Queue means the list of songs where Next and Previous will move.
     */
    public boolean playSong(Song selectedSong, List<Song> queue) {
        if (selectedSong == null || queue == null || queue.isEmpty()) {
            setError("No song is selected.");
            return false;
        }

        this.currentQueue = new ArrayList<>(queue);
        this.currentIndex = currentQueue.indexOf(selectedSong);

        if (currentIndex == -1) {
            this.currentQueue.add(selectedSong);
            this.currentIndex = currentQueue.size() - 1;
        }

        this.pausedFrame = 0;
        this.currentFrame = 0;
        this.elapsedMilliseconds = 0;
        return startCurrentSongFromFrame(0, 0);
    }

    /** Resumes the current paused song from the saved frame and saved time. */
    public boolean resume() {
        if (getCurrentSong() == null) {
            setError("No song is selected.");
            return false;
        }

        if (state == PlayerState.PLAYING) {
            return true;
        }

        return startCurrentSongFromFrame(pausedFrame, elapsedMilliseconds);
    }

    /**
     * Pauses the current MP3 by saving the current frame/time and closing audio resources.
     * Resume opens the file again, skips to the saved frame, and continues playback.
     */
    public void pause() {
        Bitstream bitstreamToClose;
        AudioDevice audioDeviceToClose;

        synchronized (this) {
            if (state != PlayerState.PLAYING) {
                return;
            }

            pauseRequested = true;
            stopRequested = false;
            pausedFrame = currentFrame;
            state = PlayerState.PAUSED;

            bitstreamToClose = activeBitstream;
            audioDeviceToClose = activeAudioDevice;
        }

        closePlaybackResources(bitstreamToClose, audioDeviceToClose);
        notifyStateChanged();
    }

    /** Stops playback and resets the pause position to the beginning. */
    public void stop() {
        stopCurrentPlayback(true);
    }

    /** Moves to the next song and plays it immediately. */
    public boolean next() {
        if (!hasNext()) {
            setError("There is no next song.");
            return false;
        }

        int nextIndex = chooseNextIndexForUserAction();

        if (nextIndex == -1) {
            setError("There is no next song.");
            return false;
        }

        currentIndex = nextIndex;
        pausedFrame = 0;
        currentFrame = 0;
        elapsedMilliseconds = 0;
        return startCurrentSongFromFrame(0, 0);
    }

    /** Moves to the previous song and plays it immediately. */
    public boolean previous() {
        if (!hasPrevious()) {
            setError("There is no previous song.");
            return false;
        }

        if (currentIndex == 0 && repeatMode == RepeatMode.ALL && currentQueue.size() > 1) {
            currentIndex = currentQueue.size() - 1;
        } else {
            currentIndex--;
        }

        pausedFrame = 0;
        currentFrame = 0;
        elapsedMilliseconds = 0;
        return startCurrentSongFromFrame(0, 0);
    }

    /**
     * Removes a song from the current playback queue.
     * Used when the user deletes a song from the library.
     */
    public void removeSongFromQueue(Song songToRemove) {
        if (songToRemove == null) {
            return;
        }

        boolean removingCurrentSong;

        synchronized (this) {
            Song currentSong = getCurrentSong();
            removingCurrentSong = songToRemove.equals(currentSong);
        }

        if (removingCurrentSong) {
            stop();
        }

        synchronized (this) {
            int indexToRemove = currentQueue.indexOf(songToRemove);

            if (indexToRemove == -1) {
                return;
            }

            currentQueue.remove(indexToRemove);

            if (currentQueue.isEmpty()) {
                currentIndex = -1;
                return;
            }

            if (indexToRemove < currentIndex) {
                currentIndex--;
            } else if (indexToRemove == currentIndex) {
                currentIndex = Math.min(currentIndex, currentQueue.size() - 1);
            }
        }

        notifyStateChanged();
    }

    /** Starts the current song from a specific MP3 frame and time. */
    private boolean startCurrentSongFromFrame(int startFrame, long startingElapsedMilliseconds) {
        Song currentSong = getCurrentSong();

        if (currentSong == null) {
            setError("No song is selected.");
            return false;
        }

        File audioFile = new File(currentSong.getFilePath());

        if (!audioFile.isFile()) {
            setError("The MP3 file could not be found:\n" + currentSong.getFilePath());
            synchronized (this) {
                state = PlayerState.STOPPED;
                pausedFrame = 0;
                currentFrame = 0;
                elapsedMilliseconds = 0;
            }
            notifyStateChanged();
            return false;
        }

        stopCurrentPlayback(false);

        final int thisSession;
        final int safeStartFrame = Math.max(0, startFrame);
        final long safeStartingElapsed = Math.max(0, startingElapsedMilliseconds);

        synchronized (this) {
            sessionId++;
            thisSession = sessionId;
            pausedFrame = safeStartFrame;
            currentFrame = safeStartFrame;
            elapsedMilliseconds = safeStartingElapsed;
            pauseRequested = false;
            stopRequested = false;
            lastErrorMessage = null;
            state = PlayerState.PLAYING;
        }

        playbackThread = new Thread(() -> runPlaybackThread(audioFile, safeStartFrame, thisSession));
        playbackThread.setName("MP3 Playback Thread");
        playbackThread.setDaemon(true);
        playbackThread.start();

        notifyStateChanged();
        return true;
    }

    /** Opens and plays the MP3 file on the background thread. */
    private void runPlaybackThread(File audioFile, int startFrame, int thisSession) {
        Bitstream bitstream = null;
        AudioDevice audioDevice = null;

        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(audioFile))) {
            bitstream = new Bitstream(inputStream);
            Decoder decoder = new Decoder();
            audioDevice = FactoryRegistry.systemRegistry().createAudioDevice();
            audioDevice.open(decoder);

            synchronized (this) {
                if (thisSession != sessionId) {
                    return;
                }

                activeBitstream = bitstream;
                activeAudioDevice = audioDevice;
            }

            skipFrames(bitstream, startFrame, thisSession);
            playFrames(bitstream, decoder, audioDevice, thisSession);
        } catch (Exception exception) {
            handlePlaybackError(thisSession, exception);
        } finally {
            closePlaybackResources(bitstream, audioDevice);
            synchronized (this) {
                if (thisSession == sessionId) {
                    activeBitstream = null;
                    activeAudioDevice = null;
                }
            }
        }
    }

    private void skipFrames(Bitstream bitstream, int framesToSkip, int thisSession) throws Exception {
        int skipped = 0;

        while (skipped < framesToSkip && shouldPlaybackContinue(thisSession)) {
            Header header = bitstream.readFrame();

            if (header == null) {
                break;
            }

            bitstream.closeFrame();
            skipped++;
        }

        synchronized (this) {
            if (thisSession == sessionId) {
                currentFrame = skipped;
                pausedFrame = skipped;
            }
        }
    }

    /** Decodes MP3 frames and sends audio samples to the speakers. */
    private void playFrames(Bitstream bitstream, Decoder decoder, AudioDevice audioDevice, int thisSession) throws Exception {
        while (shouldPlaybackContinue(thisSession)) {
            Header header = bitstream.readFrame();

            if (header == null) {
                handleSongFinishedNaturally(thisSession);
                return;
            }

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            audioDevice.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();

            synchronized (this) {
                if (thisSession == sessionId) {
                    currentFrame++;
                    pausedFrame = currentFrame;
                    elapsedMilliseconds += Math.max(0, Math.round(header.ms_per_frame()));
                }
            }
        }

        handlePlaybackInterrupted(thisSession);
    }

    private synchronized boolean shouldPlaybackContinue(int thisSession) {
        return thisSession == sessionId && state == PlayerState.PLAYING && !pauseRequested && !stopRequested;
    }

    /** Called when the song reaches the real end of the MP3 file. */
    private void handleSongFinishedNaturally(int finishedSession) {
        int nextIndexToPlay;

        synchronized (this) {
            if (finishedSession != sessionId) {
                return;
            }

            nextIndexToPlay = chooseNextIndexAfterSongFinished();

            pausedFrame = 0;
            currentFrame = 0;
            elapsedMilliseconds = 0;
            pauseRequested = false;
            stopRequested = false;

            if (nextIndexToPlay == -1) {
                state = PlayerState.STOPPED;
            } else {
                currentIndex = nextIndexToPlay;
                state = PlayerState.STOPPED;
            }
        }

        notifyStateChanged();

        if (nextIndexToPlay != -1) {
            // Start the next/repeated song from a tiny helper thread.
            // This lets the old playback thread finish closing its resources first.
            Thread autoStartThread = new Thread(() -> {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                startCurrentSongFromFrame(0, 0);
            });
            autoStartThread.setName("MP3 Auto Advance Thread");
            autoStartThread.setDaemon(true);
            autoStartThread.start();
        }
    }

    private int chooseNextIndexAfterSongFinished() {
        if (currentQueue.isEmpty() || currentIndex < 0) {
            return -1;
        }

        if (repeatMode == RepeatMode.ONE) {
            return currentIndex;
        }

        if (shuffleEnabled && currentQueue.size() > 1) {
            return chooseRandomIndexDifferentFromCurrent();
        }

        if (currentIndex < currentQueue.size() - 1) {
            return currentIndex + 1;
        }

        if (repeatMode == RepeatMode.ALL && currentQueue.size() > 0) {
            return 0;
        }

        return -1;
    }

    private int chooseNextIndexForUserAction() {
        if (currentQueue.isEmpty() || currentIndex < 0) {
            return -1;
        }

        if (shuffleEnabled && currentQueue.size() > 1) {
            return chooseRandomIndexDifferentFromCurrent();
        }

        if (currentIndex < currentQueue.size() - 1) {
            return currentIndex + 1;
        }

        if (repeatMode == RepeatMode.ALL && currentQueue.size() > 1) {
            return 0;
        }

        return -1;
    }

    private int chooseRandomIndexDifferentFromCurrent() {
        if (currentQueue.size() <= 1) {
            return currentIndex;
        }

        int randomIndex;

        do {
            randomIndex = random.nextInt(currentQueue.size());
        } while (randomIndex == currentIndex);

        return randomIndex;
    }

    /** Called when playback stops because of Pause, Stop, Next, Previous, or Delete. */
    private void handlePlaybackInterrupted(int interruptedSession) {
        synchronized (this) {
            if (interruptedSession != sessionId) {
                return;
            }

            if (pauseRequested || state == PlayerState.PAUSED) {
                pausedFrame = currentFrame;
                state = PlayerState.PAUSED;
            } else if (stopRequested) {
                pausedFrame = 0;
                currentFrame = 0;
                elapsedMilliseconds = 0;
                state = PlayerState.STOPPED;
            }

            pauseRequested = false;
            stopRequested = false;
        }

        notifyStateChanged();
    }

    /** Handles corrupted files, unsupported files, or other playback errors. */
    private void handlePlaybackError(int errorSession, Exception exception) {
        synchronized (this) {
            if (errorSession != sessionId) {
                return;
            }

            if (pauseRequested || state == PlayerState.PAUSED) {
                pausedFrame = currentFrame;
                state = PlayerState.PAUSED;
                pauseRequested = false;
                stopRequested = false;
                notifyStateChanged();
                return;
            }

            if (stopRequested) {
                pausedFrame = 0;
                currentFrame = 0;
                elapsedMilliseconds = 0;
                state = PlayerState.STOPPED;
                pauseRequested = false;
                stopRequested = false;
                notifyStateChanged();
                return;
            }

            state = PlayerState.STOPPED;
            pausedFrame = 0;
            currentFrame = 0;
            elapsedMilliseconds = 0;
            lastErrorMessage = "Playback failed: " + exception.getMessage();
        }

        notifyStateChanged();
    }

    private void stopCurrentPlayback(boolean resetState) {
        Bitstream bitstreamToClose;
        AudioDevice audioDeviceToClose;

        synchronized (this) {
            sessionId++;
            pauseRequested = false;
            stopRequested = true;
            bitstreamToClose = activeBitstream;
            audioDeviceToClose = activeAudioDevice;
            activeBitstream = null;
            activeAudioDevice = null;

            if (resetState) {
                state = PlayerState.STOPPED;
                pausedFrame = 0;
                currentFrame = 0;
                elapsedMilliseconds = 0;
            }
        }

        closePlaybackResources(bitstreamToClose, audioDeviceToClose);

        if (resetState) {
            notifyStateChanged();
        }
    }

    private void closePlaybackResources(Bitstream bitstream, AudioDevice audioDevice) {
        if (audioDevice != null) {
            try {
                audioDevice.flush();
            } catch (Exception ignored) {
            }

            try {
                audioDevice.close();
            } catch (Exception ignored) {
            }
        }

        if (bitstream != null) {
            try {
                bitstream.close();
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void setShuffleEnabled(boolean shuffleEnabled) {
        this.shuffleEnabled = shuffleEnabled;
        notifyStateChanged();
    }

    public synchronized boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    /** Cycles OFF -> ONE -> ALL -> OFF each time the Repeat button is clicked. */
    public synchronized void cycleRepeatMode() {
        if (repeatMode == RepeatMode.OFF) {
            repeatMode = RepeatMode.ONE;
        } else if (repeatMode == RepeatMode.ONE) {
            repeatMode = RepeatMode.ALL;
        } else {
            repeatMode = RepeatMode.OFF;
        }

        notifyStateChanged();
    }

    public synchronized RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public synchronized String getRepeatModeLabel() {
        if (repeatMode == RepeatMode.ONE) {
            return "Repeat: One";
        }

        if (repeatMode == RepeatMode.ALL) {
            return "Repeat: All";
        }

        return "Repeat: Off";
    }

    public synchronized int getElapsedSeconds() {
        return (int) (elapsedMilliseconds / 1000);
    }

    private void setError(String message) {
        lastErrorMessage = message;
    }

    private void notifyStateChanged() {
        if (stateChangeHandler != null) {
            stateChangeHandler.run();
        }
    }

    public synchronized Song getCurrentSong() {
        if (currentIndex < 0 || currentIndex >= currentQueue.size()) {
            return null;
        }

        return currentQueue.get(currentIndex);
    }

    public synchronized boolean isPlaying() {
        return state == PlayerState.PLAYING;
    }

    public synchronized boolean isPaused() {
        return state == PlayerState.PAUSED;
    }

    public synchronized PlayerState getState() {
        return state;
    }

    public synchronized boolean hasNext() {
        if (currentQueue.isEmpty() || currentIndex < 0) {
            return false;
        }

        if (shuffleEnabled && currentQueue.size() > 1) {
            return true;
        }

        if (currentIndex < currentQueue.size() - 1) {
            return true;
        }

        return repeatMode == RepeatMode.ALL && currentQueue.size() > 1;
    }

    public synchronized boolean hasPrevious() {
        if (currentQueue.isEmpty() || currentIndex < 0) {
            return false;
        }

        if (currentIndex > 0) {
            return true;
        }

        return repeatMode == RepeatMode.ALL && currentQueue.size() > 1;
    }

    public synchronized String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setStateChangeHandler(Runnable stateChangeHandler) {
        this.stateChangeHandler = stateChangeHandler;
    }
}
