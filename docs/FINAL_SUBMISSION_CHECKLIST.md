# Final Submission Checklist

Use this checklist before compressing and submitting the project package.

## Required Project Contents

Keep these in the final project folder:

- `src`
- `data`
- `pom.xml`
- `README.md`
- `.project`
- `.classpath`
- `.settings`

Optional:

- `docs`

Do not include generated build output:

- `target`

## Technical Checklist

- The project opens correctly in Eclipse
- Java 21 is configured
- `app.Main` runs successfully
- MP3 files can be imported
- Search by title or artist works
- Sort by title or artist works
- Song editing works
- Song deletion works
- Manual playlist creation works
- Adding songs to a manual playlist works
- Removing songs from a manual playlist works
- Smart playlist creation works
- Playlist deletion works
- Playback works
- Pause and resume work
- Next and previous work
- Shuffle and repeat work
- Closing and reopening the app preserves saved data

## Submission Packaging Checklist

- `data/songs.txt` is in the intended state for submission
- `data/playlists.txt` is in the intended state for submission
- `README.md` is present and readable
- `target` is removed
- The folder is compressed as `.zip`

## Recommended Fresh-Grader Test Flow

1. Open the project in Eclipse with Java 21
2. Run `app.Main`
3. Import 3 to 5 MP3 files
4. Search and sort the library
5. Edit one song
6. Create a manual playlist and add songs
7. Create a smart playlist using an artist or title keyword
8. Play a song and test playback controls
9. Delete one song and one playlist
10. Close and reopen the application to confirm save and load

## Recommended Zip Contents

The compressed archive should contain this project folder structure:

```text
MusicLibrary/
  .classpath
  .project
  .settings/
  data/
    songs.txt
    playlists.txt
  docs/                  optional
  pom.xml
  README.md
  src/
```
