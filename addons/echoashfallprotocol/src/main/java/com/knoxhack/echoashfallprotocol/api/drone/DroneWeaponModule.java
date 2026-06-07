package com.knoxhack.echoashfallprotocol.api.drone;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

public interface DroneWeaponModule {
    boolean canAttack(Mob target);

    double getRange();

    void tickAttack(ServerLevel level, Mob target);

    String getDisplayName();
}
