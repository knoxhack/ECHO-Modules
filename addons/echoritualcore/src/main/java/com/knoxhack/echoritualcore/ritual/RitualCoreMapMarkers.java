package com.knoxhack.echoritualcore.ritual;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoritualcore.EchoRitualCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class RitualCoreMapMarkers {
    private static final String ROOT = EchoRitualCore.MODID + "_markers";
    private static final int MAX_MARKERS = 32;

    private RitualCoreMapMarkers() {
    }

    public static void recordRitualSite(ServerPlayer player, BlockPos pos, Identifier ritualId, String title, String summary) {
        record(player, "ritual_site", pos, ritualId, title, summary, 10.0F, true);
    }

    public static void recordRiftHint(ServerPlayer player, BlockPos altarPos) {
        BlockPos markerPos = altarPos.offset(48, 0, -48);
        record(player, "rift_hint", markerPos, EchoRitualCore.id("rift_crack_reveal"),
                "Rift Crack Trace",
                "Ritual triangulation found unstable rift noise near this area. Coordinates are deliberately imprecise.",
                96.0F,
                false);
    }

    public static List<MarkerRecord> records(Player player) {
        if (player == null) {
            return List.of();
        }
        CompoundTag root = root(player);
        int count = Math.min(MAX_MARKERS, root.getIntOr("count", 0));
        List<MarkerRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "marker_" + i + "_";
            Identifier id = Identifier.tryParse(root.getStringOr(prefix + "id", ""));
            Identifier source = Identifier.tryParse(root.getStringOr(prefix + "source", ""));
            Identifier dimensionId = Identifier.tryParse(root.getStringOr(prefix + "dimension", Level.OVERWORLD.identifier().toString()));
            if (id == null || source == null || dimensionId == null) {
                continue;
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            records.add(new MarkerRecord(
                    id,
                    source,
                    root.getStringOr(prefix + "title", "Ritual Trace"),
                    root.getStringOr(prefix + "summary", "RitualCore marker."),
                    dimension,
                    new BlockPos(root.getIntOr(prefix + "x", 0), root.getIntOr(prefix + "y", 64), root.getIntOr(prefix + "z", 0)),
                    root.getFloatOr(prefix + "radius", 10.0F),
                    root.getBooleanOr(prefix + "precise", true)));
        }
        return List.copyOf(records);
    }

    private static void record(ServerPlayer player, String kind, BlockPos pos, Identifier source, String title,
            String summary, float radius, boolean precise) {
        if (player == null || pos == null) {
            return;
        }
        CompoundTag root = root(player);
        ResourceKey<Level> dimension = player.level().dimension();
        Identifier markerId = EchoRitualCore.id("map/" + kind + "/" + sanitize(dimension.identifier().toString())
                + "/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
        int existing = find(root, markerId);
        int index = existing >= 0 ? existing : Math.min(root.getIntOr("count", 0), MAX_MARKERS - 1);
        write(root, index, markerId, source, title, summary, dimension, pos, radius, precise);
        if (existing < 0 && root.getIntOr("count", 0) < MAX_MARKERS) {
            root.putInt("count", root.getIntOr("count", 0) + 1);
        }
        EchoCoreServices.refreshMapMarkers(player, "ritualcore:" + kind);
    }

    private static int find(CompoundTag root, Identifier markerId) {
        int count = Math.min(MAX_MARKERS, root.getIntOr("count", 0));
        for (int i = 0; i < count; i++) {
            if (markerId.toString().equals(root.getStringOr("marker_" + i + "_id", ""))) {
                return i;
            }
        }
        return -1;
    }

    private static void write(CompoundTag root, int index, Identifier id, Identifier source, String title,
            String summary, ResourceKey<Level> dimension, BlockPos pos, float radius, boolean precise) {
        String prefix = "marker_" + index + "_";
        root.putString(prefix + "id", id.toString());
        root.putString(prefix + "source", source == null ? EchoRitualCore.id("ritual").toString() : source.toString());
        root.putString(prefix + "title", title == null ? "Ritual Trace" : title);
        root.putString(prefix + "summary", summary == null ? "RitualCore marker." : summary);
        root.putString(prefix + "dimension", (dimension == null ? Level.OVERWORLD : dimension).identifier().toString());
        root.putInt(prefix + "x", pos.getX());
        root.putInt(prefix + "y", pos.getY());
        root.putInt(prefix + "z", pos.getZ());
        root.putFloat(prefix + "radius", Math.max(1.0F, radius));
        root.putBoolean(prefix + "precise", precise);
    }

    private static CompoundTag root(Player player) {
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        player.getPersistentData().put(ROOT, root);
        return root;
    }

    private static String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(':', '_').replace('/', '_').replace('.', '_');
    }

    public record MarkerRecord(
            Identifier id,
            Identifier source,
            String title,
            String summary,
            ResourceKey<Level> dimension,
            BlockPos pos,
            float radius,
            boolean precise) {
    }
}
