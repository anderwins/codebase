# Software Architecture, GRASP, and Design Patterns

This document outlines the software architecture of the project, how GRASP principles are applied, and the design patterns used.

## 1. Overall Architecture

The application is a desktop music player built using **JavaFX** for the graphical user interface and **SQLite** for local database storage. It allows users to manage their music library, create playlists, play audio files, and view metadata.

### 1.1. Key Technologies

*   **Java (OpenJDK):** The core programming language.
*   **JavaFX:** Used for building the rich client interface (GUI). FXML is used for defining the structure of the views declaratively.
*   **SQLite:** A file-based relational database used to store information about tracks, playlists, and application settings.
*   **Maven:** Used for project build and dependency management.
*   **JUnit:** Used for unit testing.
*   **Libraries:**
    *   `jaudiotagger`: For reading and writing metadata from audio files.
    *   `com.fasterxml.jackson.databind`: For JSON processing (if used, e.g., for configurations or external API interactions).
    *   `org.xerial.sqlitejdbc`: JDBC driver for SQLite.

### 1.2. Package Structure

The project follows a layered architecture, primarily organized by technical concern:

*   **`ulb` (root package):**
    *   `GuiMain.java`: The main entry point of the JavaFX application.
    *   `Main.java`: Potentially an alternative entry point or utility.
    *   `Config.java`: Application configuration.
    *   `LoggerConfig.java`: Configuration for logging.

*   **`ulb.controller`:** Contains controller classes that handle user input from the views, interact with the model/services, and determine which view to display.
    *   `MainController.java`: Orchestrates the overall application flow and navigation between different pages/views.
    *   Page-specific controllers (e.g., `HomeController`, `LibraryController`, `PlaylistController`, `AudioPlayerController`): Manage interactions for their respective views.
    *   `EPages.java`: An enumeration likely used to identify and manage different application pages.
    *   `handleError`: Contains custom exception classes related to controller operations.

*   **`ulb.view`:** Contains classes directly related to the user interface, primarily JavaFX view controllers that are companions to FXML files.
    *   These classes (e.g., `HomeViewController`, `LibraryViewController`, `MainViewController`) are responsible for updating the UI elements based on data from controllers/models and passing user actions to the controllers.
    *   `utils`: Utility classes for the view layer, like `AlertManager`.

*   **`ulb.model`:** Contains the application's business logic and data structures (the domain model).
    *   Core entities like `Track.java`, `Playlist.java`, `Queue.java`, `Radio.java`.
    *   Manager classes like `TrackLibrary.java` (manages the collection of tracks), `PlaylistManager.java` (manages playlists).
    *   Service-like classes such as `MetadataManager.java` (handles metadata extraction), `KaraokeSynchronizer.java`.
    *   `handbleError` (typo, likely `handleError`): Custom exception classes for model-layer operations.

*   **`ulb.dao` (Data Access Objects):** Encapsulates all database interaction logic.
    *   `DbInitializer.java`: Manages database connection setup and schema creation/migration.
    *   `DbManager.java` (and its specialized versions like `DbManagerInsert`, `DbManagerSearch`, `DbManagerUpdate`): Provide CRUD (Create, Read, Update, Delete) operations for database entities.
    *   `SQLLoader.java`: Utility for loading SQL scripts.

*   **`ulb.services`:** Provides a central access point for shared application services.
    *   `AppServices.java`: Acts as a service locator, initializing and providing access to DAO instances (`DbManager...`) and other services like `DatabaseSeeder` and `MetadataManager`.

*   **`ulb.i18n`:** Internationalization and localization.
    *   `LanguageManager.java`: Manages language bundles and provides localized strings.

*   **`ulb.utils`:** General utility classes that don't fit into other specific packages (e.g., `ColorExtractor`).

*   **FXML files (in `src/main/resources/fxml`):** Define the structure and layout of the user interface views.
*   **CSS files (in `src/main/resources/css`):** Style the JavaFX UI elements.

