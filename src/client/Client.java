package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Class describes main client logic
 */
public class Client {
    private static final int PORT = 1234;
    private SocketChannel channel;
    private String name;

    public static void main(String[] args) {
        new Client().run();
    }

    public void run() {
        System.out.println("Привет, пользователь! Представься, пожалуйста.");
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true)
                if (bufferedReader.ready()) {
                    name = bufferedReader.readLine();
                    System.out.println("Привет, " + name + "!");
                    break;
                }
            try {

                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), PORT);
                channel = SocketChannel.open(inetSocketAddress);
                Thread watcher = new Thread(new Watcher(channel));
                watcher.start();
                introduce();

                while (true) {
                    try {
                        if (bufferedReader.ready()) {
                            String command = bufferedReader.readLine();
                            String commandType = command.split(" ")[0];
                            switch (commandType) {
                                case "quit":
                                    quit();
                                    watcher.interrupt();
                                    return;
                                case "online":
                                    getOnline();
                                    break;
                                case "messages":
                                    getMessages();
                                    break;
                                case "upload":
                                    String fileName = command.split(" ")[1];
                                    uploadFile(fileName, inetSocketAddress, name);
                                    break;
                                case "download":
                                    String fileNameDownload = command.split(" ")[1];
                                    downloadFile(fileNameDownload, inetSocketAddress, name);
                                default:
                                    String message = name + ": " + command;
                                    if (!message.equals(name + ": ")) {
                                        sendMessage(message);
                                    }
                                    break;

                            }

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Missed some parameters.");
                    }
                }

            } catch (UnknownHostException e) {
                System.out.println("Хост не найден");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Start file download process
     *
     * @param fileName          name of file to download
     * @param inetSocketAddress server address
     * @param name              name of user, who want to download file
     */
    public void downloadFile(String fileName, InetSocketAddress inetSocketAddress, String name) {
        Thread fileDownloadThread = new Thread(new FileDownload(fileName, inetSocketAddress, name));
        fileDownloadThread.start();
    }

    /**
     * Start file upload process
     *
     * @param fileName          name of file to upload
     * @param inetSocketAddress server address
     * @param name              name of user, who want to upload file
     */
    public void uploadFile(String fileName, InetSocketAddress inetSocketAddress, String name) {
        Thread fileUploadThread = new Thread(new FileUpload(fileName, inetSocketAddress, name));
        fileUploadThread.start();
    }

    /**
     * Get all messages
     */
    public void getMessages() {
        send(7);
    }

    /**
     * Send message to server
     *
     * @param message message
     */
    public void sendMessage(String message) {
        send(3, message.length(), message.getBytes());
    }

    /**
     * Inform serer about client's existence
     */
    public void introduce() {
        send(1, name.length(), name.getBytes());
    }

    /**
     * Stop session
     */
    public void quit() {
        send(5);
    }

    /**
     * Get information about online users
     */
    private void getOnline() {
        send(6);
    }

    /**
     * Send client command to server
     *
     * @param command command
     */
    public void send(int command) {
        send(command, 0, new byte[0]);
    }

    /**
     * Send command with parameters to server
     *
     * @param command command
     * @param length  data length
     * @param data    data
     */
    public void send(int command, int length, byte[] data) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(command);
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
