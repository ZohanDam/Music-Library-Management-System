# Music Library Management System - Learning Guide

This guide is for learning the app as beginners. It is more detailed than the submission version and is meant to help during review, debugging, and viva preparation.

## What the app does

This is a Java Swing desktop app for:

- importing MP3 files
- reading metadata
- storing songs in a library
- creating playlists
- deleting playlists
- creating smart playlists
- playing MP3 files
- saving and loading data

## High-level structure

The project is split into small packages with clear roles:

```text
app       -> starts the program
gui       -> windows, panels, buttons, labels
manager   -> main data management logic
model     -> core objects such as Song and Playlist
player    -> MP3 playback logic
service   -> metadata reading and file saving/loading
```

## Startup flow

The entry point is `app.Main`.

Startup order:

1. Create the main objects
2. Load songs from disk
3. Load playlists from disk
4. Create the main window
5. Show the window

Why songs load before playlists:

- playlists refer to songs by file path
- if songs are not loaded yet, playlists cannot reconnect to them

## Main classes

### `Song`

Represents one song in the library.

Stores:

- title
- artist
- file path
- duration in seconds

Important detail:

- two `Song` objects are considered equal if they have the same file path

That is why duplicate imports can be blocked.

### `Playlist`

This is the abstract parent class.

It stores shared playlist data:

- playlist name

It also defines methods that subclasses must implement:

- `getPlaylistType()`
- `isEditable()`
- `addSong(...)`
- `removeSong(...)`
- `getSongs()`

This is the inheritance and polymorphism part of the project.

### `ManualPlaylist`

This is the normal playlist type.

Behavior:

- stores songs directly
- allows manual add
- allows manual remove

### `SmartPlaylist`

This playlist builds itself from a rule instead of storing songs manually.

Current rules:

- title contains some text
- artist contains some text

Behavior:

- `getSongs()` checks the music library and returns matches
- `addSong(...)` returns `false`
- `removeSong(...)` returns `false`
- `isEditable()` returns `false`

This difference is a clean polymorphism example.

### `MusicLibrary`

Manages all songs in the main library.

Main responsibilities:

- add song
- remove song
- search songs
- sort songs by title
- sort songs by artist
- find song by file path

### `PlaylistManager`

Manages all playlists.

Main responsibilities:

- create manual playlists
- create smart playlists
- store playlists in one list as `Playlist`
- delete playlists
- remove a deleted song from all playlists

### `MusicPlayer`

Handles playback using JLayer.

Main responsibilities:

- play selected song
- pause
- resume
- stop
- next/previous
- shuffle
- repeat mode
- current playback time

Important detail:

- playback runs on a background thread so the GUI does not freeze

### `FileStorage`

Handles saving and loading.

Files used:

```text
data/songs.txt
data/playlists.txt
```

Songs are saved with:

- title
- artist
- file path
- duration

Playlists are saved in two formats:

- manual playlists save song paths
- smart playlists save rule field and keyword

Older playlist files still work because legacy parsing is kept.

### `MetadataReader`

Reads MP3 metadata using Jaudiotagger.

If metadata is missing or invalid:

- the app does not crash
- the GUI asks the user to enter missing data

## GUI structure

### `MainFrame`

This is the main window. It connects all panels together.

It also:

- saves data when the window closes
- removes deleted songs from playlists
- removes deleted songs from the playback queue

### `SongPanel`

Handles library-side actions:

- import songs
- search
- sort
- edit song info
- delete song
- select a song

### `PlaylistPanel`

Handles playlist-side actions:

- create manual playlists
- create smart playlists
- delete playlists
- show playlist songs
- show smart-playlist rule summary
- add/remove songs for manual playlists

### `ControlPanel`

Handles playback actions:

- play
- pause
- previous
- next
- shuffle
- repeat
- now playing label
- time label

## Data flow examples

### Example 1: importing a song

1. `SongPanel` opens file chooser
2. `MetadataReader` reads metadata
3. `SongPanel` creates a `Song`
4. `MusicLibrary` stores it
5. `MainFrame` saves data through `FileStorage`

### Example 2: creating a smart playlist

1. `PlaylistPanel` asks for playlist type
2. User selects `Smart Playlist`
3. `PlaylistPanel` asks for rule field and keyword
4. `PlaylistManager` creates a `SmartPlaylist`
5. When displayed, `getSongs()` pulls matching songs from `MusicLibrary`

### Example 3: deleting a song

1. `SongPanel` deletes the selected song from `MusicLibrary`
2. `MainFrame` first tells `PlaylistManager` to remove it from playlists
3. `MainFrame` also tells `MusicPlayer` to remove it from the queue
4. `FileStorage` saves the updated state

## OOP concepts used

### Encapsulation

Classes keep their fields private and expose controlled methods.

Examples:

- `Song`
- `MusicLibrary`
- `MusicPlayer`

### Inheritance

- `ManualPlaylist extends Playlist`
- `SmartPlaylist extends Playlist`

### Polymorphism

Other classes store playlists using the parent type `Playlist`, then Java calls the right subclass behavior at runtime.

Examples:

- `getSongs()`
- `addSong(...)`
- `removeSong(...)`
- `isEditable()`

### Data structures

- `ArrayList<Song>` for songs
- `ArrayList<Playlist>` for playlists
- copied lists returned from getters for safer access

## Save/load format

### Songs

Each line in `data/songs.txt`:

```text
encodedTitle|encodedArtist|encodedFilePath|durationSeconds
```

### Playlists

Manual playlist line:

```text
encodedType|encodedPlaylistName|encodedSongPath1|encodedSongPath2|...
```

Smart playlist line:

```text
encodedType|encodedPlaylistName|encodedRuleField|encodedKeyword
```

Encoding used:

- Base64

Reason:

- safer text storage for file paths and special characters

## How to explain the inheritance part simply

Short explanation:

"`Playlist` is the parent type. `ManualPlaylist` stores songs directly, while `SmartPlaylist` builds its songs from a rule. The GUI and manager can work with the parent type `Playlist`, and each subclass handles `getSongs()` differently. That is the polymorphism part."

## Suggested study checklist

Make sure these can be explained clearly:

- why `Song.equals()` uses file path
- why songs load before playlists
- why `SmartPlaylist` is not editable
- how `getSongs()` behaves differently in manual vs smart playlists
- how pause/resume works in `MusicPlayer`
- how save/load works in `FileStorage`

## Suggested practice demo

Practice this exact order:

1. Open app
2. Import songs
3. Search a song
4. Sort by artist
5. Create a manual playlist and add songs
6. Create a smart playlist using an artist keyword
7. Play a song
8. Pause and resume
9. Close and reopen the app to show persistence

## Limitations

- playback time is approximate
- no progress bar seeking
- no volume control
- smart playlists only support simple text rules

This version is meant for studying. For final delivery, use `README_SUBMISSION.md`.