### 1.3. Application Startup Flow

1.  Execution begins with `GuiMain.main()`, which calls `launch(args)`.
2.  The JavaFX runtime calls `GuiMain.start(Stage primaryStage)`.
3.  Inside `start()`:
    *   `LanguageManager` is initialized for internationalization.
    *   `AppServices.init()` is called, which:
        *   Initializes `DbInitializer` to set up the database connection.
        *   Creates instances of `DbManagerInsert`, `DbManagerSearch`, `DbManagerUpdate`.
        *   Initializes `MetadataManager` and `DatabaseSeeder`.
    *   `AppServices.getDbSeeder().seedDatabase()` populates initial data if necessary.
    *   `initializeLibrary()`: Creates a `TrackLibrary` instance, loads all tracks from the database via `AppServices.getDbSearch().getAllTracks()`, and sets up change tracking.
    *   `initializePlaylists()`: Loads playlists and their associated tracks from the database using `AppServices.getDbSearch().getAllPlaylistsWithTracks()` and populates `PlaylistManager`.
    *   `loadMainView()`: Loads `/fxml/MainView.fxml` using `FXMLLoader`. The `MainViewController` is obtained from the loader.
    *   `initializePlayerView()`: Loads `/fxml/PlayerView.fxml`. The `AudioPlayerController` is initialized with its corresponding view controller and the `TrackLibrary`.
    *   A `Queue` model is created.
    *   `MainController` is instantiated, receiving the main view parent, `MainViewController`, `TrackLibrary`, and `Queue`.
    *   Inside `MainController`'s constructor:
        *   It initializes various page-specific controllers (e.g., `HomeController`, `LibraryController`, `PlaylistController`) by loading their FXML views (`mainViewController.addPage(...)`) and passing necessary dependencies.
    *   `mainController.goToLibrary()` sets the initial view.
    *   `mainController.startApplication(primaryStage)` sets up the scene, applies stylesheets, configures the primary stage, and shows it.

### 1.4. View Management

*   **FXML:** Views are defined declaratively using FXML files located in `src/main/resources/fxml/`. Each FXML file typically has an associated view controller class in the `ulb.view` package (e.g., `LibraryView.fxml` is controlled by `ulb.view.LibraryViewController`).
*   **`MainViewController`:** This class acts as a container or manager for the different content panes that make up the application's UI. It has methods like `addPage()` to load FXML and associate it with an identifier (from `EPages`), and `showPage()` to switch the visible content in the main area.
*   **`MainController`:** This controller orchestrates which page is currently active. It holds instances of the page-specific controllers (like `LibraryController`, `HomeController`) and calls `mainViewController.showPage()` to navigate. For example, `mainController.goToLibrary()` will instruct `MainViewController` to display the library page.
*   **Page-Specific Controllers (e.g., `LibraryController`, `HomeController`):** These are instantiated by `MainController`. They receive their corresponding view controller (from the `ulb.view` package, e.g., `LibraryViewController`) during their setup. They handle the logic for their specific page and interact with services and the model.
*   **View Controllers (e.g., `LibraryViewController`):** These are directly linked to FXML elements (via `@FXML` annotations). They update the UI based on data provided by the page-specific controllers and delegate user actions (button clicks, etc.) to these controllers.

This structure separates the UI definition (FXML), UI display logic (View Controllers), and UI event handling/application flow logic (Page Controllers, `MainController`).

## 2. GRASP Principles

GRASP (General Responsibility Assignment Software Patterns) principles help in designing object-oriented systems by assigning responsibilities to classes and objects. Here's how some of these principles are applied in this project:

### 2.1. Information Expert

