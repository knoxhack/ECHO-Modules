package com.knoxhack.echoarmory.integration;

import com.knoxhack.echoarmory.EchoArmory;
import net.minecraft.resources.Identifier;

public final class ArmoryTerminalIds {
   public static final Identifier ARMORY_TAB = id("armory");
   public static final Identifier SCAN_ACTION = id("scan");
   public static final Identifier EQUIP_ACTION = id("equip_loadout");
   public static final Identifier INSTALL_ACTION = id("install_module");
   public static final Identifier STANCE_ACTION = id("swap_stance");
   public static final Identifier RECHARGE_ACTION = id("recharge_core");
   public static final Identifier PREVIEW_ACTION = id("preview_readiness");
   public static final Identifier LOGISTICS_ACTION = id("request_logistics");
   public static final Identifier CHAPTER = id("chapter/armory");

   private ArmoryTerminalIds() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
   }
}
