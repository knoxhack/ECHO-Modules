package com.knoxhack.echoashfallprotocol.client.drone;

import com.knoxhack.echoashfallprotocol.api.drone.EchoDroneScanCategory;
import com.knoxhack.echoashfallprotocol.network.DroneMarkersPacket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public final class DroneClientState {
    private static final List<DroneMarkersPacket.Entry> MARKERS = new ArrayList<>();

    private DroneClientState() {
    }

    public static void accept(DroneMarkersPacket packet) {
        synchronized (MARKERS) {
            MARKERS.clear();
            if (packet != null && packet.markers() != null) {
                MARKERS.addAll(packet.markers());
            }
        }
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) {
            synchronized (MARKERS) {
                MARKERS.clear();
            }
            return;
        }

        long now = level.getGameTime();
        String dimension = level.dimension().identifier().toString();
        synchronized (MARKERS) {
            Iterator<DroneMarkersPacket.Entry> iterator = MARKERS.iterator();
            while (iterator.hasNext()) {
                DroneMarkersPacket.Entry marker = iterator.next();
                if (marker.expiresAt() <= now) {
                    iterator.remove();
                    continue;
                }
                long remaining = marker.expiresAt() - now;
                long cadence = remaining < 40L ? 16L : remaining < 100L ? 10L : 6L;
                long phase = Math.floorMod(marker.pos().asLong(), cadence);
                if (Math.floorMod(now, cadence) == phase && dimension.equals(marker.dimension())) {
                    emitPing(level, marker);
                }
            }
        }
    }

    private static void emitPing(Level level, DroneMarkersPacket.Entry marker) {
        double x = marker.pos().getX() + 0.5D;
        double y = marker.pos().getY() + 1.0D;
        double z = marker.pos().getZ() + 0.5D;
        ParticleOptions particle = particle(marker.category());
        level.addParticle(particle, x, y, z, 0.0D, 0.015D, 0.0D);
        if (marker.precise() && level.getGameTime() + 24L < marker.expiresAt()) {
            level.addParticle(particle, x, y + 0.25D, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static ParticleOptions particle(EchoDroneScanCategory category) {
        if (category == null) {
            return ParticleTypes.END_ROD;
        }
        return switch (category) {
            case MISSION -> ParticleTypes.ENCHANT;
            case HAZARD -> ParticleTypes.SMALL_FLAME;
            case HOSTILE -> ParticleTypes.ELECTRIC_SPARK;
            case LOOT -> ParticleTypes.HAPPY_VILLAGER;
            case RESOURCE -> ParticleTypes.WAX_ON;
            case CONTAINER -> ParticleTypes.END_ROD;
        };
    }

    public static int markerCount() {
        synchronized (MARKERS) {
            return MARKERS.size();
        }
    }
}
