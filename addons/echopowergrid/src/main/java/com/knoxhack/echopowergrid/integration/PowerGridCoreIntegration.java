package com.knoxhack.echopowergrid.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import net.minecraft.world.entity.player.Player;

public final class PowerGridCoreIntegration {
    public static final String CHAPTER_ID = "power_grid";

    private PowerGridCoreIntegration() {}

    public static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return EchoPowerGrid.MODID;
            }

            @Override
            public String displayName() {
                return "ECHO PowerGrid";
            }

            @Override
            public String summary() {
                return "Shared Echo Power generation, storage, transfer, diagnostics, brownout, and grid infrastructure.";
            }

            @Override
            public String statusLine(Player player) {
                return "PowerGrid: online";
            }
        });
    }
}
