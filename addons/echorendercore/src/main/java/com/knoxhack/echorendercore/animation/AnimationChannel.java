package com.knoxhack.echorendercore.animation;

import java.util.Locale;

public enum AnimationChannel {
   POSITION_X,
   POSITION_Y,
   POSITION_Z,
   ROTATION_X,
   ROTATION_Y,
   ROTATION_Z,
   SCALE_X,
   SCALE_Y,
   SCALE_Z,
   VISIBILITY,
   ALPHA;

   public static AnimationChannel byName(String value) {
      if (value == null || value.isBlank()) {
         return POSITION_Y;
      }
      String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
      return switch (normalized) {
         case "X", "POS_X", "POSITIONX" -> POSITION_X;
         case "Y", "POS_Y", "POSITIONY" -> POSITION_Y;
         case "Z", "POS_Z", "POSITIONZ" -> POSITION_Z;
         case "ROT_X", "ROTATIONX", "PITCH" -> ROTATION_X;
         case "ROT_Y", "ROTATIONY", "YAW" -> ROTATION_Y;
         case "ROT_Z", "ROTATIONZ", "ROLL" -> ROTATION_Z;
         case "SCALEX" -> SCALE_X;
         case "SCALEY" -> SCALE_Y;
         case "SCALEZ" -> SCALE_Z;
         default -> AnimationChannel.valueOf(normalized);
      };
   }

   public boolean rotation() {
      return this == ROTATION_X || this == ROTATION_Y || this == ROTATION_Z;
   }
}
