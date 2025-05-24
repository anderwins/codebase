/*
 * ==========================================================================================
 *  FICHIER : create_tables.sql
 *  DESCRIPTION :
 *      ➤ Requête de vérification des tables existantes (bloc 1).
 *      ➤ Création de toutes les tables et triggers dans un seul (bloc 2).
 *  STRUCTURE :
 *      ➤ Chaque bloc ayant un tag unique.
 * ==========================================================================================
 */

-- [tablesExist]
SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' LIMIT 1;


-- [createAllTablesAndTriggers]

-- =========================================================
--  TABLES
-- =========================================================

CREATE TABLE IF NOT EXISTS Users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    email TEXT,
    password TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS Artist (
    artist_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    bio TEXT
);

CREATE TABLE IF NOT EXISTS Album (
    album_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    artist_id INTEGER NOT NULL,
    release_year INTEGER,
    FOREIGN KEY (artist_id) REFERENCES Artist(artist_id)
);

CREATE TABLE IF NOT EXISTS Tag (
    tag_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Playlist (
    playlist_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

CREATE TABLE IF NOT EXISTS Track (
    track_id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL UNIQUE,
    artist_id INTEGER NOT NULL,
    album_id INTEGER NOT NULL,
    year INTEGER,
    duration_sec INTEGER,
    file_path TEXT NOT NULL UNIQUE,
    cover_path TEXT,
    lyrics_path TEXT,
    karaoke_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (artist_id) REFERENCES Artist(artist_id),
    FOREIGN KEY (album_id) REFERENCES Album(album_id)
);

CREATE TABLE IF NOT EXISTS Track_Tag (
    track_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,
    PRIMARY KEY (track_id, tag_id),
    FOREIGN KEY (track_id) REFERENCES Track(track_id),
    FOREIGN KEY (tag_id) REFERENCES Tag(tag_id)
);

CREATE TABLE IF NOT EXISTS PlaylistTrack (
    playlist_id INTEGER NOT NULL,
    track_id INTEGER NOT NULL,
    PRIMARY KEY (playlist_id, track_id),
    FOREIGN KEY (playlist_id) REFERENCES Playlist(playlist_id),
    FOREIGN KEY (track_id) REFERENCES Track(track_id)
);

CREATE TABLE IF NOT EXISTS UserFavorites (
    user_id INTEGER NOT NULL,
    track_id INTEGER NOT NULL,
    PRIMARY KEY (user_id, track_id),
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (track_id) REFERENCES Track(track_id)
);

-- =========================================================
--  TRIGGERS
-- =========================================================

CREATE TRIGGER IF NOT EXISTS update_track_timestamp
AFTER UPDATE ON Track
FOR EACH ROW
BEGIN
    UPDATE Track
    SET updated_at = CURRENT_TIMESTAMP
    WHERE track_id = OLD.track_id;
END;

