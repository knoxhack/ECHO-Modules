package com.knoxhack.echo.packcore;

import com.knoxhack.echo.platformcore.EchoPackId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EchoPackProfileRepository {
    private final Path workspaceRoot;

    public EchoPackProfileRepository(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot == null ? Path.of(".").toAbsolutePath().normalize() : workspaceRoot.toAbsolutePath().normalize();
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }

    public List<EchoPackProfileSource> candidateSources(EchoPackId packId) {
        String id = packId.value();
        String directoryName = id.replace('_', '-');
        return List.of(
                EchoPackProfileSource.repoRelative(workspaceRoot, workspaceRoot.resolve("packs").resolve(directoryName).resolve("echo.pack.json"), true, false),
                EchoPackProfileSource.repoRelative(workspaceRoot, workspaceRoot.resolve("src/main/resources/data/echo/pack_profiles").resolve(id + ".json"), true, false),
                EchoPackProfileSource.repoRelative(workspaceRoot, workspaceRoot.resolve("metadata/official_packs").resolve(id).resolve("profile.json"), false, true),
                EchoPackProfileSource.repoRelative(workspaceRoot, workspaceRoot.resolve("metadata/official_packs").resolve(id + ".json"), false, true)
        );
    }

    public List<String> canonicalSourceLocations(EchoPackId packId) {
        String id = packId.value();
        String directoryName = id.replace('_', '-');
        return List.of(
                "packs/" + directoryName + "/echo.pack.json",
                "src/main/resources/data/echo/pack_profiles/" + id + ".json"
        );
    }

    public EchoPackProfileSource firstExistingSource(EchoPackId packId) {
        return candidateSources(packId).stream()
                .filter(source -> source.path() != null && Files.isRegularFile(source.path()))
                .findFirst()
                .orElse(null);
    }
}
