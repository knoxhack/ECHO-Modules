package com.knoxhack.echocore.api.index;

import java.util.Collection;
import java.util.Optional;

public interface IIndexRegistry {
    void register(IndexEntry entry);

    Optional<IndexEntry> find(String id);

    Collection<IndexEntry> all();
}
