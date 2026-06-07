package com.knoxhack.echologisticsnetwork;

import com.knoxhack.echocore.api.config.EchoConfigCategory;
import com.knoxhack.echocore.api.config.EchoConfigEntry;
import com.knoxhack.echocore.api.config.EchoConfigModule;
import com.knoxhack.echocore.api.config.EchoConfigProvider;
import com.knoxhack.echocore.api.config.EchoConfigRegistry;
import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echocore.api.config.EchoNativeConfigSpec;
import java.util.List;

public final class Config {
   private static final EchoNativeConfigSpec.Builder BUILDER = new EchoNativeConfigSpec.Builder();

   public static final EchoNativeConfigSpec.IntValue SNAPSHOT_CACHE_TICKS;
   public static final EchoNativeConfigSpec.IntValue BACKGROUND_REFRESH_TICKS;
   public static final EchoNativeConfigSpec SPEC;

   static {
      BUILDER.push("performance");
      SNAPSHOT_CACHE_TICKS = BUILDER
         .comment("Server ticks that Logistics terminal snapshots may reuse expensive network scan results.")
         .defineInRange("snapshotCacheTicks", 20, 0, 200);
      BACKGROUND_REFRESH_TICKS = BUILDER
         .comment("Server ticks between staggered passive Logistics block snapshot refreshes.")
         .defineInRange("backgroundRefreshTicks", 100, 20, 1200);
      BUILDER.pop();
      SPEC = BUILDER.build();
   }

   private Config() {
   }

   public static int snapshotCacheTicks() {
      return Math.max(0, SNAPSHOT_CACHE_TICKS.get());
   }

   public static int backgroundRefreshTicks() {
      return Math.max(20, BACKGROUND_REFRESH_TICKS.get());
   }

   public static void registerEchoConfig() {
      EchoConfigRegistry.register(EchoConfigProvider.of(EchoLogisticsNetwork.MODID, () -> new EchoConfigModule(
         EchoLogisticsNetwork.MODID,
         "Logistics Network",
         List.of(new EchoConfigCategory("performance", "Performance", List.of(
            EchoConfigEntry.intSpec("snapshot_cache_ticks", "Snapshot Cache Ticks",
               "Server ticks that Logistics terminal snapshots may reuse expensive network scan results.",
               EchoConfigSide.COMMON, SNAPSHOT_CACHE_TICKS, 0, 200, true, false, false),
            EchoConfigEntry.intSpec("background_refresh_ticks", "Background Refresh Ticks",
               "Server ticks between staggered passive Logistics block snapshot refreshes.",
               EchoConfigSide.COMMON, BACKGROUND_REFRESH_TICKS, 20, 1200, true, false, false)))))));
   }
}
