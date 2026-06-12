package com.echoplatform.echocore.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface IWorldRegionService extends IRegionService {
    default List<WorldRegionDefinition> regionDefinitions() {
        return regions();
    }

    default Optional<WorldRegionDefinition> regionDefinition(Identifier id) {
        return region(id);
    }

    default List<WorldRegionInstance> nearbyRegions(Level level, BlockPos pos, int radius) {
        return List.of();
    }

    default List<WorldRegionInstance> nearbyRegions(Player player, int radius) {
        return player == null ? List.of() : nearbyRegions(player.level(), player.blockPosition(), radius);
    }

    default List<WorldRegionInstance> activeRegions(Player player) {
        return List.of();
    }

    default boolean registerHazardDefinition(WorldHazardDefinition definition) {
        return false;
    }

    default List<WorldHazardDefinition> hazardDefinitions() {
        return List.of();
    }

    default Optional<WorldHazardDefinition> hazardDefinition(Identifier id) {
        return Optional.empty();
    }

    default WorldHazardSnapshot hazardSnapshot(Player player) {
        return WorldHazardSnapshot.nominal();
    }

    default WorldMarker revealMarker(ServerPlayer player, WorldMarker marker) {
        return marker;
    }

    default WorldMarker revealMarker(Level level, WorldMarker marker) {
        return marker;
    }

    default List<WorldMarker> nearbyMarkers(Level level, BlockPos pos, int radius) {
        return List.of();
    }

    default List<WorldMarker> markers(Player player) {
        return List.of();
    }

    default List<WorldMarker> markers(Level level, ResourceKey<Level> dimension) {
        return List.of();
    }

    default Optional<WorldMarker> markerById(Level level, Identifier markerId) {
        return Optional.empty();
    }

    default List<String> validateMarkers(Level level) {
        return List.of();
    }

    default WorldCoreValidationReport validationReport(Level level) {
        return new WorldCoreValidationReport(0, 0, 0, 0, 0, java.util.Map.of(), java.util.Map.of(), List.of(), List.of());
    }

    default WorldContextSnapshot worldContext(Player player) {
        return WorldContextSnapshot.empty();
    }

    default boolean recordStructureScan(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return false;
    }

    default boolean recordStructureEntry(ServerPlayer player, Identifier structureId, BlockPos pos,
            String displayName, String summary) {
        return false;
    }

    default boolean hasDiscoveredRegion(Player player, Identifier regionId) {
        return false;
    }

    default Set<Identifier> discoveredRegions(Player player) {
        return Set.of();
    }

    default boolean discoverRegion(ServerPlayer player, Identifier regionId, WorldDiscoverySource source) {
        return false;
    }

    default Optional<WorldRegionInstance> currentRegion(Player player) {
        return activeRegions(player).stream().findFirst();
    }

    default void tickPlayer(ServerPlayer player) {
    }
}
