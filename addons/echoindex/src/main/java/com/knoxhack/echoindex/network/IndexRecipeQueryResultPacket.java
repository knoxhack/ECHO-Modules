package com.knoxhack.echoindex.network;

import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IndexRecipeQueryResultPacket(Identifier itemId, CompoundTag result) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoIndex.MODID, "recipe_query_result");
    public static final Type<IndexRecipeQueryResultPacket> TYPE = new Type<>(ID);
    private static final long MAX_RESULT_NBT_BYTES = 8L * 1024L * 1024L;
    public static final StreamCodec<FriendlyByteBuf, IndexRecipeQueryResultPacket> CODEC =
            StreamCodec.of(IndexRecipeQueryResultPacket::write, IndexRecipeQueryResultPacket::read);

    public IndexRecipeQueryResultPacket {
        result = result == null ? new CompoundTag() : result.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, IndexRecipeQueryResultPacket packet) {
        buffer.writeUtf(packet.itemId() == null ? "" : packet.itemId().toString(), 192);
        buffer.writeNbt(packet.result());
    }

    private static IndexRecipeQueryResultPacket read(FriendlyByteBuf buffer) {
        Identifier itemId = Identifier.tryParse(buffer.readUtf(192));
        Tag tag = buffer.readNbt(NbtAccounter.create(MAX_RESULT_NBT_BYTES));
        return new IndexRecipeQueryResultPacket(itemId, tag instanceof CompoundTag compound ? compound : new CompoundTag());
    }
}
