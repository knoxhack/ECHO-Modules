package com.knoxhack.echotutorialcore.api.trigger;

public enum TutorialTriggerType {
    PLACE_BLOCK,
    INTERACT_BLOCK,
    CRAFT_ITEM,
    OBTAIN_ITEM,
    ENTER_REGION,
    CUSTOM,
    FIRST_TIME,
    MISSION_COMPLETE,
    DEATH;

    public String getTranslationKey() {
        return "echotutorialcore.trigger_type." + name().toLowerCase();
    }
}
