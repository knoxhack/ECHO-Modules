package com.knoxhack.echoindex.network;

import com.knoxhack.echoindex.EchoIndex;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record IndexRecipeQueryPacket(
        Identifier itemId,
        boolean recipes,
        boolean uses,
        boolean sources) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoIndex.MODID, "recipe_query");
    public static final Type<IndexRecipeQueryPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, IndexRecipeQueryPacket> CODEC =
            StreamCodec.of(IndexRecipeQueryPacket::write, IndexRecipeQueryPacket::read);

    public IndexRecipeQueryPacket {
        recipes = recipes || (!uses && !sources);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buffer, IndexRecipeQueryPacket packet) {
        buffer.writeUtf(packet.itemId() == null ? "" : packet.itemId().toString(), 192);
        int mask = (packet.recipes() ? 1 : 0) | (packet.uses() ? 2 : 0) | (packet.sources() ? 4 : 0);
        buffer.writeByte(mask);
    }

    private static IndexRecipeQueryPacket read(FriendlyByteBuf buffer) {
        Identifier itemId = Identifier.tryParse(buffer.readUtf(192));
        int mask = buffer.readUnsignedByte();
        return new IndexRecipeQueryPacket(itemId, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
    }
}
