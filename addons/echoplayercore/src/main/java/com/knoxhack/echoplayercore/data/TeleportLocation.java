package com.knoxhack.echoplayercore.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record TeleportLocation(
        ResourceKey<Level> dimension,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String reason,
        long timestamp) {

    public static final Codec<TeleportLocation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(t -> t.dimension().identifier().toString()),
            Codec.DOUBLE.fieldOf("x").forGetter(TeleportLocation::x),
            Codec.DOUBLE.fieldOf("y").forGetter(TeleportLocation::y),
            Codec.DOUBLE.fieldOf("z").forGetter(TeleportLocation::z),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(TeleportLocation::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(TeleportLocation::pitch),
            Codec.STRING.optionalFieldOf("reason", "").forGetter(TeleportLocation::reason),
            Codec.LONG.optionalFieldOf("timestamp", 0L).forGetter(TeleportLocation::timestamp)
    ).apply(instance, TeleportLocation::fromCodec));

    public TeleportLocation {
        dimension = dimension == null ? Level.OVERWORLD : dimension;
        reason = reason == null ? "" : reason;
    }

    public static TeleportLocation fromCodec(String dimensionId, double x, double y, double z,
                                               float yaw, float pitch, String reason, long timestamp) {
        Identifier id = Identifier.tryParse(dimensionId);
        ResourceKey<Level> key = id == null ? Level.OVERWORLD : ResourceKey.create(Registries.DIMENSION, id);
        return new TeleportLocation(key, x, y, z, yaw, pitch, reason, timestamp);
    }

    public static TeleportLocation fromPlayer(ServerPlayer player, String reason) {
        return new TeleportLocation(
                player.level().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                reason,
                System.currentTimeMillis()
        );
    }
}
