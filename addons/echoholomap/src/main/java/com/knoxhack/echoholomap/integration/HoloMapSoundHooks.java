package com.knoxhack.echoholomap.integration;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoholomap.HoloMapIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class HoloMapSoundHooks {
    public static final Identifier OPEN = HoloMapIds.id("sound/ui/open");
    public static final Identifier CLOSE = HoloMapIds.id("sound/ui/close");
    public static final Identifier WAYPOINT_CREATE = HoloMapIds.id("sound/ui/waypoint_create");
    public static final Identifier WAYPOINT_DELETE = HoloMapIds.id("sound/ui/waypoint_delete");
    public static final Identifier HAZARD_OVERLAY = HoloMapIds.id("sound/ui/hazard_overlay");
    public static final Identifier ROUTE_SYNC = HoloMapIds.id("sound/ui/route_sync");

    private HoloMapSoundHooks() {
    }

    public static boolean play(Player player, Identifier eventId) {
        try {
            return player != null && eventId != null && EchoCoreServices.soundService().playEvent(player, eventId, 0.8F, 1.0F);
        } catch (RuntimeException | LinkageError exception) {
            return false;
        }
    }
}
