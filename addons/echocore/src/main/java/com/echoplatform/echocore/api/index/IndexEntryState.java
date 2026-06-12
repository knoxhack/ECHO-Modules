package com.echoplatform.echocore.api.index;

public enum IndexEntryState {
    VISIBLE,
    DISCOVERED,
    COMPLETED,
    ARCHIVED,
    CORRUPTED,
    HIDDEN,
    LOCKED;

    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
