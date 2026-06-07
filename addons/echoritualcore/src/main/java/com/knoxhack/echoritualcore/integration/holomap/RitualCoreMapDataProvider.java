package com.knoxhack.echoritualcore.integration.holomap;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.ritual.RitualCoreMapMarkers;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum RitualCoreMapDataProvider implements IMapDataProvider {
    INSTANCE;

    private static final Identifier PROVIDER_ID = id("provider/ritual_markers");
    private static final Identifier LAYER_ID = id("layer/ritual_sites");
    private static final Identifier ICON_ID = id("textures/gui/holomap/ritual_site.png");

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return List.of(new EchoMapLayer(LAYER_ID, "Ritual Sites", 72, 0xFFB072FF, true));
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        if (!(player instanceof ServerPlayer)) {
            return List.of();
        }
        return RitualCoreMapMarkers.records(player).stream()
                .<IMapMarker>map(record -> new EchoMapMarker(
                        record.id(),
                        LAYER_ID,
                        record.source(),
                        record.precise() ? IMapMarker.MarkerKind.MISSION : IMapMarker.MarkerKind.HAZARD,
                        IMapMarker.MarkerState.DISCOVERED,
                        record.title(),
                        record.summary(),
                        record.dimension(),
                        record.pos().getX() + 0.5D,
                        record.pos().getY(),
                        record.pos().getZ() + 0.5D,
                        record.radius(),
                        ICON_ID,
                        id("arcana_ritualcore"),
                        -1,
                        record.precise()))
                .toList();
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        return player != null;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRitualCore.MODID, path);
    }
}
