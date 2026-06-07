package com.knoxhack.echopresencelink.discord;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DiscordIpcTransport implements PresenceTransport {
    private static final Gson GSON = new Gson();
    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int MAX_FRAME_BYTES = 64 * 1024;
    private static final long RESPONSE_TIMEOUT_MILLIS = 2500L;
    private static final ExecutorService READ_EXECUTOR = Executors.newCachedThreadPool(new DaemonThreadFactory());

    private ByteChannel channel;
    private Closeable closeable;
    private String connectedApplicationId = "";
    private String statusLine = "Discord IPC not connected.";
    private String endpoint = "";
    private String lastResponse = "";

    @Override
    public synchronized void setActivity(String applicationId, JsonObject activity) throws IOException {
        ensureConnected(applicationId);
        PresenceActivityPayload.CommandPayload command = PresenceActivityPayload.setActivity(activity);
        send(OP_FRAME, command.json());
        awaitCommandResponse(command.nonce());
        statusLine = "Discord IPC SET_ACTIVITY acknowledged.";
    }

    @Override
    public synchronized void clearActivity(String applicationId) throws IOException {
        if (applicationId == null || applicationId.isBlank()) {
            close();
            return;
        }
        ensureConnected(applicationId);
        PresenceActivityPayload.CommandPayload command = PresenceActivityPayload.clearActivity();
        send(OP_FRAME, command.json());
        awaitCommandResponse(command.nonce());
        statusLine = "Discord IPC activity cleared.";
    }

    @Override
    public synchronized boolean connected() {
        return channel != null;
    }

    @Override
    public synchronized String statusLine() {
        return statusLine;
    }

    @Override
    public synchronized String endpoint() {
        return endpoint;
    }

    @Override
    public synchronized String lastResponse() {
        return lastResponse;
    }

    @Override
    public synchronized void close() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // Ignore close failures; the next update will reconnect.
            }
        }
        closeable = null;
        channel = null;
        connectedApplicationId = "";
        statusLine = "Discord IPC disconnected.";
    }

    private void ensureConnected(String applicationId) throws IOException {
        String safeApplicationId = applicationId == null ? "" : applicationId.strip();
        if (safeApplicationId.isBlank()) {
            throw new IOException("Discord application id is blank.");
        }
        if (channel != null && safeApplicationId.equals(connectedApplicationId)) {
            return;
        }
        close();
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            for (String candidate : ipcCandidates(index)) {
                try {
                    openEndpoint(candidate);
                    connectedApplicationId = safeApplicationId;
                    JsonObject handshake = new JsonObject();
                    handshake.addProperty("v", 1);
                    handshake.addProperty("client_id", safeApplicationId);
                    send(OP_HANDSHAKE, handshake);
                    awaitReady();
                    statusLine = "Discord IPC connected on " + endpoint + ".";
                    return;
                } catch (IOException exception) {
                    failures.add(endpointSummary(candidate) + ": " + exception.getMessage());
                    close();
                }
            }
        }
        throw new IOException("Discord desktop IPC is unavailable. " + String.join(" | ", failures));
    }

    private void openEndpoint(String candidate) throws IOException {
        endpoint = candidate;
        if (isWindowsEndpoint(candidate)) {
            RandomAccessFile file = new RandomAccessFile(candidate, "rw");
            channel = file.getChannel();
            closeable = file;
            return;
        }
        Path socket = Path.of(candidate);
        SocketChannel socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
        socketChannel.connect(UnixDomainSocketAddress.of(socket));
        channel = socketChannel;
        closeable = socketChannel;
    }

    private void send(int opcode, JsonObject payload) throws IOException {
        byte[] json = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + json.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(json.length);
        buffer.put(json);
        buffer.flip();
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private void awaitReady() throws IOException {
        IpcFrame frame = readFrame("DISCORD_RESPONSE_TIMEOUT");
        JsonObject payload = frame.payload();
        if (isError(payload)) {
            throw error(payload);
        }
        String event = string(payload, "evt");
        if (!"READY".equals(event)) {
            throw new PresenceIpcException("DISCORD_UNEXPECTED_RESPONSE",
                    "Discord IPC handshake returned " + summary(payload) + " instead of READY.",
                    frame.raw(), false);
        }
        statusLine = "Discord IPC READY received.";
    }

    private void awaitCommandResponse(String nonce) throws IOException {
        for (int attempt = 0; attempt < 6; attempt++) {
            IpcFrame frame = readFrame("DISCORD_RESPONSE_TIMEOUT");
            JsonObject payload = frame.payload();
            if (isError(payload)) {
                throw error(payload);
            }
            String responseNonce = string(payload, "nonce");
            String command = string(payload, "cmd");
            if (nonce.equals(responseNonce) || ("SET_ACTIVITY".equals(command) && responseNonce.isBlank())) {
                return;
            }
        }
        throw new PresenceIpcException("DISCORD_RESPONSE_TIMEOUT",
                "Discord IPC did not acknowledge SET_ACTIVITY before the response limit.",
                lastResponse, false);
    }

    private IpcFrame readFrame(String timeoutCode) throws IOException {
        byte[] header = readBytes(8, timeoutCode);
        ByteBuffer headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = headerBuffer.getInt();
        int length = headerBuffer.getInt();
        if (length < 0 || length > MAX_FRAME_BYTES) {
            throw new PresenceIpcException("DISCORD_BAD_FRAME",
                    "Discord IPC returned an invalid frame length " + length + ".",
                    lastResponse, false);
        }
        byte[] body = readBytes(length, timeoutCode);
        String raw = new String(body, StandardCharsets.UTF_8);
        lastResponse = compact(raw, 512);
        JsonObject payload;
        try {
            payload = JsonParser.parseString(raw).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new PresenceIpcException("DISCORD_BAD_JSON",
                    "Discord IPC returned malformed JSON.",
                    lastResponse, false);
        }
        return new IpcFrame(opcode, payload, raw);
    }

    private byte[] readBytes(int length, String timeoutCode) throws IOException {
        ByteChannel current = channel;
        if (current == null) {
            throw new EOFException("Discord IPC channel is closed.");
        }
        Callable<byte[]> readTask = () -> {
            ByteBuffer buffer = ByteBuffer.allocate(length);
            while (buffer.hasRemaining()) {
                int read = current.read(buffer);
                if (read < 0) {
                    throw new EOFException("Discord IPC closed while waiting for a response.");
                }
            }
            return buffer.array();
        };
        Future<byte[]> future = READ_EXECUTOR.submit(readTask);
        try {
            return future.get(RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            close();
            throw new PresenceIpcException(timeoutCode,
                    "Discord IPC response timed out after " + RESPONSE_TIMEOUT_MILLIS + "ms.",
                    lastResponse, false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Discord IPC response.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Discord IPC response read failed.", cause);
        }
    }

    private static PresenceIpcException error(JsonObject payload) {
        JsonObject data = payload.has("data") && payload.get("data").isJsonObject()
                ? payload.getAsJsonObject("data")
                : new JsonObject();
        String code = string(data, "code");
        if (code.isBlank()) {
            code = "DISCORD_ERROR";
        }
        String message = string(data, "message");
        if (message.isBlank()) {
            message = string(payload, "message");
        }
        if (message.isBlank()) {
            message = "Discord returned an RPC error.";
        }
        return new PresenceIpcException(code, message, compact(GSON.toJson(payload), 512), true);
    }

    private static boolean isError(JsonObject payload) {
        return "ERROR".equals(string(payload, "evt"));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static List<String> ipcCandidates(int index) throws IOException {
        if (isWindows()) {
            return List.of("\\\\?\\pipe\\discord-ipc-" + index, "\\\\.\\pipe\\discord-ipc-" + index);
        }
        String name = "discord-ipc-" + index;
        List<String> candidates = new ArrayList<>();
        for (String dir : unixSocketDirs()) {
            if (dir == null || dir.isBlank()) {
                continue;
            }
            Path path = Path.of(dir, name);
            if (Files.exists(path)) {
                candidates.add(path.toString());
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(Path.of(System.getProperty("java.io.tmpdir", "/tmp"), name).toString());
        }
        return candidates;
    }

    private static List<String> unixSocketDirs() {
        return List.of(
                System.getenv("XDG_RUNTIME_DIR"),
                System.getenv("TMPDIR"),
                System.getenv("TMP"),
                System.getenv("TEMP"),
                System.getProperty("java.io.tmpdir", "/tmp"),
                "/tmp");
    }

    private static boolean isWindowsEndpoint(String candidate) {
        return candidate.startsWith("\\\\");
    }

    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private static String summary(JsonObject object) {
        String command = string(object, "cmd");
        String event = string(object, "evt");
        if (command.isBlank() && event.isBlank()) {
            return compact(GSON.toJson(object), 120);
        }
        return "cmd=" + (command.isBlank() ? "none" : command)
                + ", evt=" + (event.isBlank() ? "none" : event);
    }

    private static String endpointSummary(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "unknown endpoint";
        }
        return candidate.replace("\\\\?\\pipe\\", "\\\\?\\pipe\\")
                .replace("\\\\.\\pipe\\", "\\\\.\\pipe\\");
    }

    private static String compact(String value, int limit) {
        String cleaned = value == null ? "" : value.replaceAll("\\s+", " ").strip();
        if (cleaned.length() > limit) {
            return cleaned.substring(0, Math.max(0, limit - 3)) + "...";
        }
        return cleaned;
    }

    private record IpcFrame(int opcode, JsonObject payload, String raw) {
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private int nextId;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ECHO Presence Link Discord IPC Reader-" + nextId++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
