package com.knoxhack.echoholomap.integration;

import com.knoxhack.echo.machinecore.EchoMachineRuntimeRegistry;
import com.knoxhack.echo.machinecore.EchoMachineRuntimeSnapshot;
import com.knoxhack.echo.machinecore.EchoMachineUiBridge;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.HoloMapIds;
import com.knoxhack.echoholomap.api.HoloMapLayerData;
import com.knoxhack.echoholomap.api.HoloMapMarkerData;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import com.knoxhack.echoholomap.map.HoloMapService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class MachineCoreHoloMapIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier LAYER = HoloMapIds.layer("machines");

    private MachineCoreHoloMapIntegration() {
    }

    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            HoloMapService.INSTANCE.registerHoloProvider(Provider.INSTANCE);
        }
    }

    private enum Provider implements IHoloMapDataProvider {
        INSTANCE;

        @Override
        public Identifier providerId() {
            return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "machinecore_runtime");
        }

        @Override
        public List<HoloMapLayerData> layers(Player player) {
            return List.of(new HoloMapLayerData(LAYER, "Machines", 255, 0xFF66E8FF, true));
        }

        @Override
        public List<HoloMapMarkerData> markers(Player player) {
            ResourceKey<Level> fallbackDimension = player == null || player.level() == null
                    ? Level.OVERWORLD
                    : player.level().dimension();
            return EchoMachineRuntimeRegistry.snapshots(player).stream()
                    .map(snapshot -> marker(snapshot, fallbackDimension))
                    .flatMap(Optional::stream)
                    .toList();
        }

        private static Optional<HoloMapMarkerData> marker(EchoMachineRuntimeSnapshot snapshot, ResourceKey<Level> fallbackDimension) {
            Optional<BlockPos> pos = EchoMachineUiBridge.position(snapshot);
            if (pos.isEmpty()) {
                return Optional.empty();
            }
            BlockPos blockPos = pos.get();
            Identifier markerId = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID,
                    "machinecore/" + EchoMachineUiBridge.sanitizePath(snapshot.id().value())
                            + "/" + Long.toUnsignedString(blockPos.asLong()));
            String summary = snapshot.state().serializedName().replace('_', ' ')
                    + " / " + EchoMachineUiBridge.energyLine(snapshot)
                    + " / " + EchoMachineUiBridge.processLine(snapshot);
            return Optional.of(new HoloMapMarkerData(
                    markerId,
                    LAYER,
                    providerIdStatic(),
                    IMapMarker.MarkerKind.GENERIC,
                    snapshot.state().degraded() ? IMapMarker.MarkerState.LOCKED : IMapMarker.MarkerState.DISCOVERED,
                    snapshot.displayName(),
                    summary,
                    EchoMachineUiBridge.dimension(snapshot, fallbackDimension),
                    blockPos.getX() + 0.5D,
                    blockPos.getY() + 0.5D,
                    blockPos.getZ() + 0.5D,
                    2.0F,
                    EchoMachineUiBridge.machineBlockIdentifier(snapshot),
                    null,
                    0,
                    HoloMapPrecision.PRECISE,
                    snapshot.state().degraded() ? 80 : 60));
        }

        private static Identifier providerIdStatic() {
            return Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "machinecore_runtime");
        }
    }
}
