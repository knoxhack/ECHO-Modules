package com.knoxhack.echomultiblockcore.integration;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.api.MultiblockMapMarkerSnapshot;
import com.knoxhack.echomultiblockcore.api.MultiblockRole;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum MultiblockMapDataProvider implements IMapDataProvider {
    INSTANCE;

    public static final Identifier LAYER_ID = EchoMultiblockCore.id("multiblocks");

    @Override
    public Identifier providerId() {
        return EchoMultiblockCore.id("multiblock_map_data");
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return List.of(new EchoMapLayer(LAYER_ID, "Multiblocks", 90, 0xFF00D8FF, true));
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        return MultiblockIntegrationServices.mapMarkers(player).stream()
                .map(this::marker)
                .toList();
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        return MultiblockIntegrationServices.refreshMapMarkers(player, reason);
    }

    private IMapMarker marker(MultiblockMapMarkerSnapshot snapshot) {
        return new EchoMapMarker(
                snapshot.markerId(),
                LAYER_ID,
                providerId(),
                kind(snapshot.role()),
                state(snapshot.state()),
                snapshot.title(),
                snapshot.summary(),
                snapshot.dimension(),
                snapshot.position().getX() + 0.5D,
                snapshot.position().getY(),
                snapshot.position().getZ() + 0.5D,
                24.0F,
                EchoMultiblockCore.id("icon/multiblock"),
                null,
                -1,
                true);
    }

    private static IMapMarker.MarkerKind kind(MultiblockRole role) {
        return switch (role == null ? MultiblockRole.INFRASTRUCTURE : role) {
            case TRANSPORT, LOGISTICS -> IMapMarker.MarkerKind.ROUTE;
            case DEFENSE, COMBAT, SURVIVAL -> IMapMarker.MarkerKind.HAZARD;
            case ORBITAL -> IMapMarker.MarkerKind.ORBITAL_SCAN;
            case NEXUS, MAGIC -> IMapMarker.MarkerKind.NEXUS_ANOMALY;
            case RESEARCH, ARCHIVE -> IMapMarker.MarkerKind.DRONE_SCAN;
            case COMMAND, INFRASTRUCTURE, PRODUCTION, AGRICULTURE -> IMapMarker.MarkerKind.BASE_OUTPOST;
        };
    }

    private static IMapMarker.MarkerState state(MultiblockState state) {
        return switch (state == null ? MultiblockState.UNBUILT : state) {
            case UNBUILT, INCOMPLETE, VALIDATING -> IMapMarker.MarkerState.LOCKED;
            case OFFLINE -> IMapMarker.MarkerState.HIDDEN;
            case FORMED, ACTIVE, PAUSED, DAMAGED, JAMMED, OVERLOADED -> IMapMarker.MarkerState.DISCOVERED;
        };
    }
}
