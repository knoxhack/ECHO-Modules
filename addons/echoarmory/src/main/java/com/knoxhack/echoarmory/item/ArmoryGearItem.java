package com.knoxhack.echoarmory.item;

public interface ArmoryGearItem {
   String gearId();

   ArmoryGearKind gearKind();

   enum ArmoryGearKind {
      WEAPON,
      ARMOR,
      MODULE,
      AMMO,
      UTILITY
   }
}
