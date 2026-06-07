package com.knoxhack.echothemecore.network;

import com.knoxhack.echothemecore.EchoThemeCore;
import com.knoxhack.echothemecore.content.ThemeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlayerThemeSyncPacket(Identifier themeId) implements CustomPacketPayload {
    private static final int MAX_ID = 96;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoThemeCore.MODID, "player_theme_sync");
    public static final Type<PlayerThemeSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerThemeSyncPacket> CODEC =
        StreamCodec.of(PlayerThemeSyncPacket::write, PlayerThemeSyncPacket::read);

    public PlayerThemeSyncPacket {
        themeId = themeId == null ? ThemeRegistry.ECHO_PLATFORM_ID : themeId;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, PlayerThemeSyncPacket packet) {
        buffer.writeUtf(packet.themeId().toString(), MAX_ID);
    }

    private static PlayerThemeSyncPacket read(RegistryFriendlyByteBuf buffer) {
        return new PlayerThemeSyncPacket(ThemeRegistry.parseThemeId(buffer.readUtf(MAX_ID)));
    }
}