*   **Definition:** Assign responsibility to the class that has the information necessary to fulfill it.
*   **Examples:**
    *   **`Track` Class (`ulb.model.Track`):** This class is the Information Expert for details about a specific music track (e.g., title, artist, album, duration, file path, metadata). Any external class needing this information would query a `Track` object.
    *   **`Playlist` Class (`ulb.model.Playlist`):** Holds a list of tracks and its own properties (e.g., name). It's the expert for managing the tracks within that specific playlist (adding, removing, reordering).
    *   **`TrackLibrary` Class (`ulb.model.TrackLibrary`):** Manages the entire collection of unique `Track` objects available in the application. It's the expert for querying all available tracks, searching for tracks, etc.
    *   **`DbManagerSearch` (`ulb.dao.DbManagerSearch`):** This class has the necessary information (database connection, SQL queries) to retrieve data from the database. It's the expert for fetching tracks, playlists, etc., from storage.
    *   **`LanguageManager` (`ulb.i18n.LanguageManager`):** Expert for providing localized strings based on the current language settings. It holds the resource bundles.

### 2.2. Creator

*   **Definition:** Assign the responsibility for creating an object to a class that aggregates, contains, or closely uses the object being created.
*   **Examples:**
    *   **`GuiMain` creating `MainController`:** `GuiMain` is the entry point and sets up the main application window. It creates the `MainController` as it's the primary orchestrator of the UI that `GuiMain` bootstraps.
    *   **`MainController` creating Page Controllers (e.g., `LibraryController`, `HomeController`):** `MainController` manages the different pages/views of the application. It creates the specific controllers for these pages because it directly uses them to manage application flow and display content.
    *   **`FXMLLoader` creating View Controllers (e.g., `LibraryViewController`):** When an FXML file is loaded, the `FXMLLoader` is responsible for instantiating the associated controller specified in the FXML (e.g., `fx:controller="ulb.view.LibraryViewController"`).
    *   **`AppServices` creating DAO instances:** `AppServices` initializes and provides instances of `DbManagerInsert`, `DbManagerSearch`, etc. It's a Creator because these DAO objects are fundamental services it manages and provides to the rest of the application.
    *   **`PlaylistManager` potentially creating `Playlist` objects:** If new playlists are created dynamically through `PlaylistManager`, it would be the Creator of `Playlist` objects.
    *   **`DbManagerInsert` creating records in DB / `DbManagerSearch` creating model objects from DB data:** While not direct object creation in Java memory in all cases for `DbManagerInsert`, `DbManagerSearch` definitely creates `Track`, `Playlist` etc. objects when it fetches data from the database and hydrates these model objects.

### 2.3. Controller

*   **Definition:** Assign the responsibility of handling system events or user interface events to a class that represents the overall system, a use case session, or acts as an intermediary between the UI and the model.
*   **Examples:**
    *   **`MainController` (`ulb.controller.MainController`):** Acts as a high-level controller. It handles navigation requests (e.g., "go to library," "go to home") and directs the `MainViewController` to display the appropriate page. It also initializes and coordinates the page-specific controllers.
    *   **Page-Specific Controllers (e.g., `LibraryController`, `PlaylistController`, `AudioPlayerController` in `ulb.controller`):** These classes handle user interactions within their specific views (e.g., a button click in the library view, a request to play a track). They interpret these UI events, interact with the model (`TrackLibrary`, `PlaylistManager`, `AudioPlayer`), and update the view (often through their associated View Controller like `LibraryViewController`).
    *   **`MainViewController` and other `ulb.view.*ViewController` classes:** While primarily part of the view layer, these controllers directly handle raw UI events from JavaFX components (defined in FXML via `onAction="#methodName"`). They then typically delegate the processing of these events to the more application-logic-oriented controllers in the `ulb.controller` package. This creates a two-tiered controller structure: view controllers for UI event handling and application controllers for use case logic.

### 2.4. Low Coupling

