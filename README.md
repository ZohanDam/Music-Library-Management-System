# Music Library Management System

This project is a Java Swing desktop application for managing and playing an MP3 music library.

## Project Purpose

The application allows a user to:

- import MP3 files into a music library
- view and manage song information
- organize songs into playlists
- play songs with common playback controls
- save and reload the library automatically

This project was developed to demonstrate object-oriented programming, GUI development, file handling, and basic media playback in Java.

## Main Features

- Import one or multiple MP3 files
- Read song metadata automatically
- Search songs by title or artist
- Sort songs by title or artist
- Edit and delete songs
- Create manual playlists
- Create smart playlists based on title or artist keywords
- Delete playlists
- Play, pause, resume, and skip songs
- Use shuffle and repeat modes
- Save and load songs and playlists automatically

## Technologies

- Java 21
- Maven
- Swing
- Jaudiotagger for MP3 metadata
- JLayer for MP3 playback

## Project Structure

```text
src/main/java/app       program startup
src/main/java/gui       user interface
src/main/java/manager   library and playlist management
src/main/java/model     core domain classes
src/main/java/player    MP3 playback
src/main/java/service   metadata and file storage
data/                   saved songs and playlists
```

## OOP Concepts Used

- Encapsulation through private fields and public methods in classes such as `Song`, `MusicLibrary`, and `MusicPlayer`
- Abstraction through the abstract `Playlist` parent class
- Inheritance through `ManualPlaylist` and `SmartPlaylist`
- Polymorphism by handling playlists through the `Playlist` type while each subclass provides its own behavior
- Data structures through `ArrayList` and Swing list models

## Main Classes

- `app.Main`
  program entry point; creates the main objects, loads saved data, and opens the main window
- `gui.MainFrame`
  main application window that connects the library panel, playlist panel, and playback controls
- `gui.SongPanel`
  handles importing, searching, sorting, editing, and deleting songs
- `gui.PlaylistPanel`
  handles creating, deleting, and managing playlists
- `gui.ControlPanel`
  handles playback controls such as play, pause, next, previous, shuffle, and repeat
- `manager.MusicLibrary`
  stores and manages all songs in the library
- `manager.PlaylistManager`
  stores and manages all playlists
- `model.Song`
  represents a single song
- `model.Playlist`
  abstract parent class for playlists
- `model.ManualPlaylist`
  playlist managed directly by the user
- `model.SmartPlaylist`
  playlist generated automatically from a rule
- `player.MusicPlayer`
  controls MP3 playback
- `service.MetadataReader`
  reads MP3 metadata such as title, artist, and duration
- `service.FileStorage`
  saves and loads songs and playlists from text files

## Playlist Design

Two playlist types are supported:

- `ManualPlaylist`: songs are added and removed directly by the user
- `SmartPlaylist`: songs are generated automatically from a title or artist keyword rule

This design makes the playlist system extensible and demonstrates inheritance and polymorphism in a practical way.

## Data Storage

Application data is stored in:

```text
data/songs.txt
data/playlists.txt
```

- Songs store title, artist, file path, and duration
- Manual playlists store song paths
- Smart playlists store rule type and keyword






## RUNNING REQUIREMENTS

- Java 21
- Maven if command-line build is used
- MP3 files for testing import and playback

Sample MP3 files for testing can be downloaded here: https://drive.google.com/drive/folders/1HnTIcgDIot2i7kYhZgKuTEsuW3gDc37u?usp=sharing

### Option 1: Eclipse (recommended)

1. Open Eclipse with Java 21 configured.
2. Select `File` -> `Import`.
3. Choose `Existing Maven Projects` or `Existing Projects into Workspace`.
4. Select the project folder.
5. Wait for Eclipse to finish building the project and downloading dependencies if needed.
6. Open `src/main/java/app/Main.java`.
7. Run `app.Main` as a Java application.

### Option 2: Maven command line

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass=app.Main
```

If the `exec:java` goal is not available in the local Maven setup, the Eclipse method above should be used.

## How To Use The Application

1. Start the application.
2. Click `Import Song(s)` and choose one or more MP3 files.
3. Use the search field to search by title or artist.
4. Use the sort controls to reorder the library.
5. Select a song and click `Edit Song` to change title or artist.
6. Select one or more songs and click `Delete Song` to remove them.
7. Create a manual playlist and add selected songs to it.
8. Create a smart playlist using a title or artist keyword.
9. Select a song and use the playback controls to play, pause, resume, skip, shuffle, or repeat.
10. Close and reopen the application to confirm that songs and playlists are saved and loaded correctly.

## Sample Test Scenarios

These scenarios can be used to evaluate the main functions of the system.

1. Import songs
   Expected result: selected MP3 files appear in the library with title, artist, and duration.
2. Search songs
   Expected result: searching by part of a title or artist shows matching songs only.
3. Sort songs
   Expected result: the library is reordered correctly by title or artist.
4. Edit song information
   Expected result: edited title or artist is updated in the library and saved.
5. Delete songs
   Expected result: selected songs are removed from the library and also removed from playlists if necessary.
6. Create a manual playlist
   Expected result: a new playlist appears and selected songs can be added or removed.
7. Create a smart playlist
   Expected result: a playlist is generated automatically from the chosen title or artist keyword rule.
8. Delete a playlist
   Expected result: the selected playlist is removed from the system.
9. Playback controls
   Expected result: play, pause, resume, next, previous, shuffle, and repeat work on the active queue.
10. Save and load
    Expected result: after closing and reopening the application, the previously saved songs and playlists are restored.

## Suggested Demo Flow

1. Launch the application
2. Import several MP3 files
3. Search and sort the library
4. Create a manual playlist and add songs
5. Create a smart playlist using an artist or title keyword
6. Play a song and show pause or resume
7. Show shuffle or repeat
8. Close and reopen the application to demonstrate save and load

## Special Design Notes

- `Playlist` is an abstract parent class.
- `ManualPlaylist` and `SmartPlaylist` are subclasses used to demonstrate abstraction, inheritance, and polymorphism.
- Smart playlists are rule-based and currently support title or artist keyword matching.
- Songs are identified by file path, which helps prevent duplicate references to the same file.

## Current Limitations

- Playback time is approximate
- No song progress bar seeking
- No volume control
- Smart playlists currently support simple keyword rules only
