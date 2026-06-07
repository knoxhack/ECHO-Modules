package com.knoxhack.echorelictech.integration.holomap;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.data.RelicVaultInfo;
import com.knoxhack.echorelictech.data.RelicVaultLoader;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class RelicTechHoloMapIntegration {
    private static final Identifier PROVIDER_ID = id("vault_markers");
    private static final Identifier LAYER_ID = id("layer/relic_vaults");
    private static final Identifier ICON_ID = id("textures/gui/holomap/relic_vault.png");
    private static boolean registered;

    private RelicTechHoloMapIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerMapDataProvider(new VaultMarkerProvider());
        EchoRelicTech.LOGGER.info("ECHO HoloMap integration loaded for RelicTech.");
    }

    private static final class VaultMarkerProvider implements IMapDataProvider {
        @Override
        public Identifier providerId() {
            return PROVIDER_ID;
        }

        @Override
        public List<IMapLayer> layers(Player player) {
            return List.of(new EchoMapLayer(LAYER_ID, "Relic Vaults", 50, 0xFFAA44, true));
        }

        @Override
        public List<IMapMarker> markers(Player player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return List.of();
            }
            List<IMapMarker> markers = new ArrayList<>();
            int order = 0;
            for (RelicTechApi.VaultMarker marker : RelicTechApi.getDiscoveredVaultMarkerRecords(serverPlayer)) {
                BlockPos pos = marker.pos();
                RelicVaultInfo info = RelicVaultLoader.get(marker.vaultId());
                String title = info != null ? info.displayName() : "Relic Vault";
                String summary = info != null ? info.markerText() : "Pre-Gridfall relic vault discovered.";
                markers.add(new EchoMapMarker(
                        id("vault/" + order + "/" + marker.vaultId().getPath()),
                        LAYER_ID,
                        marker.vaultId(),
                        IMapMarker.MarkerKind.MISSION,
                        IMapMarker.MarkerState.DISCOVERED,
                        title,
                        summary,
                        Level.OVERWORLD,
                        pos.getX() + 0.5,
                        pos.getY(),
                        pos.getZ() + 0.5,
                        12.0F,
                        ICON_ID,
                        marker.vaultId(),
                        order++,
                        true));
            }
            return markers;
        }

        @Override
        public boolean refresh(ServerPlayer player, String reason) {
            return player != null;
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
    }
}
