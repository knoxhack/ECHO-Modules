package com.knoxhack.echonetcore.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

public final class EchoPayloadCodecs {
    public static final int ID = 160;
    public static final int SMALL_TEXT = 512;
    public static final int ACTION_PAYLOAD = 4096;

    private EchoPayloadCodecs() {
    }

    public static void writeIdentifier(FriendlyByteBuf buffer, Identifier id) {
        writeUtf(buffer, id == null ? "" : id.toString(), ID);
    }

    public static Identifier readIdentifier(FriendlyByteBuf buffer) {
        Identifier id = Identifier.tryParse(readUtf(buffer, ID));
        return id == null ? Identifier.fromNamespaceAndPath("echonetcore", "unknown") : id;
    }

    public static void writeOptionalIdentifier(FriendlyByteBuf buffer, Identifier id) {
        buffer.writeBoolean(id != null);
        if (id != null) {
            writeIdentifier(buffer, id);
        }
    }

    public static Identifier readOptionalIdentifier(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? readIdentifier(buffer) : null;
    }

    public static void writeOptionalBlockPos(FriendlyByteBuf buffer, BlockPos pos) {
        buffer.writeBoolean(pos != null);
        if (pos != null) {
            buffer.writeBlockPos(pos);
        }
    }

    public static BlockPos readOptionalBlockPos(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readBlockPos() : null;
    }

    public static <E extends Enum<E>> void writeEnum(FriendlyByteBuf buffer, E value, E fallback) {
        buffer.writeEnum(value == null ? fallback : value);
    }

    public static <E extends Enum<E>> E readEnum(FriendlyByteBuf buffer, Class<E> enumClass, E fallback) {
        try {
            E value = buffer.readEnum(enumClass);
            return value == null ? fallback : value;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    public static void writeUtf(FriendlyByteBuf buffer, String value, int maxLength) {
        buffer.writeUtf(value == null ? "" : value, Math.max(1, maxLength));
    }

    public static String readUtf(FriendlyByteBuf buffer, int maxLength) {
        return buffer.readUtf(Math.max(1, maxLength)).trim();
    }
}
