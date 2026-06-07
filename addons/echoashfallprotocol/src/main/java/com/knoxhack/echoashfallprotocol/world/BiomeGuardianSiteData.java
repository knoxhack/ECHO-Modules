package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfile;
import com.knoxhack.echoashfallprotocol.guardian.BiomeGuardianProfiles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class BiomeGuardianSiteData extends SavedData {
    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
                    Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
                    Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
            ).apply(instance, BlockPos::new)
    );

    public static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("guardianId", "").forGetter(Entry::guardianId),
                    Codec.STRING.optionalFieldOf("biomePath", "").forGetter(Entry::biomePath),
                    BLOCK_POS_CODEC.optionalFieldOf("entrance", BlockPos.ZERO).forGetter(Entry::entrance),
                    BLOCK_POS_CODEC.optionalFieldOf("arena", BlockPos.ZERO).forGetter(Entry::arena),
                    Codec.BOOL.optionalFieldOf("defeated", false).forGetter(Entry::defeated)
            ).apply(instance, Entry::new)
    );

    public static final Codec<BiomeGuardianSiteData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ENTRY_CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(data -> data.sites)
            ).apply(instance, BiomeGuardianSiteData::new)
    );

    public static final SavedDataType<BiomeGuardianSiteData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, "biome_guardian_sites"),
            BiomeGuardianSiteData::new,
            CODEC
    );

    private final List<Entry> sites = new ArrayList<>();

    public BiomeGuardianSiteData() {
    }

    private BiomeGuardianSiteData(List<Entry> sites) {
        sites.stream()
                .filter(BiomeGuardianSiteData::isValidEntry)
                .forEach(this.sites::add);
    }

    public static BiomeGuardianSiteData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public void addOrUpdate(BiomeGuardianProfile profile, BlockPos entrance, BlockPos arena) {
        sites.removeIf(entry -> entry.guardianId().equals(profile.bossPath())
                && entry.entrance().distSqr(entrance) < 128 * 128);
        sites.add(new Entry(profile.bossPath(), profile.biomePath(), entrance.immutable(), arena.immutable(), false));
        setDirty();
    }

    public void markDefeated(String guardianId, BlockPos pos) {
        boolean changed = false;
        for (int i = 0; i < sites.size(); i++) {
            Entry entry = sites.get(i);
            if (!entry.guardianId().equals(guardianId) || entry.defeated()) {
                continue;
            }
            if (entry.arena().distSqr(pos) > 160 * 160 && entry.entrance().distSqr(pos) > 160 * 160) {
                continue;
            }
            sites.set(i, new Entry(entry.guardianId(), entry.biomePath(), entry.entrance(), entry.arena(), true));
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    public Optional<Entry> nearestActive(BlockPos origin, String guardianId) {
        return sites.stream()
                .filter(entry -> !entry.defeated())
                .filter(entry -> guardianId == null || guardianId.isBlank() || entry.guardianId().equals(guardianId))
                .min(Comparator.comparingDouble(entry -> entry.entrance().distSqr(origin)));
    }

    public Optional<Entry> nearestActiveForMission(BlockPos origin, String missionId) {
        return BiomeGuardianProfiles.byMissionId(missionId)
                .flatMap(profile -> nearestActive(origin, profile.bossPath()));
    }

    public List<Entry> allSites() {
        return List.copyOf(sites);
    }

    private static boolean isValidEntry(Entry entry) {
        return entry != null
                && entry.guardianId() != null
                && !entry.guardianId().isBlank()
                && entry.biomePath() != null
                && !entry.biomePath().isBlank()
                && entry.entrance() != null
                && entry.arena() != null;
    }

    public record Entry(String guardianId, String biomePath, BlockPos entrance, BlockPos arena, boolean defeated) {
    }
}
