package com.knoxhack.echopowergrid.integration.holomap;

import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.api.EchoGridState;
import com.knoxhack.echopowergrid.api.EchoPowerGridApi;
import com.knoxhack.echopowergrid.api.EchoPowerNodeType;
import com.knoxhack.echopowergrid.api.PowerGridAlert;
import com.knoxhack.echopowergrid.api.PowerGridNetworkSummary;
import com.knoxhack.echopowergrid.api.PowerGridNodeSummary;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class PowerGridMapDataProvider implements IMapDataProvider {
    public static final PowerGridMapDataProvider INSTANCE = new PowerGridMapDataProvider();
    private static final Identifier PROVIDER_ID = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "holomap/power_grid");
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "power_networks");
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "textures/gui/holomap/power_network.png");

    private PowerGridMapDataProvider() {
    }

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return List.of(new EchoMapLayer(LAYER_ID, "Power Networks", 82, 0xFF55DDEF, true));
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(serverPlayer.level() instanceof ServerLevel level)) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        EchoPowerGridApi.loadedNetworkSummaries(level).stream()
                .map(PowerGridMapDataProvider::marker)
                .forEach(markers::add);
        EchoPowerGridApi.loadedNodeSummaries(level).stream()
                .filter(PowerGridMapDataProvider::isDepthMarker)
                .limit(48)
                .map(PowerGridMapDataProvider::nodeMarker)
                .forEach(markers::add);
        EchoPowerGridApi.alerts(level).stream()
                .map(PowerGridMapDataProvider::alertMarker)
                .forEach(markers::add);
        return List.copyOf(markers);
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        return player != null;
    }

    private static EchoMapMarker marker(PowerGridNetworkSummary summary) {
        Identifier markerId = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID,
                "power_network/" + summary.networkId().toString().replace('-', '_'));
        String title = "Power Network " + summary.networkId().toString().substring(0, 8);
        String status = summary.state().name() + " / " + summary.quality().name();
        String summaryText = status
                + " / gen " + summary.totalGeneration() + " EP/t"
                + " / demand " + summary.totalDemand() + " EP/t"
                + " / stored " + summary.totalStored() + "/" + summary.totalCapacity() + " EP"
                + (summary.state() == EchoGridState.BROWNOUT || summary.state() == EchoGridState.OVERLOADED
                        ? " / attention required"
                        : "");
        return new EchoMapMarker(
                markerId,
                LAYER_ID,
                PROVIDER_ID,
                IMapMarker.MarkerKind.BASE_OUTPOST,
                IMapMarker.MarkerState.DISCOVERED,
                title,
                summaryText,
                summary.dimension(),
                summary.anchorPos().getX() + 0.5D,
                summary.anchorPos().getY(),
                summary.anchorPos().getZ() + 0.5D,
                28.0F,
                ICON,
                null,
                -1,
                true);
    }

    private static boolean isDepthMarker(PowerGridNodeSummary summary) {
        if (summary == null) {
            return false;
        }
        return summary.type() == EchoPowerNodeType.SUBSTATION && summary.transferLimit() >= 3000L
                || summary.type() == EchoPowerNodeType.CABLE && summary.transferLimit() >= 1200L
                || summary.type() == EchoPowerNodeType.STORAGE && summary.capacity() >= 160000L;
    }

    private static EchoMapMarker nodeMarker(PowerGridNodeSummary summary) {
        String kind = nodeMarkerKind(summary);
        Identifier markerId = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID,
                "power_node/" + kind + "/" + summary.pos().asLong());
        return new EchoMapMarker(
                markerId,
                LAYER_ID,
                PROVIDER_ID,
                IMapMarker.MarkerKind.BASE_OUTPOST,
                IMapMarker.MarkerState.DISCOVERED,
                nodeMarkerTitle(summary),
                nodeMarkerSummary(summary),
                summary.dimension(),
                summary.pos().getX() + 0.5D,
                summary.pos().getY(),
                summary.pos().getZ() + 0.5D,
                16.0F,
                ICON,
                null,
                -1,
                true);
    }

    private static String nodeMarkerKind(PowerGridNodeSummary summary) {
        return switch (summary.type()) {
            case SUBSTATION -> "factory_substation_anchor";
            case STORAGE -> "field_battery_room";
            case CABLE -> "high_voltage_trunk";
            default -> "power_support";
        };
    }

    private static String nodeMarkerTitle(PowerGridNodeSummary summary) {
        return switch (summary.type()) {
            case SUBSTATION -> "Factory Substation";
            case STORAGE -> "Field Battery Room";
            case CABLE -> "High Voltage Trunk";
            default -> "Power Support Node";
        };
    }

    private static String nodeMarkerSummary(PowerGridNodeSummary summary) {
        return switch (summary.type()) {
            case SUBSTATION -> "Commissioning anchor / " + summary.transferLimit()
                    + " EP/t transfer / scan with Lens and link to grid mission.";
            case STORAGE -> "Reserve buffer / " + summary.storedEnergy() + "/" + summary.capacity()
                    + " EP / mark before scaling machine halls.";
            case CABLE -> "High-voltage trunk / " + summary.transferLimit()
                    + " EP/t / keep routes readable and budgeted.";
            default -> "PowerGrid support marker.";
        };
    }

    private static EchoMapMarker alertMarker(PowerGridAlert alert) {
        Identifier markerId = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID,
                "power_alert/" + alert.code() + "/" + alert.pos().asLong());
        return new EchoMapMarker(
                markerId,
                LAYER_ID,
                PROVIDER_ID,
                IMapMarker.MarkerKind.BASE_OUTPOST,
                IMapMarker.MarkerState.DISCOVERED,
                "Power Alert: " + alert.code(),
                alert.level().name() + " / " + alert.message(),
                alert.dimension(),
                alert.pos().getX() + 0.5D,
                alert.pos().getY(),
                alert.pos().getZ() + 0.5D,
                18.0F,
                ICON,
                null,
                -1,
                true);
    }
}