*   **Definition:** Assign responsibilities so that coupling (dependencies between classes) remains low. Low coupling minimizes the impact of changes in one class on other classes.
*   **Examples:**
    *   **`AppServices` as a Service Locator:** Controllers and other classes don't need to know how to instantiate database managers (`DbManagerSearch`, `DbManagerInsert`). They simply ask `AppServices` for an instance. This decouples them from the specifics of DAO creation and lifecycle.
    *   **Separation of Concerns (MVC/MVP-like structure):**
        *   **Model (`ulb.model`):** Unaware of the UI (View/Controller). Changes in the UI don't affect the model.
        *   **View (`ulb.view` and FXML):** Primarily responsible for display. Ideally, depends on controllers/view models but not directly on the core business logic of the model, other than displaying its data.
        *   **Controllers (`ulb.controller`):** Mediate between View and Model, reducing direct coupling between them.
    *   **DAO Pattern (`ulb.dao`):** The business logic (in controllers/services) is decoupled from the specifics of database access. If the database schema or even the database system changed, the changes would primarily be localized to the DAO classes.
    *   **Observer Pattern:** The subject (e.g., `TrackLibrary`) doesn't know the concrete classes of its observers, only that they implement an observer interface. This allows adding new observers without modifying the subject.

### 2.5. High Cohesion

*   **Definition:** Assign responsibilities so that the elements within a class are closely related and focused on a single, well-defined purpose.
*   **Examples:**
    *   **`DbManagerSearch` (`ulb.dao.DbManagerSearch`):** Highly cohesive as all its methods are focused solely on retrieving data from the database. It doesn't mix data retrieval with data modification or UI logic. Similarly for `DbManagerInsert` and `DbManagerUpdate`.
    *   **`Track` Class (`ulb.model.Track`):** Its responsibilities are all related to representing and providing information about a single music track.
    *   **`AudioPlayerController` (`ulb.controller.AudioPlayerController`):** Focused on controlling audio playback (play, pause, stop, volume, seeking, observing playback state).
    *   **`LanguageManager` (`ulb.i18n.LanguageManager`):** Solely responsible for managing and providing localized text.
    *   **View Controllers (e.g., `LibraryViewController`):** Cohesive around managing the UI elements and events for a specific view (the library view).

### 2.6. Polymorphism

*   **Definition:** When related alternatives or behaviors vary by type (class), assign responsibility for the behavior—using polymorphic operations—to the types for which the behavior varies.
*   **Examples:**
    *   **JavaFX Event Handling:** While not explicitly creating diverse subclasses for a single action, JavaFX's event handling system itself is polymorphic. `setOnAction(EventHandler<ActionEvent> handler)` takes an `EventHandler` interface. Different anonymous inner classes or lambda expressions implementing this interface provide varied behavior for different UI controls.
    *   **Observer Pattern Implementations:** If `PlayerObserver` or `PlaylistObserver` are interfaces, different classes can implement these interfaces to react to player or playlist events in various ways (e.g., updating UI, logging, etc.). The subject (e.g., `AudioPlayerController`, `Playlist`) would interact with them through the common observer interface.
    *   **(Hypothetical) Different Track Types:** If the application were to support different types of media (e.g., local tracks, streaming tracks, podcast episodes) each represented by a class implementing a common `PlayableMedia` interface, then operations like `play()` could be handled polymorphically. The current structure with `Track.java` seems specific, but this is a common area for polymorphism.

### 2.7. Pure Fabrication

*   **Definition:** Assign a highly cohesive set of responsibilities to an artificial or convenience class that does not represent a problem domain concept—something made up, to support high cohesion, low coupling, and reuse.
*   **Examples:**
    *   **`AppServices` (`ulb.services.AppServices`):** This class doesn't represent a real-world entity in the music player domain (like a Track or Playlist). It's a Pure Fabrication created to manage the lifecycle and provide access to database services and other shared application services (like `MetadataManager`). This improves organization and reduces coupling, as various parts of the application don't need to manage these services themselves.
    *   **`DbInitializer`, `DbManagerInsert`, `DbManagerSearch`, `DbManagerUpdate` (`ulb.dao`):** These DAO classes are fabrications to encapsulate database interaction logic. They don't represent domain entities but provide a clean separation of data access concerns.
    *   **`LanguageManager` (`ulb.i18n.LanguageManager`):** Fabricated to handle the complexities of internationalization, keeping this responsibility separate from domain or UI logic.
    *   **`AlertManager` (`ulb.view.utils.AlertManager`):** A utility class fabricated to standardize the display of alert dialogs.

