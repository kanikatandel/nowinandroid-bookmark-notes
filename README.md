# Now in Android

A fully functional Android app built with Kotlin and Jetpack Compose. Users can browse Android development news, follow topics, receive notifications for new content, and bookmark articles.

This fork of [Now in Android](https://github.com/android/nowinandroid) adds **bookmark notes** and **multi-select bookmark management**. See [Bookmark Notes & Multi-Select](#bookmark-notes--multi-select).

## Architecture

The app follows [official architecture guidance](https://developer.android.com/topic/architecture) and is described in detail in the [architecture learning journey](docs/ArchitectureLearningJourney.md). Modularization strategy is covered in the [modularization learning journey](docs/ModularizationLearningJourney.md).

## Modules

The main Android app lives in `app/`. Feature modules live in `feature/` and core/shared modules in `core/`.

## Build & Run

The app has two product flavors (`demo`, `prod`) and two build types (`debug`, `release`).

The `demo` flavor uses static local data — use this for development. The `prod` flavor makes real network calls to a backend server that is not publicly available.

```bash
# Build
./gradlew assembleDemoDebug

# Fix formatting
./gradlew spotlessApply
```

## Testing

The project uses dependency injection with Hilt. Most data layer components are defined as interfaces with concrete implementations swapped for test doubles in tests.

**No mocking libraries are used.** Test doubles implement the same interfaces as production code and provide test-only hooks for state manipulation. `ViewModel` tests use these test repositories via constructor injection.

```bash
# Run local tests
./gradlew testDemoDebugUnitTest

# Run a single test class
./gradlew testDemoDebugUnitTest --tests "com.example.MyTestClass"

# Run instrumented tests
./gradlew connectedDemoDebugAndroidTest
```

> **Note:** Do not run `./gradlew test` or `./gradlew connectedAndroidTest` — those execute tests against all build variants, which will fail. Only the `demoDebug` variant has tests.

## Bookmark Notes & Multi-Select

### Bookmark notes

- When you bookmark an article on For You, Topic or Search, a dialog lets you add an optional personal note (plain text, max 280 characters). Tap **Skip** to bookmark without a note.
- On the Saved (Bookmarks) screen, a note appears below its article's title. Tap the note to edit or delete it.
- Notes are stored in Proto DataStore, so they persist across app restarts.
- A note only exists while its article is bookmarked: removing the bookmark also deletes the note. Notes are trimmed, and saving a blank note deletes it.

### Multi-select on the Saved screen

- Tap **Select** in the toolbar, or long-press an article, to enter selection mode.
- In selection mode, each article shows a checkbox and tapping an article toggles it. The toolbar shows how many are selected, plus **Select all** / **Deselect all**, **Remove** and **Cancel selection**.
- **Remove** deletes all selected bookmarks and their notes at once, then exits selection mode. A single **UNDO** snackbar restores every removed bookmark together with its note.
- Tap **Cancel selection** or press Back to leave selection mode without removing anything.
- Note editing is turned off in selection mode. Removing a single bookmark outside selection mode works as before.

### Scope

- Notes are plain text only (no formatting or images) and can only be attached to bookmarked articles.
- Notes are shown only on the Saved screen.
- Multi-select is available only on the Saved screen, and remove is the only bulk action.

### Where the code lives

| Layer | Location |
| --- | --- |
| Model | `core/model/.../data/BookmarkNote.kt` (`BOOKMARK_NOTE_MAX_LENGTH`) |
| Storage | `core/datastore-proto/.../user_preferences.proto` (`bookmark_notes`), `core/datastore/.../NiaPreferencesDataSource.kt` |
| Repository | `core/data/.../repository/UserDataRepository.kt` (`setBookmarkNote`, `removeBookmarks`, `restoreBookmarks`) |
| Note dialog | `core/ui/.../BookmarkNoteDialog.kt` |
| Saved screen | `feature/bookmarks/impl/.../BookmarksScreen.kt`, `BookmarksViewModel.kt` |
