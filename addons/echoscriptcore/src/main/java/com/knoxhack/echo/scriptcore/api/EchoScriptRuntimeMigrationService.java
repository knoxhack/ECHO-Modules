package com.knoxhack.echo.scriptcore.api;

import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface EchoScriptRuntimeMigrationService {
    boolean available();

    String backendName();

    EchoScriptRuntimeSnapshot snapshotPlayer(ServerPlayer player);

    EchoScriptRuntimeSnapshot snapshotWorld(Level level);

    EchoScriptRuntimeMigrationReport previewPlayer(ServerPlayer player, String from, String to);

    EchoScriptRuntimeMigrationReport applyPlayer(ServerPlayer player, String from, String to);

    EchoScriptRuntimeMigrationReport previewWorld(Level level, String from, String to);

    EchoScriptRuntimeMigrationReport applyWorld(Level level, String from, String to);

    Path exportSnapshot(EchoScriptRuntimeSnapshot snapshot);
}
