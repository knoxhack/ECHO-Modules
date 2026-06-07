package com.knoxhack.echo.npcore.integration.holomap;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.knoxhack.echo.npcore.data.NpcContactData;
import com.knoxhack.echo.npcore.entity.EchoNpcEntity;
import com.knoxhack.echo.npcore.profile.EchoNpcProfile;
import com.knoxhack.echo.npcore.profile.EchoNpcProfileManager;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public enum NpcMapDataProvider implements IMapDataProvider {
    INSTANCE;

    private static final Identifier PROVIDER_ID = id("provider/map_data");
    private static final Identifier CONTACT_LAYER = Identifier.fromNamespaceAndPath("echoholomap", "layer/npc_contacts");

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<IMapLayer> layers(Player player) {
        return List.of(new EchoMapLayer(CONTACT_LAYER, "NPC Contacts", 45, 0xFF66E8FF, true));
    }

    @Override
    public List<IMapMarker> markers(Player player) {
        if (player == null) {
            return List.of();
        }
        List<IMapMarker> markers = new ArrayList<>();
        for (EchoNpcEntity npc : player.level().getEntitiesOfClass(EchoNpcEntity.class,
                player.getBoundingBox().inflate(384.0D), npc -> npc != null && npc.isAlive())) {
            EchoNpcProfile profile = EchoNpcProfileManager.getOrFallback(npc.npcProfileId());
            if (!profile.integrations().mapMarker() || !NpcContactData.discovered(player, profile.id())) {
                continue;
            }
            BlockPos pos = npc.homePos();
            markers.add(new EchoMapMarker(
                    id("map/contact/" + profile.id().getNamespace() + "/" + sanitize(profile.id().getPath())),
                    CONTACT_LAYER,
                    PROVIDER_ID,
                    IMapMarker.MarkerKind.BASE_OUTPOST,
                    IMapMarker.MarkerState.DISCOVERED,
                    profile.displayName(),
                    profile.role() + " / " + profile.faction(),
                    player.level().dimension(),
                    pos.getX() + 0.5D,
                    pos.getY(),
                    pos.getZ() + 0.5D,
                    24.0F,
                    Identifier.fromNamespaceAndPath("echoholomap", "icon/npc/contact"),
                    null,
                    -1,
                    true));
        }
        return List.copyOf(markers);
    }

    @Override
    public boolean refresh(ServerPlayer player, String reason) {
        return player != null;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, sanitize(path));
    }

    private static String sanitize(String value) {
        String clean = value == null ? "unknown" : value.trim().toLowerCase(Locale.ROOT);
        clean = clean.replace('\\', '/').replace(':', '/').replaceAll("[^a-z0-9_./-]", "_");
        return clean.isBlank() ? "unknown" : clean;
    }
}
