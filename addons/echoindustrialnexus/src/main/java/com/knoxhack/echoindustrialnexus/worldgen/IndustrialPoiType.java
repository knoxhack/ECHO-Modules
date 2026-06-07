package com.knoxhack.echoindustrialnexus.worldgen;

public enum IndustrialPoiType {
   ABANDONED_THERMAL_PLANT("abandoned_thermal_plant", 41001),
   RUSTED_FACTORY_COMPLEX("rusted_factory_complex", 41002),
   GEOTHERMAL_DRILL_SITE("geothermal_drill_site", 41003),
   REACTOR_COOLING_STATION("reactor_cooling_station", 41004),
   NEXUS_HEAT_EXCHANGER_RUINS("nexus_heat_exchanger_ruins", 41005);

   private final String id;
   private final int salt;

   IndustrialPoiType(String id, int salt) {
      this.id = id;
      this.salt = salt;
   }

   public String id() {
      return this.id;
   }

   public int salt() {
      return this.salt;
   }
}