### 2.8. Indirection

*   **Definition:** Assign responsibility to an intermediate object to mediate between other components or services, to reduce direct coupling between them.
*   **Examples:**
    *   **`MainController` (`ulb.controller.MainController`):** Acts as an intermediary for navigation between different pages/views. Page controllers don't directly call each other to switch views; they typically go through `MainController` (e.g., `mainController.goToLibrary()`). This decouples the page controllers from each other regarding navigation.
    *   **`AppServices` (`ulb.services.AppServices`):** Provides a layer of indirection to the actual database management and metadata services. Controllers and other components don't access `DbManager` instances directly by creating them; they go through `AppServices`.
    *   **Controllers acting as intermediaries between View and Model:** The entire MVC/MVP-like structure relies on controllers (e.g., `LibraryController`) as indirection layers between the UI (e.g., `LibraryViewController` and FXML) and the data/business logic (e.g., `TrackLibrary`).
    *   **`SQLLoader` (`ulb.dao.SQLLoader`):** Provides indirection for accessing SQL statements, potentially loading them from files, decoupling the DAO classes from hardcoded SQL strings.

### 2.9. Protected Variations

*   **Definition:** Protect elements from the variations in other elements (e.g., new requirements, changes in technology) by wrapping the predicted points of instability with a stable interface.
*   **Examples:**
    *   **DAO Layer (`ulb.dao`):** This entire layer protects the rest of the application (especially controllers and services) from changes in the database implementation or schema. If the application were to switch from SQLite to another database, or if table structures changed significantly, the primary impact would be within the DAO classes. The interfaces/methods provided by `DbManagerSearch`, `DbManagerInsert`, etc., would ideally remain stable.
    *   **`LanguageManager` (`ulb.i18n.LanguageManager`):** Provides a stable interface for obtaining localized strings. The underlying mechanism for storing or fetching these strings could change (e.g., from properties files to a database) without affecting the clients of `LanguageManager`.
    *   **`AppServices` (`ulb.services.AppServices`):** The way services are instantiated or configured within `AppServices` can change, but as long as the getter methods (`getDbSearch()`, etc.) remain consistent, the clients are protected from these internal variations.
    *   **FXML and View Controllers:** By separating UI layout (FXML) from UI logic (View Controllers like `LibraryViewController`) and application logic (Controllers like `LibraryController`), changes to the FXML layout (e.g., rearranging buttons) should ideally not break the application controllers, as long as the `fx:id`s and method bindings remain consistent with the View Controller.

## 3. Design Patterns

Several common software design patterns are utilized in this project to structure the code, manage responsibilities, and promote maintainability and flexibility.

### 3.1. Model-View-Controller (MVC) / Model-View-Presenter (MVP) Variant

The application exhibits a structure that aligns closely with MVC or one of its variants like MVP, aiming to separate concerns:

*   **Model (`ulb.model` package):**
    *   Represents the application's data and business logic.
    *   Examples: `Track`, `Playlist`, `TrackLibrary`, `PlaylistManager`, `Queue`.
    *   Includes logic for managing tracks, playlists, metadata, and the playback queue.
    *   It is independent of the UI and does not directly reference View or Controller components (except possibly through observer notifications).

