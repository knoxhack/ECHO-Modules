package com.knoxhack.echo.scriptcore.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface EchoScriptRuntimeState {
    boolean available();

    String backendName();

    void registerKeys(EchoScriptRegistryView registry);

    boolean worldState(Level level, Identifier state);

    boolean setWorldState(Level level, Identifier state, boolean value);

    long factionReputation(Player player, Identifier faction);

    boolean setFactionReputation(Player player, Identifier faction, long amount);

    long changeFactionReputation(Player player, Identifier faction, long delta);

    long customMetric(Player player, String metric);

    boolean setCustomMetric(Player player, String metric, long value);

    long changeCustomMetric(Player player, String metric, long delta);

    boolean branchMarker(Player player, String marker);

    boolean setBranchMarker(Player player, String marker, boolean value);

    boolean dialogueChoiceMade(Player player, Identifier dialogue, String choice);

    boolean recordDialogueChoice(Player player, Identifier dialogue, String choice);
}
