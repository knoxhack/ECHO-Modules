package com.knoxhack.echoindex.client;

import java.util.List;

public final class IndexHistoryState {
    public List<String> recent() {
        return IndexHistoryStore.recent();
    }

    public void add(String kind, String id, String label, String icon) {
        IndexHistoryStore.add(kind, id, label, icon);
    }

    public void clear() {
        IndexHistoryStore.clear();
    }
}