*   **View:**
    *   Responsible for rendering the user interface and presenting data to the user.
    *   Primarily composed of:
        *   **FXML files (`src/main/resources/fxml`):** Declaratively define the layout and components of the UI.
        *   **View Controllers (`ulb.view` package, e.g., `LibraryViewController`, `MainViewController`):** These classes are directly associated with FXML files. They handle low-level UI events (e.g., button clicks defined via `onAction` in FXML), manipulate UI elements (e.g., populating a `TableView`), and delegate more complex actions to the Application Controllers. They often hold references to UI elements injected via `@FXML`.

*   **Controller/Presenter (`ulb.controller` package):**
    *   Acts as an intermediary between the Model and the View.
    *   Examples: `MainController`, `LibraryController`, `PlaylistController`, `AudioPlayerController`.
    *   **Responsibilities:**
        *   Receives user actions from the View Controllers (e.g., `LibraryViewController` calls a method on `LibraryController`).
        *   Interacts with the Model to fetch or update data (e.g., `LibraryController` uses `TrackLibrary` to get tracks).
        *   Prepares data for display and instructs the View Controllers to update the UI.
        *   Handles application flow and navigation (especially `MainController`).
    *   This setup is a common pattern in JavaFX applications. The `ulb.controller` classes act more like Presenters in MVP or Application Controllers, while the `ulb.view` classes are the View, with some controller-like responsibilities for direct FXML interaction.

### 3.2. Service Locator

*   **Pattern:** Provides a global point of access to services (like database access or utility services) without coupling the clients to the concrete classes of these services or their complex initialization logic.
*   **Implementation:** The **`AppServices` class (`ulb.services.AppServices`)** acts as a Service Locator.
    *   It initializes and holds instances of various services, primarily DAO classes (`DbInitializer`, `DbManagerInsert`, `DbManagerSearch`, `DbManagerUpdate`), `DatabaseSeeder`, and `MetadataManager`.
    *   These services are accessed statically via getter methods (e.g., `AppServices.getDbSearch()`).
    *   **Benefits:**
        *   Decouples consumers from the concrete service implementations and their instantiation.
        *   Centralizes service management and configuration.
        *   Simplifies access to shared services across the application.

### 3.3. Singleton

*   **Pattern:** Ensures a class has only one instance and provides a global point of access to it.
*   **Implementations:**
    *   **`LanguageManager` (`ulb.i18n.LanguageManager`):** Uses `LanguageManager.getInstance()` to provide a single instance for managing language resources and internationalization throughout the application. This ensures consistent language settings.
    *   **`PlaylistManager` (`ulb.model.PlaylistManager`):** Accessed via `PlaylistManager.getInstance()`. This ensures that all parts of the application interact with the same set of playlists.
    *   **`GuiMain.audioPlayerController` (static field):** While not a formal singleton implemented with a private constructor and static getter, making `audioPlayerController` a public static field in `GuiMain` provides a global, single point of access to the `AudioPlayerController` instance after it's initialized. This is a simpler, though less encapsulated, way to achieve a similar global access goal.
    *   **`AppServices` (static access):** While `AppServices` itself is not instantiated (all methods are static or access static fields), it effectively provides singleton-like access to the services it manages, as these services are initialized once and stored in static fields.

### 3.4. Observer

*   **Pattern:** Defines a one-to-many dependency between objects so that when one object (the subject) changes state, all its dependents (observers) are notified and updated automatically.
*   **Implementations:**
    *   **`TrackLibrary` and `ChangeTracker`:** In `GuiMain.initializeLibrary()`, a `ChangeTracker` is added as an observer to the `TrackLibrary` instance (`library.addObserver(changeTracker)`). When tracks are added, removed, or modified in the `TrackLibrary`, the `ChangeTracker` would be notified to potentially persist these changes (e.g., via `AppServices.getDbUpdate()`).
    *   **`PlaylistManager` and `PlaylistObserver` / `PlaylistsObserver`:** The existence of `PlaylistObserver.java` and `PlaylistsObserver.java` suggests that components can observe changes in individual playlists or the collection of playlists managed by `PlaylistManager`. For instance, UI elements displaying playlist content would observe the relevant `Playlist` object to update automatically when tracks are added or removed.
    *   **`AudioPlayerController` and `PlayerObserver`:** The `PlayerObserver.java` interface likely allows different parts of the application (e.g., UI components showing current track info, playback progress) to observe the state of the audio player (e.g., track changes, play/pause events, progress updates) and react accordingly. `AudioPlayerController` would be the subject.

