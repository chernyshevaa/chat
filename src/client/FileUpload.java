package client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Thread for uploading file by client
 */
public class FileUpload implements Runnable {
    String fileName;
    InetSocketAddress inetSocketAddress;
    SocketChannel channel;
    String userName;

    public FileUpload(String fileName, InetSocketAddress inetSocketAddress, String name) {
        this.fileName = fileName;
        this.inetSocketAddress = inetSocketAddress;
        this.userName = name;

    }

    @Override
    public void run() {
        System.out.println("Start uploading file " + fileName);
        Path file = Paths.get(userName + "/" + fileName);
        if (Files.exists(file)) {
            try (InputStream inputStream = Files.newInputStream(file)) {
                byte[] fileBytes = inputStream.readAllBytes();
                System.out.println("size of file is " + fileBytes.length + " bytes");
                channel = SocketChannel.open(inetSocketAddress);
                byte[] output = new byte[fileBytes.length + 4 + fileName.length() + 4];
                System.out.println(fileName.length());
                System.arraycopy(ByteBuffer.allocate(4).putInt(fileName.length()).array(), 0, output, 0, 4);
                System.arraycopy(fileName.getBytes(), 0, output, 4, fileName.length());
                System.arraycopy(ByteBuffer.allocate(4).putInt(fileBytes.length).array(), 0, output, 4 + fileName.length(), 4);
                System.arraycopy(fileBytes, 0, output, 4 + fileName.length() + 4, fileBytes.length);
                send(output.length, output);
                //                send(2, fileBytes.length, fileBytes);
                System.out.println("File " + fileName + " is uploaded succesfully");
            } catch (IOException e) {
                System.out.println("Some problems while uploading file");
                Thread.currentThread().interrupt();
            }
        } else {
            System.out.println("Could not find file " + fileName);
        }
        Thread.currentThread().interrupt();

    }

    /**
     * Send request to upload file to server
     *
     * @param length file length
     * @param data   file
     */
    public void send(int length, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * length);
        buffer.putInt(2);
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
