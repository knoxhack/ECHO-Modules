package com.knoxhack.echobasegrid.integration.holomap;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimActionResult;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRecord;
import com.knoxhack.echobasegrid.data.BaseGridSavedData;
import com.knoxhack.echobasegrid.network.BaseGridNetwork;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import com.echoplatform.echocore.api.IMapMarker;
import com.knoxhack.echoholomap.api.HoloMapChunkActionResult;
import com.knoxhack.echoholomap.api.HoloMapChunkSelection;
import com.knoxhack.echoholomap.api.HoloMapLayerData;
import com.knoxhack.echoholomap.api.HoloMapPrecision;
import com.knoxhack.echoholomap.api.HoloMapQuery;
import com.knoxhack.echoholomap.api.HoloMapZoneData;
import com.knoxhack.echoholomap.api.HoloMapZonePattern;
import com.knoxhack.echoholomap.api.HoloMapZoneShape;
import com.knoxhack.echoholomap.api.IHoloMapChunkActionProvider;
import com.knoxhack.echoholomap.api.IHoloMapDataProvider;
import com.knoxhack.echoholomap.map.HoloMapChunkActions;
import com.knoxhack.echoholomap.map.HoloMapService;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class BaseGridHoloMapIntegration {
    public static final Identifier PROVIDER_ID = EchoBaseGrid.id("holomap/base_grid_claims");
    public static final Identifier LAYER_ID = EchoBaseGrid.id("layer/base_grid_claims");
    public static final Identifier ACTION_CLAIM = EchoBaseGrid.id("holomap/action/claim_chunk");
    public static final Identifier ACTION_UNCLAIM = EchoBaseGrid.id("holomap/action/unclaim_chunk");

    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final int MINE_FILL = 0x332EF7A6;
    private static final int MINE_OUTLINE = 0xCC2EF7A6;
    private static final int TRUSTED_FILL = 0x33FFDA73;
    private static final int TRUSTED_OUTLINE = 0xCCFFDA73;
    private static final int OCCUPIED_FILL = 0x33FF5C7A;
    private static final int OCCUPIED_OUTLINE = 0xCCFF5C7A;

    private BaseGridHoloMapIntegration() {
    }

    public static void register() {
        boolean firstRegistration = REGISTERED.compareAndSet(false, true);
        boolean providerRegistered = HoloMapService.INSTANCE.registerHoloProvider(BaseGridClaimMapProvider.INSTANCE);
        boolean actionRegistered = HoloMapChunkActions.register(BaseGridClaimChunkActionProvider.INSTANCE);
        if (firstRegistration || providerRegistered || actionRegistered) {
            EchoBaseGrid.LOGGER.info("ECHO: Base Grid HoloMap integration online.");
        }
    }

    public static boolean isBaseGridClaimZone(Identifier sourceId) {
        return PROVIDER_ID.equals(sourceId);
    }

    public static String relationFromZoneId(Identifier zoneId) {
        if (zoneId == null) {
            return "";
        }
        String path = zoneId.getPath();
        String prefix = "holomap/claim/";
        if (!path.startsWith(prefix)) {
            return "";
        }
        String remainder = path.substring(prefix.length());
        int slash = remainder.indexOf('/');
        return slash <= 0 ? remainder : remainder.substring(0, slash);
    }

    private enum ClaimRelation {
        MINE("mine", "Base Grid Claim", "Claimed by you", IMapMarker.MarkerState.CHECKED, MINE_FILL, MINE_OUTLINE),
        TRUSTED("trusted", "Trusted Base Claim", "You are trusted here", IMapMarker.MarkerState.DISCOVERED,
                TRUSTED_FILL, TRUSTED_OUTLINE),
        OCCUPIED("occupied", "Occupied Base Claim", "Another operator controls this chunk",
                IMapMarker.MarkerState.DISCOVERED, OCCUPIED_FILL, OCCUPIED_OUTLINE);

        private final String id;
        private final String title;
        private final String summary;
        private final IMapMarker.MarkerState state;
        private final int fill;
        private final int outline;

        ClaimRelation(String id, String title, String summary, IMapMarker.MarkerState state, int fill, int outline) {
            this.id = id;
            this.title = title;
            this.summary = summary;
            this.state = state;
            this.fill = fill;
            this.outline = outline;
        }
    }

    private static final class BaseGridClaimMapProvider implements IHoloMapDataProvider {
        private static final BaseGridClaimMapProvider INSTANCE = new BaseGridClaimMapProvider();

        @Override
        public Identifier providerId() {
            return PROVIDER_ID;
        }

        @Override
        public List<HoloMapLayerData> layers(Player player) {
            return List.of(new HoloMapLayerData(LAYER_ID, "Base Grid Claims", 430, MINE_OUTLINE, true));
        }

        @Override
        public List<HoloMapZoneData> zones(Player player, HoloMapQuery query) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || !(serverPlayer.level() instanceof ServerLevel level)) {
                return List.of();
            }
            ResourceKey<Level> dimension = query == null ? serverPlayer.level().dimension() : query.dimension();
            UUID playerId = serverPlayer.getUUID();
            HoloMapTerrainSavedData terrain = HoloMapTerrainSavedData.get(level);
            ArrayList<HoloMapZoneData> zones = new ArrayList<>();
            for (ClaimRecord claim : BaseGridSavedData.get(level).claims()) {
                if (!dimension.identifier().toString().equals(claim.dimension())) {
                    continue;
                }
                int minX = claim.chunkX() * 16;
                int minZ = claim.chunkZ() * 16;
                if (query != null && !query.intersectsBounds(dimension, minX, minZ, minX + 16, minZ + 16)) {
                    continue;
                }
                if (!terrain.hasRenderableTile(playerId, dimension, claim.chunkX(), claim.chunkZ())) {
                    continue;
                }
                zones.add(zoneFor(claim, relation(claim, playerId), dimension));
            }
            return List.copyOf(zones);
        }

        private static ClaimRelation relation(ClaimRecord claim, UUID playerId) {
            if (claim.ownedBy(playerId)) {
                return ClaimRelation.MINE;
            }
            if (claim.allows(playerId, ClaimPermission.BUILD) || claim.member(playerId).isPresent()) {
                return ClaimRelation.TRUSTED;
            }
            return ClaimRelation.OCCUPIED;
        }

        private static HoloMapZoneData zoneFor(ClaimRecord claim, ClaimRelation relation,
                ResourceKey<Level> dimension) {
            Identifier id = EchoBaseGrid.id("holomap/claim/" + relation.id + "/" + safePath(claim.dimension())
                    + "/" + claim.chunkX() + "/" + claim.chunkZ());
            String summary = relation.summary + " | Owner: " + claim.ownerName();
            return new HoloMapZoneData(
                    id,
                    LAYER_ID,
                    PROVIDER_ID,
                    HoloMapZoneShape.RECT,
                    HoloMapZonePattern.SOLID,
                    relation.state,
                    relation.title,
                    summary,
                    dimension,
                    claim.chunkX() * 16.0D + 8.0D,
                    64.0D,
                    claim.chunkZ() * 16.0D + 8.0D,
                    0.0F,
                    16.0F,
                    16.0F,
                    relation.fill,
                    relation.outline,
                    HoloMapPrecision.PRECISE,
                    120,
                    List.of());
        }
    }

    private static final class BaseGridClaimChunkActionProvider implements IHoloMapChunkActionProvider {
        private static final BaseGridClaimChunkActionProvider INSTANCE = new BaseGridClaimChunkActionProvider();

        @Override
        public Identifier providerId() {
            return PROVIDER_ID;
        }

        @Override
        public HoloMapChunkActionResult handle(ServerPlayer player, HoloMapChunkSelection selection,
                Identifier actionId) {
            if (selection == null || actionId == null) {
                return HoloMapChunkActionResult.failure("No Chunk", "No Base Grid chunk action was selected.");
            }
            String dimension = selection.dimensionId().toString();
            ClaimActionResult result;
            if (ACTION_CLAIM.equals(actionId)) {
                result = BaseGridClaimService.claim(player, dimension, selection.chunkX(), selection.chunkZ());
            } else if (ACTION_UNCLAIM.equals(actionId)) {
                result = BaseGridClaimService.unclaim(player, dimension, selection.chunkX(), selection.chunkZ());
            } else {
                return HoloMapChunkActionResult.failure("Unknown Action", "Base Grid does not know that HoloMap action.");
            }
            BaseGridNetwork.sendSnapshot(player, dimension, selection.chunkX(), selection.chunkZ(),
                    result.title().isBlank() ? result.message() : result.title() + ": " + result.message());
            return new HoloMapChunkActionResult(result.success(), result.title(), result.message());
        }
    }

    private static String safePath(String value) {
        String clean = value == null ? "unknown" : value.toLowerCase(Locale.ROOT).strip();
        return clean.replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
    }
}
