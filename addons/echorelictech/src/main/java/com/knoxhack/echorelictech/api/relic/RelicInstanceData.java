package com.knoxhack.echorelictech.api.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record RelicInstanceData(
        Identifier relicId,
        RelicCondition condition,
        int instabilityModifier,
        BlockPos boundPos,
        String boundDimension,
        int charge,
        boolean corruptionFlag,
        boolean overclockFlag,
        boolean containmentFlag,
        boolean identified,
        int cooldownRemaining
) {
    public static final RelicInstanceData EMPTY = new RelicInstanceData(
            Identifier.fromNamespaceAndPath("echorelictech", "unknown"),
            RelicCondition.UNKNOWN, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0);

    private static final Codec<Identifier> IDENTIFIER_CODEC = Codec.STRING.xmap(Identifier::parse, Identifier::toString);

    private static final Codec<BlockPos> BLOCK_POS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("z").forGetter(BlockPos::getZ)
    ).apply(instance, BlockPos::new));

    public static final Codec<RelicInstanceData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IDENTIFIER_CODEC.fieldOf("relic_id").forGetter(RelicInstanceData::relicId),
            RelicCondition.CODEC.fieldOf("condition").forGetter(RelicInstanceData::condition),
            Codec.INT.optionalFieldOf("instability_modifier", 0).forGetter(RelicInstanceData::instabilityModifier),
            BLOCK_POS_CODEC.optionalFieldOf("bound_pos", BlockPos.ZERO).forGetter(RelicInstanceData::boundPos),
            Codec.STRING.optionalFieldOf("bound_dimension", "").forGetter(RelicInstanceData::boundDimension),
            Codec.INT.optionalFieldOf("charge", 0).forGetter(RelicInstanceData::charge),
            Codec.BOOL.optionalFieldOf("corruption", false).forGetter(RelicInstanceData::corruptionFlag),
            Codec.BOOL.optionalFieldOf("overclock", false).forGetter(RelicInstanceData::overclockFlag),
            Codec.BOOL.optionalFieldOf("contained", false).forGetter(RelicInstanceData::containmentFlag),
            Codec.BOOL.optionalFieldOf("identified", false).forGetter(RelicInstanceData::identified),
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(RelicInstanceData::cooldownRemaining)
    ).apply(instance, RelicInstanceData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RelicInstanceData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString), RelicInstanceData::relicId,
            ByteBufCodecs.fromCodec(RelicCondition.CODEC), RelicInstanceData::condition,
            ByteBufCodecs.VAR_INT, RelicInstanceData::instabilityModifier,
            BlockPos.STREAM_CODEC, RelicInstanceData::boundPos,
            ByteBufCodecs.STRING_UTF8, RelicInstanceData::boundDimension,
            ByteBufCodecs.VAR_INT, RelicInstanceData::charge,
            ByteBufCodecs.BOOL, RelicInstanceData::corruptionFlag,
            ByteBufCodecs.BOOL, RelicInstanceData::overclockFlag,
            ByteBufCodecs.BOOL, RelicInstanceData::containmentFlag,
            ByteBufCodecs.BOOL, RelicInstanceData::identified,
            ByteBufCodecs.VAR_INT, RelicInstanceData::cooldownRemaining,
            RelicInstanceData::new);

    public RelicInstanceData withCondition(RelicCondition c) {
        return new RelicInstanceData(relicId, c, instabilityModifier, boundPos, boundDimension, charge, corruptionFlag, overclockFlag, containmentFlag, identified, cooldownRemaining);
    }

    public RelicInstanceData withBound(BlockPos pos, String dim) {
        return new RelicInstanceData(relicId, condition, instabilityModifier, pos, dim, charge, corruptionFlag, overclockFlag, containmentFlag, identified, cooldownRemaining);
    }

    public RelicInstanceData withCharge(int c) {
        return new RelicInstanceData(relicId, condition, instabilityModifier, boundPos, boundDimension, c, corruptionFlag, overclockFlag, containmentFlag, identified, cooldownRemaining);
    }

    public RelicInstanceData withCooldown(int cd) {
        return new RelicInstanceData(relicId, condition, instabilityModifier, boundPos, boundDimension, charge, corruptionFlag, overclockFlag, containmentFlag, identified, cd);
    }

    public RelicInstanceData makeIdentified() {
        return new RelicInstanceData(relicId, condition, instabilityModifier, boundPos, boundDimension, charge, corruptionFlag, overclockFlag, containmentFlag, true, cooldownRemaining);
    }
}
