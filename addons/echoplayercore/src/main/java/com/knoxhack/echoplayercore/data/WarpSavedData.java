package com.knoxhack.echoplayercore.data;

import com.knoxhack.echoplayercore.EchoPlayerCore;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class WarpSavedData extends SavedData {
    public static final Codec<WarpSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WarpLocation.CODEC.listOf().optionalFieldOf("warps", List.of())
                    .forGetter(WarpSavedData::warpList)
    ).apply(instance, WarpSavedData::fromCodec));

    public static final SavedDataType<WarpSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EchoPlayerCore.MODID, "warps"),
            WarpSavedData::new,
            CODEC
    );

    private final Map<String, WarpLocation> warps = new LinkedHashMap<>();

    public WarpSavedData() {
    }

    private WarpSavedData(List<WarpLocation> warpList) {
        for (WarpLocation w : warpList) {
            if (w != null && w.id() != null && !w.id().isBlank()) {
                warps.put(w.id().toLowerCase(java.util.Locale.ROOT), w);
            }
        }
    }

    public static WarpSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public synchronized Optional<WarpLocation> getWarp(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(warps.get(id.toLowerCase(java.util.Locale.ROOT)));
    }

    public synchronized List<WarpLocation> warpList() {
        return List.copyOf(warps.values());
    }

    public synchronized boolean setWarp(WarpLocation warp) {
        if (warp == null || warp.id() == null || warp.id().isBlank()) {
            return false;
        }
        warps.put(warp.id().toLowerCase(java.util.Locale.ROOT), warp);
        setDirty();
        return true;
    }

    public synchronized boolean deleteWarp(String id) {
        if (id == null) {
            return false;
        }
        boolean removed = warps.remove(id.toLowerCase(java.util.Locale.ROOT)) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public synchronized int warpCount() {
        return warps.size();
    }

    private static WarpSavedData fromCodec(List<WarpLocation> warps) {
        return new WarpSavedData(warps);
    }
}
