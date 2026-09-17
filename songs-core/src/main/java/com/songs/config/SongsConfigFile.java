package com.songs.config;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

public final class SongsConfigFile {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();
    private final Path path;

    public SongsConfigFile() {
        this(resolveDefaultPath());
    }

    public SongsConfigFile(Path path) {
        this.path = path;
    }

    public SongsConfig load() throws IOException {
        if (!Files.exists(path)) {
            return new SongsConfig();
        }
        return MAPPER.readValue(path.toFile(), SongsConfig.class);
    }

    public void save(SongsConfig config) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
            restrictDirectory(parent);
        }
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        MAPPER.writeValue(temporary.toFile(), config);
        restrictFile(temporary);
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        restrictFile(path);
    }

    private static Path resolveDefaultPath() {
        String override = System.getenv("SONGS_CONFIG");
        return override == null || override.isBlank()
            ? Path.of(System.getProperty("user.home"), ".songs", "config.json")
            : Path.of(override);
    }

    private static void restrictDirectory(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            ));
        } catch (UnsupportedOperationException ignored) {
        }
    }

    private static void restrictFile(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException ignored) {
        }
    }
}