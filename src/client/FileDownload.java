package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Thread for downloading file by client
 */
public class FileDownload implements Runnable {
    String fileName;
    InetSocketAddress inetSocketAddress;
    SocketChannel channel;
    String userName;

    public FileDownload(String fileName, InetSocketAddress inetSocketAddress, String name) {
        this.fileName = fileName;
        this.inetSocketAddress = inetSocketAddress;
        this.userName = name;
    }

    @Override
    public void run() {
        System.out.println("Start downloading file " + fileName);
        try {
            channel = SocketChannel.open(inetSocketAddress);
            send(fileName.length(), fileName.getBytes());

            int BUFFER_SIZE = 1024;
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    channel.read(buffer);
                    buffer.position(0);
                    int success = buffer.getInt();
                    if (success != 1) {
                        System.out.println("rec mes");
                        System.out.println(new String(buffer.array(), StandardCharsets.UTF_8));
                        break;
                    } else {
                        int size = buffer.getInt();
                        if (size > 0) {
                            try {
                                byte[] input = new byte[1024 - 8];
                                byte[] input1 = new byte[size];

                                byte[] full_input = new byte[size];
                                buffer.get(input);

                                if (size > 1024) {
                                    ByteBuffer buffer1 = ByteBuffer.allocate(size);
                                    channel.read(buffer1);

                                    while (buffer1.position() < (buffer1.limit() - 1024)) {
                                        channel.read(buffer1);
                                    }

                                    buffer1.position(0);
                                    buffer1.get(input1);

                                    System.arraycopy(input, 0, full_input, 0, input.length);
                                    System.arraycopy(input1, 0, full_input, input.length, size - input.length);

                                } else {
                                    System.arraycopy(input, 0, full_input, 0, size);
                                }

                                if (!Files.exists(Paths.get(userName + "/"))) {
                                    Files.createDirectory(Paths.get(userName + "/"));
                                }
                                Files.write(Paths.get(userName + "/" + fileName), full_input);
                                System.out.println("File " + fileName + " was downloaded succesfully");
                                break;

                            } catch (IOException e) {
                            } catch (Exception e) {
                            }
                        }
                    }

                } catch (IOException e) {
                    System.out.println("e: " + e);
                    Thread.currentThread().interrupt();
                    return;
                }

            }


        } catch (IOException e) {
            System.out.println(e);
        }


        Thread.currentThread().interrupt();
    }

    /**
     * Send request for file downloading to server
     *
     * @param length length of file name
     * @param data   file name
     */
    public void send(int length, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(4);
        buffer.putInt(length);
        buffer.put(data);
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.getMessage();
        }
        buffer.clear();

    }
}
