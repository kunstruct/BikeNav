package com.kunstruct.bikenavhelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Byte-transparent, bounded-memory relay. First EOF/error closes both directions. */
public final class RelayPipe implements AutoCloseable {
    public static final byte[] GREETING = "BKNAVR1\n".getBytes(StandardCharsets.US_ASCII);
    private final Socket board, phone;
    private final AtomicBoolean closed = new AtomicBoolean();
    public final AtomicLong toBoard = new AtomicLong(), toPhone = new AtomicLong();
    private volatile IOException failure;

    public RelayPipe(Socket board, Socket phone) { this.board = board; this.phone = phone; }

    public void run() throws IOException, InterruptedException {
        board.getOutputStream().write(GREETING);
        Thread uplink = new Thread(() -> copy(phone, board, toBoard), "AA-to-board");
        uplink.start();
        try { copy(board, phone, toPhone); }
        finally { close(); uplink.join(3000); }
        if (uplink.isAlive()) throw new IOException("Relay worker failed to stop");
        if (failure != null) throw failure;
    }

    private void copy(Socket from, Socket to, AtomicLong count) {
        byte[] buffer = new byte[32768];
        try {
            InputStream input = from.getInputStream(); OutputStream output = to.getOutputStream();
            while (!closed.get()) {
                int n = input.read(buffer);
                if (n < 0) break;
                output.write(buffer, 0, n);
                count.addAndGet(n);
            }
        } catch (IOException e) {
            if (!closed.get()) failure = e;
        } finally { close(); }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { board.close(); } catch (IOException ignored) { }
        try { phone.close(); } catch (IOException ignored) { }
    }
}