### 3.5. Data Access Object (DAO)

*   **Pattern:** Abstracts and encapsulates all access to the data source (here, an SQLite database). The DAO manages the connection with the data source to obtain and store data.
*   **Implementation:** The **`ulb.dao` package** implements this pattern.
    *   **`DbInitializer`:** Manages the database connection setup and potentially schema creation/initialization.
    *   **`DbManagerSearch`:** Provides methods to query the database and retrieve data (e.g., `getAllTracks()`, `getAllPlaylistsWithTracks()`). It maps database records to Java model objects (`Track`, `Playlist`).
    *   **`DbManagerInsert`:** Provides methods to insert new data into the database.
    *   **`DbManagerUpdate`:** Provides methods to update existing data in the database.
    *   **`SQLLoader`:** A utility to load SQL statements, likely from `.sql` files, decoupling SQL from Java code.
    *   **Benefits:**
        *   Decouples business logic (in controllers, services, model managers) from data access logic.
        *   Centralizes data access, making it easier to manage and modify (e.g., if database schema changes or if migrating to a different database type, changes are mostly confined to the DAO layer).
        *   Improves testability, as the DAO layer can be mocked for testing business logic.

### 3.6. Dependency Injection (Manual)

*   **Pattern:** A technique whereby one object (or static method) supplies the dependencies of another object. A dependency is an object that can be used (a service).
*   **Implementation:** While the project doesn't use a dedicated DI framework (like Spring or Guice), it employs manual dependency injection, primarily **constructor injection**:
    *   **`MainController` constructor:**
        ```java
        public MainController(Parent mainParent,
                              MainViewController mainViewController,
                              TrackLibrary trackLibrary,
                              Queue modelQueue)
        ```
        Here, `MainController` receives its dependencies (`mainParent`, `mainViewController`, `trackLibrary`, `modelQueue`) when it's created in `GuiMain`.
    *   **Page-specific controllers created by `MainController`:**
        ```java
        playlist = new PlaylistController(
            (PlaylistViewController) mainViewController.addPage("/fxml/PlaylistView.fxml", EPages.PLAYLIST),
            this, // MainController instance
            AppServices.getDbInsert(),
            audioPlayerController,
            PlaylistManager.getInstance()
        );
        ```
        `PlaylistController` receives its view controller, a reference to `MainController`, a DAO instance from `AppServices`, the `audioPlayerController`, and the `PlaylistManager`.
    *   **`AudioPlayerController` constructor:**
        ```java
        public AudioPlayerController(PlayerViewController view, TrackLibrary trackLibrary)
        ```
        Receives its view controller and the track library.
    *   **Benefits:**
        *   Makes class dependencies explicit.
        *   Improves testability by allowing mock dependencies to be injected during tests.
        *   Leads to more modular and decoupled components compared to direct instantiation of dependencies within a class.

### 3.7. Facade (Potentially)

*   **Pattern:** Provides a simplified interface to a larger body of code, such as a complex subsystem.
*   **Potential Implementations:**
    *   **`PlaylistManager`:** Could be seen as a Facade for managing the complexities of playlist creation, modification, and persistence (if it internally coordinates with DAOs or other services).
    *   **`MetadataManager`:** Simplifies the process of extracting metadata from files, potentially hiding the details of using libraries like `jaudiotagger`.
    *   **`AppServices`:** While primarily a Service Locator, it also acts as a Facade to the data access subsystem, providing a simplified way to get different DAO managers.

This list covers the most prominent patterns. Other more granular patterns might be found on closer inspection of specific components.
