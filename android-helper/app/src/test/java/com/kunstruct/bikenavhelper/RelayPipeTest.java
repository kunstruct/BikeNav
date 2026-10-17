package com.kunstruct.bikenavhelper;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.*;

/** Runs on a desktop JVM; exercises real sockets and production relay code. */
public final class RelayPipeTest {
    private static final ExecutorService workers = Executors.newCachedThreadPool();
    private static Socket[] pair() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Socket client = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
            Socket peer = listener.accept(); peer.setSoTimeout(5000); client.setSoTimeout(5000);
            return new Socket[]{client, peer};
        }
    }
    private static void equal(byte[] expected, byte[] got) {
        if (!Arrays.equals(expected, got)) throw new AssertionError("Byte stream changed or truncated");
    }
    private static void duplex() throws Exception {
        Socket[] board = pair(), phone = pair();
        RelayPipe relay = new RelayPipe(board[0], phone[0]);
        Future<?> task = workers.submit(() -> { try { relay.run(); } catch (Exception e) { throw new RuntimeException(e); } });
        byte[] video = new byte[2 * 1024 * 1024 + 17], control = new byte[131079];
        new Random(12).nextBytes(video); new Random(13).nextBytes(control);
        equal(RelayPipe.GREETING, board[1].getInputStream().readNBytes(RelayPipe.GREETING.length));
        Future<?> videoWriter = workers.submit(() -> { try { phone[1].getOutputStream().write(video); } catch (Exception e) { throw new RuntimeException(e); } });
        Future<?> controlWriter = workers.submit(() -> { try {
            // Fragment reads/writes at arbitrary boundaries, like TCP in practice.
            for (int offset = 0; offset < control.length; offset += 113)
                board[1].getOutputStream().write(control, offset, Math.min(113, control.length - offset));
        } catch (Exception e) { throw new RuntimeException(e); } });
        Future<byte[]> receivedVideo = workers.submit(() -> board[1].getInputStream().readNBytes(video.length));
        equal(control, phone[1].getInputStream().readNBytes(control.length));
        equal(video, receivedVideo.get(10, TimeUnit.SECONDS));
        videoWriter.get(5, TimeUnit.SECONDS); controlWriter.get(5, TimeUnit.SECONDS);
        if (relay.toBoard.get() != video.length || relay.toPhone.get() != control.length) throw new AssertionError("Bad byte counters");
        phone[1].shutdownOutput(); task.get(5, TimeUnit.SECONDS);
        if (board[1].getInputStream().read() != -1) throw new AssertionError("EOF did not close board");
        board[1].close(); phone[1].close();
    }
    private static void cancelStalledWriter() throws Exception {
        Socket[] board = pair(), phone = pair();
        board[0].setSendBufferSize(1024);
        RelayPipe relay = new RelayPipe(board[0], phone[0]);
        Future<?> task = workers.submit(() -> { try { relay.run(); } catch (Exception e) { throw new RuntimeException(e); } });
        board[1].getInputStream().readNBytes(RelayPipe.GREETING.length);
        Future<?> sender = workers.submit(() -> { try { phone[1].getOutputStream().write(new byte[16 * 1024 * 1024]); } catch (Exception ignored) { } });
        // Deliberately never read at board peer. Cancelling must unblock both
        // a blocked write and the opposite blocked read, without leaking threads.
        Thread.sleep(150); relay.close(); task.get(5, TimeUnit.SECONDS);
        phone[1].close(); board[1].close(); sender.get(5, TimeUnit.SECONDS);
    }
    private static void boardDisconnect() throws Exception {
        Socket[] board = pair(), phone = pair();
        RelayPipe relay = new RelayPipe(board[0], phone[0]);
        Future<?> task = workers.submit(() -> { try { relay.run(); } catch (Exception ignored) { } });
        board[1].getInputStream().readNBytes(RelayPipe.GREETING.length);
        board[1].close(); task.get(5, TimeUnit.SECONDS);
        if (phone[1].getInputStream().read() != -1) throw new AssertionError("Board loss did not close headunit");
        phone[1].close();
    }
    public static void main(String[] args) throws Exception {
        try { duplex(); cancelStalledWriter(); boardDisconnect();
            System.out.println("PASS: full-duplex binary relay, fragmentation, counters, EOF, board loss, cancellation under backpressure");
        } finally { workers.shutdownNow(); }
    }
}
