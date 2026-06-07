package com.knoxhack.echobasegrid.integration.holomap;

import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.client.HoloMapChunkMenuAction;
import com.knoxhack.echoholomap.client.HoloMapClientChunkActions;
import com.knoxhack.echoholomap.client.IHoloMapClientChunkActionProvider;
import com.knoxhack.echoholomap.network.HoloMapClientState;
import com.knoxhack.echoholomap.network.HoloMapSnapshotPacket;
import com.knoxhack.echoholomap.network.HoloMapTerrainClientState;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;

public final class BaseGridHoloMapClientIntegration {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int CLAIM_COLOR = 0xFF2EF7A6;
    private static final int UNCLAIM_COLOR = 0xFFFFDA73;
    private static final int TRUSTED_COLOR = 0xFFFFDA73;
    private static final int OCCUPIED_COLOR = 0xFFFF5C7A;

    private BaseGridHoloMapClientIntegration() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        HoloMapClientChunkActions.register(BaseGridClaimClientActionProvider.INSTANCE);
    }

    private static final class BaseGridClaimClientActionProvider implements IHoloMapClientChunkActionProvider {
        private static final BaseGridClaimClientActionProvider INSTANCE = new BaseGridClaimClientActionProvider();

        @Override
        public Identifier providerId() {
            return BaseGridHoloMapIntegration.PROVIDER_ID;
        }

        @Override
        public List<HoloMapChunkMenuAction> actions(String dimension, int chunkX, int chunkZ) {
            if (!HoloMapTerrainClientState.hasRenderableTile(dimension, chunkX, chunkZ)) {
                return List.of();
            }
            String relation = relationAt(dimension, chunkX, chunkZ);
            return switch (relation) {
                case "" -> List.of(new HoloMapChunkMenuAction(BaseGridHoloMapIntegration.PROVIDER_ID,
                        BaseGridHoloMapIntegration.ACTION_CLAIM, "CLAIM CHUNK", true, CLAIM_COLOR));
                case "mine" -> List.of(new HoloMapChunkMenuAction(BaseGridHoloMapIntegration.PROVIDER_ID,
                        BaseGridHoloMapIntegration.ACTION_UNCLAIM, "UNCLAIM", true, UNCLAIM_COLOR));
                case "trusted" -> List.of(new HoloMapChunkMenuAction(BaseGridHoloMapIntegration.PROVIDER_ID,
                        BaseGridHoloMapIntegration.ACTION_CLAIM, "TRUSTED CLAIM", false, TRUSTED_COLOR));
                default -> List.of(new HoloMapChunkMenuAction(BaseGridHoloMapIntegration.PROVIDER_ID,
                        BaseGridHoloMapIntegration.ACTION_CLAIM, "OCCUPIED", false, OCCUPIED_COLOR));
            };
        }

        private static String relationAt(String dimension, int chunkX, int chunkZ) {
            for (HoloMapSnapshotPacket.ZoneData zone : HoloMapClientState.zonesForDimension(dimension)) {
                if (!BaseGridHoloMapIntegration.isBaseGridClaimZone(zone.sourceId())
                        || zone.shape() != HoloMapZoneShape.RECT
                        || chunkX != chunkX(zone)
                        || chunkZ != chunkZ(zone)) {
                    continue;
                }
                return BaseGridHoloMapIntegration.relationFromZoneId(zone.id());
            }
            return "";
        }

        private static int chunkX(HoloMapSnapshotPacket.ZoneData zone) {
            return Math.floorDiv((int) Math.floor(zone.x()), 16);
        }

        private static int chunkZ(HoloMapSnapshotPacket.ZoneData zone) {
            return Math.floorDiv((int) Math.floor(zone.z()), 16);
        }
    }
}
