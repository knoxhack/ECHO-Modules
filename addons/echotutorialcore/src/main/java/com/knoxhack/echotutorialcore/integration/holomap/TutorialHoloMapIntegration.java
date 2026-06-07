package com.knoxhack.echotutorialcore.integration.holomap;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoMapLayer;
import com.knoxhack.echocore.api.EchoMapMarker;
import com.knoxhack.echocore.api.IMapDataProvider;
import com.knoxhack.echocore.api.IMapLayer;
import com.knoxhack.echocore.api.IMapMarker;
import com.knoxhack.echotutorialcore.EchoTutorialCore;
import com.knoxhack.echotutorialcore.api.TutorialCoreApi;
import com.knoxhack.echotutorialcore.data.TutorialPlayerData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class TutorialHoloMapIntegration {
    private static boolean registered;

    private TutorialHoloMapIntegration() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        EchoCoreServices.registerMapDataProvider(Provider.INSTANCE);
        EchoTutorialCore.LOGGER.info("ECHO: TutorialCore integrated with HoloMap. Route prep provider registered.");
    }

    public static void reportMapOpened(Player player) {
        TutorialCoreApi.reportHoloMapOpened(player);
    }

    public static void reportRouteReviewed(Player player, Identifier routeId) {
        TutorialCoreApi.reportHoloMapOpened(player);
        TutorialCoreApi.reportProgress(player, Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, "reviewed_route"));
    }

    private enum Provider implements IMapDataProvider {
        INSTANCE;

        private static final Identifier PROVIDER_ID = id("holomap/tutorial_guidance");
        private static final Identifier LAYER_ID = id("route_prep");

        @Override
        public Identifier providerId() {
            return PROVIDER_ID;
        }

        @Override
        public List<IMapLayer> layers(Player player) {
            return List.of(new EchoMapLayer(LAYER_ID, "ECHO-7 Route Prep", 64, 0xFF92F7A6, true));
        }

        @Override
        public List<IMapMarker> markers(Player player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return List.of();
            }
            TutorialPlayerData data = TutorialPlayerData.get(player);
            List<IMapMarker> markers = new ArrayList<>();
            if (data.hasProgress(id("used_scanner")) && !data.hasProgress(id("opened_holomap"))) {
                markers.add(marker(serverPlayer, "open_holomap", "Open HoloMap",
                        "Scanner use recorded. Review route and hazard overlays before committing.",
                        IMapMarker.MarkerKind.ROUTE));
            } else if (data.hasProgress(id("opened_holomap")) && !data.hasProgress(id("reviewed_route"))) {
                markers.add(marker(serverPlayer, "review_route", "Review Route Prep",
                        "Map opened. Confirm route supplies, hazards, and recovery plan.",
                        IMapMarker.MarkerKind.ROUTE));
            }
            if (!data.lastHazardIds().isEmpty()) {
                markers.add(marker(serverPlayer, "hazard_prep", "Prepare Hazard Protection",
                        "Recent hazard context: " + String.join(", ", data.lastHazardIds()),
                        IMapMarker.MarkerKind.HAZARD));
            }
            return List.copyOf(markers);
        }

        private static EchoMapMarker marker(ServerPlayer player, String path, String title, String summary,
                IMapMarker.MarkerKind kind) {
            return new EchoMapMarker(
                    id("marker/" + path + "/" + player.getUUID().toString().replace("-", "_")),
                    LAYER_ID,
                    PROVIDER_ID,
                    kind,
                    IMapMarker.MarkerState.DISCOVERED,
                    title,
                    summary,
                    player.level().dimension(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    0.0F,
                    null,
                    null,
                    -1,
                    true);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTutorialCore.MODID, path);
    }
}
