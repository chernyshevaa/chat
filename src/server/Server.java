package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Server of chat
 */
public class Server {

    private static final int PORT = 1234;
    private Selector selector;
    private ArrayList<String> messages;
    private HashMap<SocketAddress, String> connectedClients;

    private static String SERVER_PACKAGE = "server";


    public static void main(String[] args) {
        new Server().run();
    }

    public void run() {
        try {
            selector = Selector.open();
            messages = new ArrayList<>();
            connectedClients = new HashMap<>();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            InetSocketAddress hostAddress = new InetSocketAddress(InetAddress.getLocalHost(), PORT);
            serverChannel.bind(hostAddress);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            while (true) {
                int readyCount = selector.select();
                if (readyCount == 0) continue;
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                Iterator iterator = readyKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (!key.isValid()) continue;
                    if (key.isAcceptable()) accept(key);
                    if (key.isReadable()) read(key);
                    // if(key.isWritable()) readers.add(key);

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        Socket socket = client.socket();
        SocketAddress socketAddress = socket.getRemoteSocketAddress();
        System.out.println("Подключено к " + socketAddress);
        client.register(selector, SelectionKey.OP_READ);
    }

    /**
     * Функция для обработки команды, пришедшей от пользователя.
     * Коды:
     * 1 - передача информации о подключившемся пользователе
     * 2 - загрузка файлов
     * 3 - отправка сообщений
     * 4 - выгрузка файлов
     * 5 - завершение работы
     * 6 - получение информации о клиентах онлайн
     * 7 - отправка 100 последних сообщений
     */
    private void read(SelectionKey key) throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        int BUFFER_SIZE = 1024;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        client.read(buffer);
//        System.out.println("buffer position " + buffer.position());
        buffer.position(0);
        int command = buffer.getInt();
//        System.out.println("Команда " + command);
        switch (command) {
            case 1:
                processIntroduction(buffer, key);
                break;
            case 2:
                getFile(buffer, key);
                break;
            case 3:
                getMessage(buffer, key);
                break;
            case 4:
                sendFile(buffer, key);
                break;
            case 5:
                processQuit(key);
                break;
            case 6:
                getOnlineClients(key);
                break;
            case 7:
                sendMessageHistory(key);
        }
        buffer.clear();
    }

    /**
     * Get file from user
     * Method read file, which is uploaded by user and writes it to local server package.
     *
     * @param buffer command info from user
     * @param key    key for client chanel identification
     */
    private void getFile(ByteBuffer buffer, SelectionKey key) {
        String fileName = "";
        try {
            int sizeFull = buffer.getInt();
            int fileNameSize = buffer.getInt();
            byte[] fileNameInput = new byte[fileNameSize];
            buffer.get(fileNameInput);
            fileName = new String(fileNameInput, StandardCharsets.UTF_8);
            int size = buffer.getInt();

            byte[] input = new byte[buffer.limit() - buffer.position()];
            byte[] input1 = new byte[size];

            byte[] full_input = new byte[size];

            buffer.get(input);


            if (size > 1024) {
                SocketChannel client = (SocketChannel) key.channel();
                ByteBuffer buffer1 = ByteBuffer.allocate(size);
                client.read(buffer1);

                ByteBuffer buffer2 = ByteBuffer.allocate(size);
                ByteBuffer[] buf_arr = new ByteBuffer[2];
                buf_arr[0] = ByteBuffer.allocate(100000);
                buf_arr[1] = ByteBuffer.allocate(100000);

//                System.out.println(buffer1.position()+" "+buffer1.limit()+" "+(buffer1.position() < buffer1.limit()));

                while (buffer1.position() < (buffer1.limit()-1024)){
//                    System.out.println(size+" "+buffer1.position()+" "+buffer1.limit());
//                    client.read(buf_arr,0,2);
                    client.read(buffer1);
//                    client.read(buffer1);
                }


                buffer1.position(0);
                buffer1.get(input1);


                System.arraycopy(input, 0, full_input, 0, input.length);
                System.arraycopy(input1, 0, full_input, input.length, size - input.length);

            } else {
                System.arraycopy(input, 0, full_input, 0, size);
            }


            try {
                System.out.println("Writes file " + fileName);
                Files.write(Paths.get(SERVER_PACKAGE + "/" + fileName), full_input);

                broadcast("Uploaded file " + fileName);
                messages.add("Uploaded file " + fileName);
            } catch (Exception e) {
                System.out.println("e: " + e);
            }


        } catch (IOException e) {
            System.out.println("e: "+e);
        } catch (Exception e) {
            System.out.println("e: "+e);
        }


    }

    /**
     * Send file to client by key.
     * Looks for fileName, get from user, in server package.
     * If there is no such file, generates message to user.
     *
     * @param buffer command from client
     * @param key    key for client chanel identification
     */
    private void sendFile(ByteBuffer buffer, SelectionKey key) {
        int size = buffer.getInt();
        byte[] input = new byte[size];
        buffer.get(input);
        String fileName = new String(input);

        String message = "";

        try {
            Path file = Paths.get(SERVER_PACKAGE + "/" + fileName);
            if (Files.exists(file)) {
                try (InputStream inputStream = Files.newInputStream(file)) {
                    byte[] fileBytes = inputStream.readAllBytes();

                    ByteBuffer bufferToSend = ByteBuffer.allocate(2 * fileBytes.length);
                    bufferToSend.putInt(1);

                    bufferToSend.putInt(fileBytes.length);
                    bufferToSend.put(fileBytes);
                    bufferToSend.flip();

                    if (key.isValid() && key.channel() instanceof SocketChannel) {
                        SocketChannel channel = (SocketChannel) key.channel();

                        channel.write(bufferToSend);
                    }
                    buffer.clear();

                    System.out.println("File " + fileName + " is sent succesfully");
                } catch (IOException e) {
                    System.out.println("Some problems while sending file " + fileName);
                    message = "Could not send file " + fileName;
                }
            } else {
                message = "Could not find file " + fileName;
            }
        } catch (InvalidPathException e) {
            message = "Could not find file " + fileName;
        }
        if (!message.isEmpty()) {
            System.out.println("sent mes");
            sendMessage(key, message);
        }

    }

    /**
     * Функция для предоставления информации о подключившемся пользователе
     *
     * @param buffer command from client
     * @param key    key for client chanel identification
     */
    private void processIntroduction(ByteBuffer buffer, SelectionKey key) {
        int size = buffer.getInt();
        byte[] input = new byte[size * 2];
        buffer.get(input);
        String message = new String(input);
        connectedClients.put(((SocketChannel) key.channel()).socket().getRemoteSocketAddress(), message);
        broadcast(key, "Подключился пользователь " + message);
        sendMessageHistory(key);
    }

    /**
     * Функция для отключения пользователя
     *
     * @param key    key for client chanel identification
     */
    private void processQuit(SelectionKey key) {
        String name = connectedClients.get(((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
        if (name != null) {
            connectedClients.remove(((SocketChannel) key.channel()).socket().getRemoteSocketAddress());
            key.cancel();
            try {
                key.channel().close();
            } catch (IOException e) {
                e.getMessage();
            }
            broadcast("Пользователь " + name + " отключился");


        }
    }

    /**
     * Функция для получения информации о пользователях, которые сейчас онлайн
     *
     * @param key    key for client chanel identification
     */
    public void getOnlineClients(SelectionKey key) {
        StringBuffer message = new StringBuffer("Online: ");
        for (String name : connectedClients.values()) {
            message.append(name + ", ");
        }
        message.delete(message.length() - 2, message.length() - 1);
        sendMessage(key, message.toString());
    }

    /**
     * Функция для получения сообщения от пользователя
     *
     * @param buffer command from client
     * @param key    key for client chanel identification
     */
    public void getMessage(ByteBuffer buffer, SelectionKey key) {
        int size = buffer.getInt();
        byte[] input = new byte[size * 2];
        buffer.get(input);
        String message = new String(input);
        if (messages.size() >= 100) {
            String messageToRemove = messages.remove(0);
            if (messageToRemove.matches("Uploaded file (.*)")) {
                try {
                    Path fileToRemove = Paths.get("server/" + messageToRemove.split(" ")[2]);
                    Files.delete(fileToRemove);
                } catch (InvalidPathException e) {

                } catch (IOException e) {

                }

            }
        }
        messages.add(message);
        broadcast(key, message);
    }

    /**
     * Функция для отправки сообщения пользователю
     */
    private void sendMessage(SelectionKey key, String message) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.put(message.getBytes());
        buffer.flip();
        if (key.isValid() && key.channel() instanceof SocketChannel) {
            SocketChannel channel = (SocketChannel) key.channel();
            try {
                channel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        buffer.clear();
    }

    /**
     * broadcast используется для передачи сообщения всем клиентам
     */
    private void broadcast(String message) {
        System.out.println(message);
        for (SelectionKey key : selector.keys()) {
            sendMessage(key, message);
        }
    }

    /**
     * broadcast используется для передачи сообщения всем клиентам, кроме источника сообщения
     */
    private void broadcast(SelectionKey source, String message) {
        System.out.println(message);
        for (SelectionKey key : selector.keys()) {
            if (!source.channel().equals(key.channel()))
                sendMessage(key, message);
        }
    }

    /**
     * функция для отправки всех истории сообщений пользователю
     */
    private void sendMessageHistory(SelectionKey key) {
        for (String message : messages) sendMessage(key, message + '\n');
    }

}
