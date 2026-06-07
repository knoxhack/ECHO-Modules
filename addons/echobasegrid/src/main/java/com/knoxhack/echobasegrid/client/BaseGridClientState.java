package com.knoxhack.echobasegrid.client;

import com.knoxhack.echobasegrid.network.BaseGridSnapshotPacket;
import com.knoxhack.echoscreencore.api.EchoScreens;
import java.util.List;

public final class BaseGridClientState {
    private static final BaseGridSnapshotPacket EMPTY = new BaseGridSnapshotPacket(
            "minecraft:overworld",
            0,
            0,
            0,
            0,
            "",
            "unclaimed",
            "Unclaimed",
            false,
            false,
            false,
            0,
            0,
            6,
            "Awaiting Base Grid snapshot.",
            List.of(),
            List.of(),
            List.of());

    private static volatile BaseGridSnapshotPacket snapshot = EMPTY;
    private static volatile String selectedDimension = "";
    private static volatile int selectedChunkX;
    private static volatile int selectedChunkZ;
    private static volatile long lastSnapshotRequestMillis;

    private BaseGridClientState() {
    }

    public static BaseGridSnapshotPacket snapshot() {
        return snapshot;
    }

    public static boolean hasSnapshot() {
        return snapshot != EMPTY && !snapshot.chunks().isEmpty();
    }

    public static void apply(BaseGridSnapshotPacket packet) {
        if (packet == null) {
            return;
        }
        snapshot = packet;
        selectedDimension = packet.dimension();
        selectedChunkX = packet.selectedChunkX();
        selectedChunkZ = packet.selectedChunkZ();
        EchoScreens.invalidateData();
    }

    public static void select(String key) {
        Selection selection = parseSelection(key);
        if (selection == null) {
            return;
        }
        selectedDimension = selection.dimension();
        selectedChunkX = selection.chunkX();
        selectedChunkZ = selection.chunkZ();
        EchoScreens.invalidateData();
    }

    public static String selectedDimension() {
        return selectedDimension == null || selectedDimension.isBlank() ? snapshot.dimension() : selectedDimension;
    }

    public static int selectedChunkX() {
        return selectedChunkX;
    }

    public static int selectedChunkZ() {
        return selectedChunkZ;
    }

    public static String selectedKey() {
        String dimension = selectedDimension();
        if (dimension == null || dimension.isBlank()) {
            return snapshot.selectedKey();
        }
        return dimension + "|" + selectedChunkX + "|" + selectedChunkZ;
    }

    public static boolean needsInitialSnapshot() {
        long now = System.currentTimeMillis();
        if (hasSnapshot()) {
            return false;
        }
        if (now - lastSnapshotRequestMillis < 1500L) {
            return false;
        }
        lastSnapshotRequestMillis = now;
        return true;
    }

    public static void markSnapshotRequested() {
        lastSnapshotRequestMillis = System.currentTimeMillis();
    }

    private static Selection parseSelection(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String[] parts = key.strip().split("\\|");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Selection(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private record Selection(String dimension, int chunkX, int chunkZ) {
    }
}
