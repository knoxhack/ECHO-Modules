package com.knoxhack.echoplayercore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WarpLocation(
        String id,
        String displayName,
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean publicWarp,
        String requiredPermission) {

    public static final Codec<WarpLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(WarpLocation::id),
            Codec.STRING.optionalFieldOf("displayName", "").forGetter(WarpLocation::displayName),
            Codec.STRING.fieldOf("dimension").forGetter(w -> w.dimension().identifier().toString()),
            Codec.DOUBLE.fieldOf("x").forGetter(WarpLocation::x),
            Codec.DOUBLE.fieldOf("y").forGetter(WarpLocation::y),
            Codec.DOUBLE.fieldOf("z").forGetter(WarpLocation::z),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(WarpLocation::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(WarpLocation::pitch),
            Codec.BOOL.optionalFieldOf("publicWarp", true).forGetter(WarpLocation::publicWarp),
            Codec.STRING.optionalFieldOf("requiredPermission", "").forGetter(WarpLocation::requiredPermission)
    ).apply(instance, WarpLocation::fromCodec));

    public WarpLocation {
        id = id == null ? "" : id.strip();
        displayName = displayName == null || displayName.isBlank() ? id : displayName.strip();
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        requiredPermission = requiredPermission == null ? "" : requiredPermission.strip();
    }

    public static WarpLocation fromCodec(String id, String displayName, String dimensionId, double x, double y, double z,
                                           float yaw, float pitch, boolean publicWarp, String requiredPermission) {
        Identifier dimId = Identifier.tryParse(dimensionId);
        ResourceKey<Level> key = dimId == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, dimId);
        return new WarpLocation(id, displayName, key, x, y, z, yaw, pitch, publicWarp, requiredPermission);
    }
}
