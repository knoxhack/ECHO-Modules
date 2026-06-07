package com.knoxhack.echostationfall.progression;
import java.util.Locale;
public enum StationPowerState { OFFLINE("Offline",4,true,false), EMERGENCY("Emergency",3,true,false), PARTIAL("Partial",2,true,true), STABLE("Stable",0,false,true), OVERLOADED("Overloaded",5,true,true);
    private final String displayName; private final int oxygenDrain; private final boolean hostile; private final boolean opensDoors;
    StationPowerState(String d,int o,boolean h,boolean doors){displayName=d;oxygenDrain=o;hostile=h;opensDoors=doors;} public String displayName(){return displayName;} public int oxygenDrain(){return oxygenDrain;} public boolean hostile(){return hostile;} public boolean opensDoors(){return opensDoors;} public boolean stableOrBetter(){return this==STABLE||this==OVERLOADED;}
    public static StationPowerState byName(String n){if(n==null||n.isBlank())return EMERGENCY; try{return valueOf(n.trim().toUpperCase(Locale.ROOT));}catch(Exception e){return EMERGENCY;}}
}
