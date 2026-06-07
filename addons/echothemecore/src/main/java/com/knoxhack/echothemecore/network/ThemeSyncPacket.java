package com.knoxhack.echothemecore.network;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ThemeSyncPacket(Identifier themeId) implements CustomPacketPayload {
    private static final int MAX_ID = 96;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "theme_sync");
    public static final Type<ThemeSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ThemeSyncPacket> CODEC =
        StreamCodec.of(ThemeSyncPacket::write, ThemeSyncPacket::read);

    public ThemeSyncPacket {
        themeId = themeId == null ? ThemeRegistry.ECHO_PLATFORM_ID : themeId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, ThemeSyncPacket packet) {
        buffer.writeUtf(packet.themeId().toString(), MAX_ID);
    }

    private static ThemeSyncPacket read(RegistryFriendlyByteBuf buffer) {
        return new ThemeSyncPacket(ThemeRegistry.parseThemeId(buffer.readUtf(MAX_ID)));
    }
}
