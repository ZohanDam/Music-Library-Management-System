# Music Library Management System - Version 8

A beginner-friendly Java Swing desktop application for managing and playing an MP3 music library.

This project is built for an Object-Oriented Programming and Data Structures course. The code includes comments that explain the purpose of each major class and method.

## Main Features

- Import one or multiple MP3 files
- Read MP3 metadata automatically using Jaudiotagger
- Ask the user to type title/artist manually if metadata is missing
- Display imported songs in a song list
- Search songs by title or artist
- Sort songs by title
- Sort songs by artist
- Create playlists
- Add songs to playlists
- Remove songs from playlists
- Delete songs from the library
- Edit song title/artist
- Save and load songs automatically
- Save and load playlists automatically
- Play real MP3 audio using JLayer
- Pause and resume playback
- Next and previous song controls
- Shuffle mode
- Repeat current song mode
- Repeat all songs mode
- Show approximate current playback time

## Project Structure

```text
src/main/java/
├── app/
│   └── Main.java
├── gui/
│   ├── MainFrame.java
│   ├── SongPanel.java
│   ├── PlaylistPanel.java
│   └── ControlPanel.java
├── manager/
│   ├── MusicLibrary.java
│   └── PlaylistManager.java
├── model/
│   ├── Song.java
│   └── Playlist.java
├── player/
│   └── MusicPlayer.java
└── service/
    ├── FileStorage.java
    ├── MetadataInfo.java
    └── MetadataReader.java
```

## Important Classes

### Song
Represents one music file.

Stores:
- title
- artist
- file path
- duration in seconds

### Playlist
Represents one playlist.

Stores:
- playlist name
- songs inside that playlist

### MusicLibrary
Stores all imported songs.

Main actions:
- add song
- remove song
- search songs
- sort songs by title
- sort songs by artist
- find song by file path

### PlaylistManager
Stores all playlists.

Main actions:
- create playlist
- delete playlist
- add/remove songs from playlists
- remove a deleted song from every playlist

### MusicPlayer
Handles real MP3 playback.

Main actions:
- play
- pause
- resume
- next
- previous
- shuffle
- repeat one
- repeat all
- approximate current time

The GUI does not directly play audio. It calls methods in `MusicPlayer`.

### FileStorage
Saves and loads data from:

```text
data/songs.txt
data/playlists.txt
```

The saved files store song information and playlist information. The MP3 files themselves are not copied into the project.

### MetadataReader
Uses Jaudiotagger to read MP3 title, artist, and duration when possible.

## Maven Dependencies

The project uses Maven to download these libraries:

- Jaudiotagger: reads MP3 metadata
- JLayer: plays MP3 audio

## How to Run in Eclipse

1. Extract the project folder.
2. Open Eclipse.
3. Go to:

```text
File → Import → Maven → Existing Maven Projects
```

4. Select the extracted project folder.
5. Click Finish.
6. Right-click the project:

```text
Maven → Update Project
```

7. Run:

```text
app.Main
```

## How to Test Version 8

### Import multiple MP3 files

1. Click `Import MP3(s)`.
2. Select multiple `.mp3` files.
3. Click Open.
4. Check the import summary.
5. Imported songs should appear in the song list.

### Sort

1. Import several songs.
2. Click `Sort Title`.
3. Songs should appear alphabetically by title.
4. Click `Sort Artist`.
5. Songs should appear alphabetically by artist.

### Shuffle

1. Import at least three songs.
2. Select a song and click Play.
3. Turn on `Shuffle`.
4. Click Next.
5. The next song should be chosen randomly, not strictly in list order.

### Repeat current song

1. Play a song.
2. Click the Repeat button until it shows `Repeat: One`.
3. When the song finishes, the same song should start again.

### Repeat all songs

1. Play songs from the song list.
2. Click the Repeat button until it shows `Repeat: All`.
3. When the last song finishes, playback should return to the first song.

### Current playback time

1. Play a song.
2. Watch the time label.
3. It should show an approximate time such as:

```text
Time: 00:15 / 03:42
```

The time display is approximate because MP3 playback is frame-based.

## Beginner Explanation of New Version 7 Features

### Multiple Import
The file chooser allows multiple selected files. The program uses a loop to import each file one by one.

### Sorting
The program uses `Comparator` to tell Java how to order Song objects.

### Shuffle
Shuffle uses Java's `Random` class to choose the next song. It avoids choosing the same song twice in a row when possible.

### Repeat
Repeat is controlled by an enum:

```java
OFF, ONE, ALL
```

This is easier to understand than using unclear numbers such as 0, 1, and 2.

### Current Time
The player counts approximate MP3 playback time while audio frames are decoded. The GUI uses a Swing Timer to refresh the label once per second.

## Known Limitations

- The time label is approximate, not millisecond-perfect.
- There is no draggable progress bar or seeking.
- There is no volume control yet.
- If an MP3 file is moved or deleted from the computer, the app may not be able to play it.
- MP3 files are not copied into the project folder; only their local file paths are saved.

## Recommended GitHub Note

Do not upload personal MP3 files to GitHub.

Recommended `.gitignore` entries:

```text
target/
.classpath
.project
.settings/
*.mp3
music/
data/songs.txt
data/playlists.txt
```

## Version 8 UI Polish

Version 8 focuses on making the interface cleaner and easier to use.

Changes:

- Song controls are split into smaller rows instead of one long row.
- Playlist controls are stacked vertically so they stay visible on narrow windows.
- Playlist list and playlist-song list use a vertical split to improve readability.
- Playback controls are grouped into two clear rows.
- The separate Resume button was removed.
- The Play button changes to Resume only when the current song is paused.
- The main window has a minimum size to prevent important controls from being hidden.
- Tooltips were added to explain common buttons.

### Playback UI Behavior

Normal state:

```text
Play | Pause | Previous | Next
```

Paused state:

```text
Resume | Pause disabled
```

There is no longer a second Resume button. This avoids confusion for users.
