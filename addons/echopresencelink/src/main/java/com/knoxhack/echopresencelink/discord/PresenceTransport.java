package com.knoxhack.echopresencelink.discord;

import com.google.gson.JsonObject;
import java.io.IOException;

public interface PresenceTransport extends AutoCloseable {
    void setActivity(String applicationId, JsonObject activity) throws IOException;

    void clearActivity(String applicationId) throws IOException;

    boolean connected();

    String statusLine();

    default String endpoint() {
        return "";
    }

    default String lastResponse() {
        return "";
    }

    @Override
    void close();
}
