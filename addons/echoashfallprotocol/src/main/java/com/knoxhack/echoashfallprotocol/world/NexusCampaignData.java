package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayProfile;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayProfiles;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayState;
import com.knoxhack.echoashfallprotocol.endgame.NexusRelayType;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-saved Nexus Warfront state layered beside the irreversible Nexus path choice.
 */
public class NexusCampaignData extends SavedData {
    public static final int REQUIRED_RELAY_SCAN_COUNT = 6;
    public static final int REQUIRED_RELAY_RESOLUTION_COUNT = 3;
    public static final int MAX_INSTABILITY = 100;

    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
                    Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
                    Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
            ).apply(instance, BlockPos::new)
    );

    public static final Codec<NexusCampaignData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("instability", 0).forGetter(d -> d.instability),
                    Codec.BOOL.optionalFieldOf("awakened", false).forGetter(d -> d.awakened),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("relays", Map.of())
                            .forGetter(NexusCampaignData::serializedRelays),
                    Codec.INT.optionalFieldOf("readinessRestore", 0).forGetter(d -> d.readinessRestore),
                    Codec.INT.optionalFieldOf("readinessDestroy", 0).forGetter(d -> d.readinessDestroy),
                    Codec.INT.optionalFieldOf("readinessControl", 0).forGetter(d -> d.readinessControl),
                    Codec.BOOL.optionalFieldOf("siegeComplete", false).forGetter(d -> d.siegeComplete),
                    Codec.BOOL.optionalFieldOf("wardenDefeated", false).forGetter(d -> d.wardenDefeated),
                    Codec.BOOL.optionalFieldOf("finaleComplete", false).forGetter(d -> d.finaleComplete),
                    Codec.LONG.optionalFieldOf("lastCrisisTick", 0L).forGetter(d -> d.lastCrisisTick),
                    BLOCK_POS_CODEC.optionalFieldOf("nexusPos", BlockPos.ZERO).forGetter(d -> d.nexusPos),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("warfrontContent", Map.of())
                            .forGetter(NexusCampaignData::serializedWarfrontContent)
            ).apply(instance, (instability, awakened, relays, readinessRestore, readinessDestroy,
                    readinessControl, siegeComplete, wardenDefeated, finaleComplete, lastCrisisTick, nexusPos,
                    warfrontContent) -> {
                NexusCampaignData data = new NexusCampaignData();
                data.instability = clampInstability(instability);
                data.awakened = awakened;
                data.readinessRestore = Math.max(0, readinessRestore);
                data.readinessDestroy = Math.max(0, readinessDestroy);
                data.readinessControl = Math.max(0, readinessControl);
                data.siegeComplete = siegeComplete;
                data.wardenDefeated = wardenDefeated;
                data.finaleComplete = finaleComplete;
                data.lastCrisisTick = Math.max(0L, lastCrisisTick);
                data.nexusPos = nexusPos == null ? BlockPos.ZERO : nexusPos;
                data.loadRelays(relays);
                data.loadWarfrontContent(warfrontContent);
                return data;
            })
    );

    public static final SavedDataType<NexusCampaignData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "nexus_campaign"),
            NexusCampaignData::new,
            CODEC
    );

    private int instability = 0;
    private boolean awakened = false;
    private final EnumMap<NexusRelayType, NexusRelayState> relays = new EnumMap<>(NexusRelayType.class);
    private int readinessRestore = 0;
    private int readinessDestroy = 0;
    private int readinessControl = 0;
    private boolean siegeComplete = false;
    private boolean wardenDefeated = false;
    private boolean finaleComplete = false;
    private long lastCrisisTick = 0L;
    private BlockPos nexusPos = BlockPos.ZERO;
    private final EnumMap<NexusRelayType, BlockPos> relaySites = new EnumMap<>(NexusRelayType.class);
    private final EnumMap<NexusRelayType, Boolean> relayGenerated = new EnumMap<>(NexusRelayType.class);
    private final EnumMap<NexusRelayType, Boolean> relayEncounterStarted = new EnumMap<>(NexusRelayType.class);
    private final EnumMap<NexusRelayType, Boolean> relayEncounterComplete = new EnumMap<>(NexusRelayType.class);
    private final EnumMap<NexusRelayType, Integer> relayPressureKills = new EnumMap<>(NexusRelayType.class);
    private final EnumMap<NexusRelayType, Boolean> relayCommanderDefeated = new EnumMap<>(NexusRelayType.class);
    private boolean finalBossSummoned = false;
    private String finalBossPath = "";

    public NexusCampaignData() {
        for (NexusRelayType type : NexusRelayType.values()) {
            relays.put(type, NexusRelayState.UNKNOWN);
        }
        resetRelayContentMaps();
    }

    public static NexusCampaignData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public int getInstability() {
        return instability;
    }

    public boolean isAwakened() {
        return awakened;
    }

    public int getReadinessRestore() {
        return readinessRestore;
    }

    public int getReadinessDestroy() {
        return readinessDestroy;
    }

    public int getReadinessControl() {
        return readinessControl;
    }

    public boolean isSiegeComplete() {
        return siegeComplete;
    }

    public boolean isWardenDefeated() {
        return wardenDefeated;
    }

    public boolean isFinaleComplete() {
        return finaleComplete;
    }

    public long getLastCrisisTick() {
        return lastCrisisTick;
    }

    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public NexusRelayState getRelayState(NexusRelayType type) {
        return relays.getOrDefault(type, NexusRelayState.UNKNOWN);
    }

    public Map<NexusRelayType, NexusRelayState> getRelays() {
        return Map.copyOf(relays);
    }

    public boolean hasRelaySite(NexusRelayType type) {
        BlockPos pos = getRelaySite(type);
        return pos != null && !pos.equals(BlockPos.ZERO);
    }

    public BlockPos getRelaySite(NexusRelayType type) {
        return relaySites.getOrDefault(type, BlockPos.ZERO);
    }

    public boolean isRelayGenerated(NexusRelayType type) {
        return relayGenerated.getOrDefault(type, false);
    }

    public boolean isRelayEncounterStarted(NexusRelayType type) {
        return relayEncounterStarted.getOrDefault(type, false);
    }

    public boolean isRelayEncounterComplete(NexusRelayType type) {
        return relayEncounterComplete.getOrDefault(type, false);
    }

    public int getRelayPressureKills(NexusRelayType type) {
        return Math.max(0, relayPressureKills.getOrDefault(type, 0));
    }

    public boolean isRelayCommanderDefeated(NexusRelayType type) {
        return relayCommanderDefeated.getOrDefault(type, false);
    }

    public boolean isFinalBossSummoned() {
        return finalBossSummoned;
    }

    public String getFinalBossPath() {
        return finalBossPath == null ? "" : finalBossPath;
    }

    public boolean awaken(BlockPos corePos) {
        boolean changed = !awakened || !copy(corePos).equals(nexusPos);
        awakened = true;
        nexusPos = copy(corePos);
        if (instability < 25) {
            instability = 25;
            changed = true;
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public boolean scanRelays() {
        boolean changed = false;
        for (NexusRelayType type : NexusRelayType.values()) {
            if (relays.get(type) == NexusRelayState.UNKNOWN) {
                relays.put(type, NexusRelayState.SCANNED);
                changed = true;
            }
        }
        if (changed) {
            instability = Math.max(instability, 35);
            setDirty();
        }
        return changed;
    }

    public boolean resolveRelay(NexusRelayType type, NexusRelayState outcome) {
        if (type == null || outcome == null || !outcome.isResolved()) {
            return false;
        }
        NexusRelayState previous = getRelayState(type);
        if (previous == NexusRelayState.UNKNOWN || previous.isResolved()) {
            return false;
        }
        relays.put(type, outcome);
        applyReadiness(outcome);
        instability = switch (outcome) {
            case STABILIZED -> Math.max(0, instability - 12);
            case SEVERED -> Math.min(MAX_INSTABILITY, Math.max(0, instability - 4) + 6);
            case OVERRIDDEN -> Math.max(0, instability - 7);
            default -> instability;
        };
        setDirty();
        return true;
    }

    public boolean activateRelay(NexusRelayType type) {
        if (type == null || getRelayState(type).isResolved()) {
            return false;
        }
        if (getRelayState(type) == NexusRelayState.ACTIVE) {
            return false;
        }
        relays.put(type, NexusRelayState.ACTIVE);
        setDirty();
        return true;
    }

    public NexusRelayType firstUnresolvedRelay() {
        for (NexusRelayType type : NexusRelayType.values()) {
            if (!getRelayState(type).isResolved()) {
                return type;
            }
        }
        return null;
    }

    public NexusRelayType firstEncounterCompleteUnresolvedRelay() {
        for (NexusRelayType type : NexusRelayType.values()) {
            if (!getRelayState(type).isResolved() && isRelayEncounterComplete(type)) {
                return type;
            }
        }
        return null;
    }

    public NexusRelayType firstEncounterPendingRelay() {
        for (NexusRelayType type : NexusRelayType.values()) {
            if (!getRelayState(type).isResolved() && !isRelayEncounterComplete(type)) {
                return type;
            }
        }
        return null;
    }

    public int getScannedRelayCount() {
        int count = 0;
        for (NexusRelayState state : relays.values()) {
            if (state != NexusRelayState.UNKNOWN) {
                count++;
            }
        }
        return count;
    }

    public int getResolvedRelayCount() {
        int count = 0;
        for (NexusRelayState state : relays.values()) {
            if (state.isResolved()) {
                count++;
            }
        }
        return count;
    }

    public boolean markSiegeComplete() {
        if (siegeComplete) {
            return false;
        }
        siegeComplete = true;
        instability = Math.max(0, instability - 15);
        setDirty();
        return true;
    }

    public boolean markWardenDefeated() {
        if (wardenDefeated) {
            return false;
        }
        wardenDefeated = true;
        setDirty();
        return true;
    }

    public boolean markFinaleComplete() {
        if (finaleComplete) {
            return false;
        }
        finaleComplete = true;
        instability = Math.max(0, instability - 20);
        setDirty();
        return true;
    }

    public boolean reduceInstability(int amount) {
        int previous = instability;
        instability = clampInstability(instability - Math.max(0, amount));
        if (previous != instability) {
            setDirty();
        }
        return previous != instability;
    }

    public void assignRelaySite(NexusRelayType type, BlockPos pos) {
        if (type == null || pos == null) {
            return;
        }
        relaySites.put(type, copy(pos));
        setDirty();
    }

    public void markRelayGenerated(NexusRelayType type) {
        if (type == null) {
            return;
        }
        if (isRelayGenerated(type)) {
            return;
        }
        relayGenerated.put(type, true);
        setDirty();
    }

    public void markRelayEncounterStarted(NexusRelayType type) {
        if (type == null) {
            return;
        }
        if (isRelayEncounterStarted(type)) {
            return;
        }
        relayEncounterStarted.put(type, true);
        setDirty();
    }

    public void markRelayEncounterComplete(NexusRelayType type) {
        if (type == null) {
            return;
        }
        if (isRelayEncounterComplete(type)) {
            return;
        }
        relayEncounterComplete.put(type, true);
        setDirty();
    }

    public void incrementRelayPressureKill(NexusRelayType type) {
        if (type == null) {
            return;
        }
        relayPressureKills.put(type, getRelayPressureKills(type) + 1);
        setDirty();
    }

    public void markRelayCommanderDefeated(NexusRelayType type) {
        if (type == null) {
            return;
        }
        if (isRelayCommanderDefeated(type)) {
            return;
        }
        relayCommanderDefeated.put(type, true);
        setDirty();
    }

    public boolean isRelayObjectiveSatisfied(NexusRelayType type, NexusRelayProfile profile) {
        if (type == null || profile == null) {
            return false;
        }
        return getRelayPressureKills(type) >= profile.requiredPressureKills()
                && (!profile.needsCommander() || isRelayCommanderDefeated(type));
    }

    public void markFinalBossSummoned(PostNexusData.NexusPath path) {
        finalBossSummoned = true;
        finalBossPath = path == null ? "" : path.name();
        setDirty();
    }

    public boolean isFinalBossSummonedFor(PostNexusData.NexusPath path) {
        return finalBossSummoned && path != null && path.name().equalsIgnoreCase(getFinalBossPath());
    }

    public void clearFinalBossSummoned() {
        finalBossSummoned = false;
        finalBossPath = "";
        setDirty();
    }

    public boolean tickInstability(ServerLevel level, boolean pathChosen) {
        if (!awakened || finaleComplete) {
            return false;
        }
        long now = level.getGameTime();
        if (now - lastCrisisTick < 2400L) {
            return false;
        }
        lastCrisisTick = now;
        int unresolvedRelays = NexusRelayType.values().length - getResolvedRelayCount();
        int delta = pathChosen ? Math.max(0, unresolvedRelays - 3) : Math.max(1, 4 + unresolvedRelays);
        if (siegeComplete) {
            delta = Math.max(0, delta - 2);
        }
        int previous = instability;
        instability = clampInstability(instability + delta);
        if (previous != instability) {
            setDirty();
        }
        return previous != instability;
    }

    public boolean isWarfrontComplete() {
        return awakened && getResolvedRelayCount() >= REQUIRED_RELAY_RESOLUTION_COUNT && siegeComplete;
    }

    public String statusLine() {
        if (!awakened) {
            return "Nexus Core dormant. Stand near the unresolved Core to anchor the warfront signal.";
        }
        return "Instability " + instability + "%"
                + " | Prime Relays " + getResolvedRelayCount() + "/" + REQUIRED_RELAY_RESOLUTION_COUNT
                + " | Countermeasure " + (siegeComplete ? "survived" : "armed")
                + " | Finale " + (finaleComplete ? "sealed" : "unsealed");
    }

    public List<String> relaySummaryLines() {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(finalBossStatusLine()),
                relays.entrySet().stream()
                        .map(entry -> entry.getKey().displayName() + ": " + entry.getValue().name()
                                + " | site " + formatPos(getRelaySite(entry.getKey()))
                                + " | " + relayEncounterStatus(entry.getKey()))
        ).toList();
    }

    public String relaySummaryPayload() {
        return String.join("\n", relaySummaryLines());
    }

    public String relayEncounterStatus(NexusRelayType type) {
        NexusRelayProfile profile = NexusRelayProfiles.byType(type).orElse(null);
        if (!hasRelaySite(type)) {
            return "site unassigned";
        }
        if (getRelayState(type).isResolved()) {
            return "resolved";
        }
        if (!isRelayGenerated(type)) {
            return "site assigned, shell pending";
        }
        if (isRelayEncounterComplete(type)) {
            return "encounter complete";
        }
        if (!isRelayEncounterStarted(type)) {
            return "encounter ready";
        }
        int required = profile == null ? 0 : profile.requiredPressureKills();
        String commander = profile != null && profile.needsCommander()
                ? ", commander " + (isRelayCommanderDefeated(type) ? "defeated" : "pending")
                : "";
        return "encounter active " + getRelayPressureKills(type) + "/" + required + commander;
    }

    private String finalBossStatusLine() {
        if (finaleComplete) {
            return "Final Protocol: complete";
        }
        if (finalBossSummoned) {
            return "Final Boss: " + getFinalBossPath() + " signal live or recovery armed";
        }
        return "Final Boss: locked until path operation";
    }

    public void bootstrapWarfrontComplete(BlockPos corePos) {
        awakened = true;
        nexusPos = copy(corePos);
        for (NexusRelayType type : NexusRelayType.values()) {
            if (!getRelayState(type).isResolved()) {
                relays.put(type, NexusRelayState.STABILIZED);
            }
            relayEncounterComplete.put(type, true);
        }
        readinessRestore = Math.max(readinessRestore, REQUIRED_RELAY_RESOLUTION_COUNT);
        siegeComplete = true;
        instability = 0;
        setDirty();
    }

    public void resetForTests() {
        instability = 0;
        awakened = false;
        relays.clear();
        for (NexusRelayType type : NexusRelayType.values()) {
            relays.put(type, NexusRelayState.UNKNOWN);
        }
        readinessRestore = 0;
        readinessDestroy = 0;
        readinessControl = 0;
        siegeComplete = false;
        wardenDefeated = false;
        finaleComplete = false;
        lastCrisisTick = 0L;
        nexusPos = BlockPos.ZERO;
        resetRelayContentMaps();
        finalBossSummoned = false;
        finalBossPath = "";
        setDirty();
    }

    private void applyReadiness(NexusRelayState outcome) {
        switch (outcome) {
            case STABILIZED -> readinessRestore++;
            case SEVERED -> readinessDestroy++;
            case OVERRIDDEN -> readinessControl++;
            default -> {
            }
        }
    }

    private Map<String, String> serializedRelays() {
        Map<String, String> serialized = new LinkedHashMap<>();
        for (NexusRelayType type : NexusRelayType.values()) {
            serialized.put(type.name(), getRelayState(type).name());
        }
        return serialized;
    }

    private void loadRelays(Map<String, String> serialized) {
        relays.clear();
        for (NexusRelayType type : NexusRelayType.values()) {
            String value = serialized == null ? "" : serialized.get(type.name());
            relays.put(type, NexusRelayState.byName(value));
        }
    }

    private Map<String, String> serializedWarfrontContent() {
        Map<String, String> serialized = new LinkedHashMap<>();
        for (NexusRelayType type : NexusRelayType.values()) {
            BlockPos site = getRelaySite(type);
            if (site != null && !site.equals(BlockPos.ZERO)) {
                serialized.put("site." + type.name(), serializePos(site));
            }
            putIfTrue(serialized, "generated." + type.name(), isRelayGenerated(type));
            putIfTrue(serialized, "started." + type.name(), isRelayEncounterStarted(type));
            putIfTrue(serialized, "complete." + type.name(), isRelayEncounterComplete(type));
            putIfTrue(serialized, "commander." + type.name(), isRelayCommanderDefeated(type));
            if (getRelayPressureKills(type) > 0) {
                serialized.put("pressure." + type.name(), Integer.toString(getRelayPressureKills(type)));
            }
        }
        putIfTrue(serialized, "finalBoss.summoned", finalBossSummoned);
        if (finalBossPath != null && !finalBossPath.isBlank()) {
            serialized.put("finalBoss.path", finalBossPath);
        }
        return serialized;
    }

    private void loadWarfrontContent(Map<String, String> serialized) {
        resetRelayContentMaps();
        if (serialized == null || serialized.isEmpty()) {
            return;
        }
        for (NexusRelayType type : NexusRelayType.values()) {
            relaySites.put(type, parsePos(serialized.get("site." + type.name())));
            relayGenerated.put(type, Boolean.parseBoolean(serialized.getOrDefault("generated." + type.name(), "false")));
            relayEncounterStarted.put(type, Boolean.parseBoolean(serialized.getOrDefault("started." + type.name(), "false")));
            relayEncounterComplete.put(type, Boolean.parseBoolean(serialized.getOrDefault("complete." + type.name(), "false")));
            relayCommanderDefeated.put(type, Boolean.parseBoolean(serialized.getOrDefault("commander." + type.name(), "false")));
            relayPressureKills.put(type, parseNonNegativeInt(serialized.get("pressure." + type.name())));
        }
        finalBossSummoned = Boolean.parseBoolean(serialized.getOrDefault("finalBoss.summoned", "false"));
        finalBossPath = serialized.getOrDefault("finalBoss.path", "");
    }

    private void resetRelayContentMaps() {
        relaySites.clear();
        relayGenerated.clear();
        relayEncounterStarted.clear();
        relayEncounterComplete.clear();
        relayPressureKills.clear();
        relayCommanderDefeated.clear();
        for (NexusRelayType type : NexusRelayType.values()) {
            relaySites.put(type, BlockPos.ZERO);
            relayGenerated.put(type, false);
            relayEncounterStarted.put(type, false);
            relayEncounterComplete.put(type, false);
            relayPressureKills.put(type, 0);
            relayCommanderDefeated.put(type, false);
        }
    }

    private static int clampInstability(int value) {
        return Math.max(0, Math.min(MAX_INSTABILITY, value));
    }

    private static BlockPos copy(BlockPos pos) {
        if (pos == null) {
            return BlockPos.ZERO;
        }
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    private static void putIfTrue(Map<String, String> target, String key, boolean value) {
        if (value) {
            target.put(key, "true");
        }
    }

    private static String serializePos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos parsePos(String value) {
        if (value == null || value.isBlank()) {
            return BlockPos.ZERO;
        }
        String[] parts = value.split(",");
        if (parts.length != 3) {
            return BlockPos.ZERO;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()));
        } catch (NumberFormatException ignored) {
            return BlockPos.ZERO;
        }
    }

    private static int parseNonNegativeInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String formatPos(BlockPos pos) {
        if (pos == null || pos.equals(BlockPos.ZERO)) {
            return "unassigned";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
