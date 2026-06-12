package com.echoplatform.echocore.api.index;

import net.minecraft.world.entity.player.Player;

public record IndexBuildContext(Player player, boolean client, String reason, String profile) {
    public static final IndexBuildContext EMPTY = new IndexBuildContext(null, false, "", "default");

    public IndexBuildContext {
        reason = reason == null ? "" : reason;
        profile = profile == null || profile.isBlank() ? "default" : profile;
    }

    public IndexBuildContext(String profile) {
        this(null, false, "", profile);
    }

    public static IndexBuildContext of(Player player, boolean client, String reason) {
        return new IndexBuildContext(player, client, reason, player == null ? "server" : player.getUUID().toString());
    }
}
