# Code Review Questions

## Questions regarding `AudioPlayerController.java`

### Error Handling
1.  In `playFile` and `playStream`, errors are logged, and alerts are shown, but then the player is often reset, or a new `IllegalArgumentException` is thrown. Could these errors be handled more gracefully? Could specific exceptions be caught and handled differently?
2.  Is it sufficient for `media.setOnError(...)` and `mediaPlayer.setOnError(...)` lambdas to only log errors, or should they trigger more visible user notifications or state changes?

### Resource Management
3.  How is it ensured that the `shutdown()` method for the `scheduler` is always called when `AudioPlayerController` is no longer needed? What are the consequences if it's missed?
4.  Are there edge cases where `mediaPlayer` in `reset()` might not be fully cleaned up? Why is `mediaPlayer.setOnEndOfMedia(null)` called in `reset()`?

### State Management
5.  Are the `isPlaying` and `isPaused` boolean flags robust enough for managing player state, especially with async operations? Could an enum for states (e.g., `IDLE`, `PLAYING`, `PAUSED`) be more appropriate?

### Code Duplication & Refactoring
6.  How could the common logic in `playStream()` and `playFile()` for `MediaPlayer` initialization and error handling be refactored to reduce duplication?
7.  Regarding `onPrevious()` and `onNext()` methods, why are these separate methods executing runnables, rather than a more generic event subscription or direct management by collaborators?

### Lyrics Fetching (`showLyrics` method)
8.  Could the complex multi-step logic in `showLyrics` be broken down into smaller helper methods for better readability/testability?
9.  Could the synchronous online call `lrcLibService.searchAndSaveLyrics(track)` block the UI? If so, how should it be handled?
10. What are potential issues with string concatenation for building lyrics file paths, especially concerning platform differences or special characters?
11. Could multiple alerts from `showLyrics` during various failure points lead to alert fatigue?

### Concurrency
12. When the `progressUpdater` (from `scheduler`) calls `viewController.updateProgress`, is this UI update always correctly routed to the JavaFX Application Thread (e.g., via `Platform.runLater`)?
13. Is `Platform.runLater` consistently used for all UI updates originating from background tasks?

### Magic Numbers/Strings & Configuration
14. Would it be better to define magic numbers (e.g., `progressUpdateRate = 100`, `fade = 15`) as named constants? Why?
15. How does using string literals for logging and `AlertManager` messages affect maintainability and internationalization?

### Testing
16. How would you unit test methods like `playFile`, especially its error handling and state changes, using the `MediaPlayerFactory`?
17. What are the challenges in testing the fade-in/fade-out logic?

### Miscellaneous
18. For events like "onEnd," would a list of observers be more flexible than a single `Runnable` if multiple components need notification?
19. Could the naming of `stopFade` boolean and `setFade()` method be clarified for better intuitiveness (e.g., `isFadeEnabled`, `toggleFadeEffect()`)?
20. Is the declared `balanceThread` field used, or is it old code?

## Questions regarding `DbManager.java`

### Error Handling
1.  Is returning -1 from `getId` sufficient to distinguish "not found" from a database error? Should a custom exception be thrown for `SQLException`?
2.  How would a client of `getId` differentiate -1 meaning "not found" versus "an error occurred"?

### Resource Management (Connection Handling)
3.  Who is responsible for opening the connection passed to `DbManager`? Who is responsible for calling `closeConnection()`?
4.  What happens if `closeConnection()` is called multiple times? Are try-with-resources patterns used by client code to ensure connections are always closed?
5.  `PreparedStatement` and `ResultSet` in `getId` are well-managed with try-with-resources. Is this consistent across all subclasses for all DB resources?

### SQL Injection Prevention
6.  `getId` uses `PreparedStatement`. What ensures the SQL strings loaded from `Config.COMMON_QUERIES_SQL_FILE` are secure and not built unsafely before loading?
7.  Are all subclasses of `DbManager` consistently using `PreparedStatement` for all data-handling operations?

### Abstraction Design
8.  What is the intended purpose of `DbManager` being abstract if its main lookup methods are concrete? What abstract methods are subclasses expected to implement?
9.  What happens if `Config.COMMON_QUERIES_SQL_FILE` (used by `SQLLoader`) is missing or contains malformed queries?

### Temporary Dependencies / Design Concerns
10. Why are `trackLibrary` and `changes` fields considered temporary or misplaced in `DbManager`? Where might be a more appropriate location, and what design principles are at play?

### Connection Management
11. Since `connection` is `protected`, can subclasses directly manipulate it (commit, rollback)? What's the transaction management strategy?

## General Project Questions

### Logging Strategy
1.  What is the configured logging level for `java.util.logging`? Is it environment-configurable? Where do logs go? Is there log rotation?
2.  Are there clear developer guidelines for what to log and at which level?
3.  In `AudioPlayerController`, `Logger.getLogger(DbInitializer.class.getName())` is used. Is this intentional or a mistake?

### Configuration Management (`Config.java`)
4.  How are configuration values in `Config.java` stored/loaded (hardcoded, file, env vars)? How easy is it to change them without recompiling?
5.  What happens if `Config.setUpFolders()` fails?

### Modularity and Separation of Concerns
6.  How strictly is separation of concerns enforced between layers (controller, DAO, service, view)? Do controllers directly access DAOs?
7.  Are there any classes that seem to have too many responsibilities (potential "god classes")?
8.  Why are both Jackson and Gson JSON libraries included as dependencies? Could the project standardize?

### Internationalization (i18n)
9.  How are user-facing strings managed via `LanguageManager` and `src/main/resources/i18n/`? How is the current locale set?
10. Are all user-visible strings (e.g., in `AlertManager`) correctly internationalized using this system?

### Overall Testing Strategy
11. What's the reason for having both JUnit 4 and JUnit 5? Is there a migration or specific uses for each?
12. `mockito-inline` is used for static mocking. How does the prevalence of static methods affect testability?
13. What is the extent of UI test coverage with TestFX? Are critical workflows covered?
14. Is there a strategy for unit vs. integration tests (e.g., are DAOs tested against a real test database)? Is test coverage measured?

### Build and Dependency Management
15. Are any specific Java 21 features being utilized that are worth discussing?
16. Why is `maven-shade-plugin` used? What is the effect of its configuration (`createDependencyReducedPom=false`, `shadedArtifactAttached=false`)?

### Database
17. Why are dependencies for both MySQL and SQLite included? Are they used for different purposes or to offer choice?
18. How is the database schema managed (migrations, initial setup)? What roles do `DbInitializer` and `DatabaseSeeder` play?
