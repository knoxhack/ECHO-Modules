package com.knoxhack.echoblackboxprotocol.progression;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum BlackboxEnding implements StringRepresentable {
   NONE("none", "No Ending", "The Nexus Core Chamber is unresolved."),
   RESTORE("restore", "Restore", "The world is damaged, but no longer lost."),
   CONTROL("control", "Control", "You did not save the world. You placed your hand on its throat."),
   DESTROY("destroy", "Destroy", "Silence is not peace. But it is finally silence."),
   MERGE("merge", "Merge", "User identity no longer singular. Welcome back.");

   public static final Codec<BlackboxEnding> CODEC = StringRepresentable.fromEnum(BlackboxEnding::values);
   private final String serializedName;
   private final String displayName;
   private final String finalLine;

   private BlackboxEnding(String serializedName, String displayName, String finalLine) {
      this.serializedName = serializedName;
      this.displayName = displayName;
      this.finalLine = finalLine;
   }

   public String displayName() {
      return this.displayName;
   }

   public String finalLine() {
      return this.finalLine;
   }

   public String getSerializedName() {
      return this.serializedName;
   }

   public static BlackboxEnding byName(String name) {
      for (BlackboxEnding ending : values()) {
         if (ending.serializedName.equalsIgnoreCase(name) || ending.name().equalsIgnoreCase(name)) {
            return ending;
         }
      }

      return NONE;
   }
}
