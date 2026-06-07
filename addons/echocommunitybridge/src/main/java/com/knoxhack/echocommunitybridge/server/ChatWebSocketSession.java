package com.knoxhack.echocommunitybridge.server;

import com.knoxhack.echocommunitybridge.EchoCommunityBridge;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChatWebSocketSession {
    private final OutputStream output;
    private final AtomicBoolean open = new AtomicBoolean(true);

    public ChatWebSocketSession(OutputStream output) {
        this.output = output;
    }

    public boolean isOpen() {
        return open.get();
    }

    public boolean sendText(String text) {
        return sendFrame(0x81, text.getBytes(StandardCharsets.UTF_8));
    }

    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        sendFrame(0x88, new byte[0]);
        try {
            output.close();
        } catch (IOException ignored) {
            // Socket already closed by the client.
        }
    }

    public void readUntilClosed(InputStream input, Runnable onClose) {
        try {
            while (open.get()) {
                int first = input.read();
                if (first < 0) {
                    break;
                }
                int second = input.read();
                if (second < 0) {
                    break;
                }
                int opcode = first & 0x0F;
                boolean masked = (second & 0x80) != 0;
                long length = second & 0x7FL;
                if (length == 126L) {
                    length = ((long) input.read() << 8) | input.read();
                } else if (length == 127L) {
                    length = 0L;
                    for (int index = 0; index < 8; index++) {
                        int part = input.read();
                        if (part < 0) {
                            return;
                        }
                        length = (length << 8) | part;
                    }
                }
                if (length > 65_536L) {
                    break;
                }
                byte[] mask = new byte[4];
                if (masked) {
                    readFully(input, mask);
                }
                byte[] payload = new byte[(int) length];
                readFully(input, payload);
                if (masked) {
                    for (int index = 0; index < payload.length; index++) {
                        payload[index] = (byte) (payload[index] ^ mask[index % 4]);
                    }
                }
                if (opcode == 0x8) {
                    break;
                }
                if (opcode == 0x9) {
                    sendFrame(0x8A, payload);
                }
            }
        } catch (IOException ex) {
            EchoCommunityBridge.LOGGER.debug("ECHO chat WebSocket closed.");
        } finally {
            open.set(false);
            onClose.run();
            try {
                input.close();
            } catch (IOException ignored) {
                // Already closed.
            }
        }
    }

    private boolean sendFrame(int opcode, byte[] payload) {
        if (!open.get()) {
            return false;
        }
        try {
            byte[] header = header(opcode, payload.length);
            synchronized (output) {
                output.write(header);
                output.write(payload);
                output.flush();
            }
            return true;
        } catch (IOException ex) {
            open.set(false);
            return false;
        }
    }

    private static byte[] header(int opcode, int length) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        header.write(opcode);
        if (length <= 125) {
            header.write(length);
        } else if (length <= 65_535) {
            header.write(126);
            header.write((length >>> 8) & 0xFF);
            header.write(length & 0xFF);
        } else {
            header.write(127);
            for (int shift = 56; shift >= 0; shift -= 8) {
                header.write((length >>> shift) & 0xFF);
            }
        }
        return header.toByteArray();
    }

    private static void readFully(InputStream input, byte[] target) throws IOException {
        int offset = 0;
        while (offset < target.length) {
            int read = input.read(target, offset, target.length - offset);
            if (read < 0) {
                throw new IOException("WebSocket frame ended early.");
            }
            offset += read;
        }
    }
}
