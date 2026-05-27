# Music Library Management System - Submission and Presentation Guide

This project is a Java Swing desktop application for managing and playing an MP3 music library.

## Main features

- import one or multiple MP3 files
- read song metadata automatically
- search songs by title or artist
- sort songs by title or artist
- edit and delete songs
- create manual playlists
- create smart playlists based on title or artist keywords
- delete playlists
- play, pause, resume, and skip songs
- use shuffle and repeat modes
- save and load songs and playlists automatically

## Project structure

```text
app       -> program startup
gui       -> user interface
manager   -> library and playlist management
model     -> core domain classes
player    -> MP3 playback
service   -> metadata and file storage
```

## OOP concepts used

- Encapsulation:
  private fields with public methods in classes such as `Song`, `MusicLibrary`, and `MusicPlayer`
- Inheritance:
  `ManualPlaylist` and `SmartPlaylist` extend the abstract parent class `Playlist`
- Polymorphism:
  playlist objects are handled through the parent type `Playlist`, while each subclass provides its own behavior for methods like `getSongs()` and `isEditable()`
- Data structures:
  `ArrayList` is used for storing songs and playlists

## Playlist design

Two playlist types are supported:

- `ManualPlaylist`
  songs are added and removed directly by the user
- `SmartPlaylist`
  songs are generated automatically from a rule such as artist keyword or title keyword

This design was added to make the playlist system more extensible and to demonstrate inheritance and polymorphism in a practical way.

## Storage

The app stores data in:

```text
data/songs.txt
data/playlists.txt
```

- songs are saved with title, artist, file path, and duration
- manual playlists save song paths
- smart playlists save rule information

## Libraries used

- Jaudiotagger for MP3 metadata
- JLayer for MP3 playback

## Demo flow

Presentation order (probly):

1. Launch the app
2. Import a few MP3 files
3. Search and sort the library
4. Create a manual playlist and add songs
5. Create a smart playlist using an artist or title keyword
6. Play a song and show pause/resume
7. Show shuffle or repeat
8. Close and reopen the app to demonstrate save/load

## Key points:

- The app separates GUI, business logic, models, playback, and storage into different packages.
- `Playlist` is an abstract parent class.
- `ManualPlaylist` and `SmartPlaylist` show inheritance.
- The app uses polymorphism when working with playlist objects through the parent type.
- Smart playlists were added as a meaningful feature, not just as a technical requirement.

## Current limitations

- playback time is approximate (not accurate to miliseconds, but very usable)
- no progress bar seeking (display in minutes and seconds)
- no volume control
- smart playlists currently support simple keyword rules only
