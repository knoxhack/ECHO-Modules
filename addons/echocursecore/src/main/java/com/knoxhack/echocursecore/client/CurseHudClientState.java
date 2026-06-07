package com.knoxhack.echocursecore.client;

import com.knoxhack.echocursecore.network.CurseHudSyncPacket;

public final class CurseHudClientState {
    private static int echoRotStage;
    private static int glassVeinsStage;
    private static int riftHungerStage;
    private static int soulStaticStage;
    private static int phantomBurnStage;
    private static int bloodDebtStage;
    private static int voidMarkStage;
    private static int contractCount;
    private static int cleanseableCount;
    private static long lastChanged;
    private static long syncedAtMillis;

    private CurseHudClientState() {
    }

    public static void apply(CurseHudSyncPacket packet) {
        echoRotStage = clamp(packet.echoRotStage());
        glassVeinsStage = clamp(packet.glassVeinsStage());
        riftHungerStage = clamp(packet.riftHungerStage());
        soulStaticStage = clamp(packet.soulStaticStage());
        phantomBurnStage = clamp(packet.phantomBurnStage());
        bloodDebtStage = clamp(packet.bloodDebtStage());
        voidMarkStage = clamp(packet.voidMarkStage());
        contractCount = Math.max(0, packet.contractCount());
        cleanseableCount = Math.max(0, packet.cleanseableCount());
        lastChanged = Math.max(0L, packet.lastChanged());
        syncedAtMillis = System.currentTimeMillis();
    }

    public static int echoRotStage() {
        return echoRotStage;
    }

    public static int glassVeinsStage() {
        return glassVeinsStage;
    }

    public static int riftHungerStage() {
        return riftHungerStage;
    }

    public static int soulStaticStage() {
        return soulStaticStage;
    }

    public static int phantomBurnStage() {
        return phantomBurnStage;
    }

    public static int bloodDebtStage() {
        return bloodDebtStage;
    }

    public static int voidMarkStage() {
        return voidMarkStage;
    }

    public static int contractCount() {
        return contractCount;
    }

    public static int cleanseableCount() {
        return cleanseableCount;
    }

    public static long lastChanged() {
        return lastChanged;
    }

    public static boolean hasActiveCurse() {
        return echoRotStage > 0 || glassVeinsStage > 0 || riftHungerStage > 0 || soulStaticStage > 0
                || phantomBurnStage > 0 || bloodDebtStage > 0 || voidMarkStage > 0;
    }

    public static boolean recentlySynced() {
        return System.currentTimeMillis() - syncedAtMillis < 3_000L;
    }

    private static int clamp(int stage) {
        return Math.max(0, Math.min(5, stage));
    }
}
