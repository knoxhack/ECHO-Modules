package com.knoxhack.echopowergrid.client;

import com.knoxhack.echopowergrid.network.PowerGridNetworkSummaryPacket;
import java.util.List;

public final class PowerGridClientState {
    private static volatile PowerGridNetworkSummaryPacket snapshot =
            new PowerGridNetworkSummaryPacket(List.of(), "PowerGrid awaiting sync.", 0L);
    private static volatile long lastRequestMillis;

    private PowerGridClientState() {
    }

    public static void handle(PowerGridNetworkSummaryPacket packet) {
        if (packet != null) {
            snapshot = packet;
        }
    }

    public static PowerGridNetworkSummaryPacket snapshot() {
        return snapshot;
    }

    public static boolean shouldRequest(long intervalMillis) {
        long now = System.currentTimeMillis();
        if (now - lastRequestMillis < Math.max(250L, intervalMillis)) {
            return false;
        }
        lastRequestMillis = now;
        return true;
    }
}
