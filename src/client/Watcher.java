package client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Watcher implements Runnable {
    SocketChannel channel;

    public Watcher(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        int BUFFER_SIZE = 1024;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                channel.read(buffer);
                System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
                buffer.clear();
                Arrays.fill(buffer.array(), (byte) 0);

            } catch (IOException e) {
                Thread.currentThread().interrupt();
                return;
            }

        }
    }
}
