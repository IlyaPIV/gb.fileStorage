package server;

import constants.ConnectionCommands;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    private ServerSocketChannel server;
    private Selector selector;

    private String currentDirectory;

    public NioServer() throws IOException {
        server = ServerSocketChannel.open();
        selector = Selector.open();

        server.bind(new InetSocketAddress(ServerSettings.SERVER_PORT));
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);

        currentDirectory = FilesStorage.DIRECTORY;
    }

    public void start() throws IOException {
        System.out.println("сервак запущен");
        while (server.isOpen()) {

            selector.select();
//            if (selector.select(1000)==0) {
//                System.out.println("Прошла 1 сек без подключения... Жду дальше...");
//                continue;
//            }

            //если мы тут - значит что-то случилось
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iter = keys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();

                //OP_ACCEPT - новое клиентское соединение
                if (key.isAcceptable()) {
                    handleAccept();
                }

                if (key.isReadable()) {
                    handleRead(key);
                }

                if (key.isWritable()) {
                    //
                    System.out.println("к такому меня жизнь не готовила");
                }

                keys.remove(key);
            }
        }
        System.out.println("сервак выключен");
    }

    private void handleAccept() throws IOException {
        //получаем сетевой клиент
        SocketChannel channel = server.accept();
        //устанавливаем неблокирующим
        channel.configureBlocking(false);
        //подключаем к регистрации события чтения
        channel.register(selector, SelectionKey.OP_READ);   //


        System.out.println("Удачно подключён клиент: "+channel.toString());
        //шлем ответ клиенту
        channel.write(ByteBuffer.wrap("connection to server is activated\n".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1024);

        SocketChannel channel = (SocketChannel) key.channel();

        StringBuilder sb = new StringBuilder();

        while (channel.isOpen()) {
            int readedBytes = channel.read(buf);

            if (readedBytes<0) {
                channel.close();
                return;
            }

            if (readedBytes==0) {
                break;
            }

            buf.flip(); //обнуление позиции в считанном буффере


            while (buf.hasRemaining()) {
                sb.append((char) buf.get());    //расшифровка из байтов в символы
            }
            String clientMsg = sb.toString();
            System.out.println("received msg from client: " + clientMsg);

            //ответка клиенту поступившей команды
            byte[] answer = ("SERVER\\"+currentDirectory+"-> "+clientMsg+"\n").getBytes(StandardCharsets.UTF_8);
            channel.write(ByteBuffer.wrap(answer));

            String Msg = clientMsg.toLowerCase();
            //обработчик поступивших сообщений на наличие прочих команд
            if (Msg.equalsIgnoreCase(ConnectionCommands.CMD_LS)) {
                System.out.println("команда на вывод списка файлов");
                sendListOfFilesNIO(channel);
            }
            if (Msg.equalsIgnoreCase(ConnectionCommands.CMD_HELP)) {
                System.out.println("команда на help");
                printTerminalCommands(channel);
            }
            if (Msg.startsWith(ConnectionCommands.CMD_CAT))  {
                System.out.println("команда на вывод содержимого файла");
                printFileData(channel, Msg);
            }
            if (Msg.startsWith(ConnectionCommands.CMD_CD)){
                System.out.println("команда на смену дирректории");
                changeDirectory(channel, Msg);
            }

        }

    }

    /**
     * отправляет на терминал содержимое файла
     * @param channel
     * @param msg
     */
    private void printFileData(SocketChannel channel, String msg) throws IOException {
        String[] tokens = msg.split(" ");
        if (tokens.length != 2){
            channel.write(ByteBuffer.wrap("Wrong format of command!\n".getBytes()));
            return;
        }
        File file = new File(Paths.get(currentDirectory).resolve(tokens[1]).toString());
        if (!file.exists()) {
            channel.write(ByteBuffer.wrap("No such file in current directory!\n".getBytes(StandardCharsets.UTF_8)));
            return;
        }
        if (file.isDirectory()) {
            channel.write(ByteBuffer.wrap("It is directory path - can't get data\n".getBytes(StandardCharsets.UTF_8)));
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try (RandomAccessFile aFile = new RandomAccessFile(file, "rw");
                FileChannel inFileChannel = aFile.getChannel()) {
            while (inFileChannel.read(buffer)>0) {
                buffer.flip();
                channel.write(buffer);
            }
        }

        channel.write(ByteBuffer.wrap("\n".getBytes(StandardCharsets.UTF_8)));

    }

    /**
     * меняет текущую дирректорию на сервере
     * @param channel
     * @param clientMsg
     */
    private void changeDirectory(SocketChannel channel, String clientMsg) throws IOException {
        String[] tokens = clientMsg.split(" ");
        if (tokens.length != 2){
            channel.write(ByteBuffer.wrap("Wrong format of command!\n".getBytes()));
            return;
        }
        if (tokens[1].equals("..")) {
            currentDirectory = new File(currentDirectory).getParentFile().getPath();
            byte[] answer = ("SERVER\\"+currentDirectory+"-> directory was changed...\n").getBytes(StandardCharsets.UTF_8);
            channel.write(ByteBuffer.wrap(answer));
        } else {
            Path path = Paths.get(currentDirectory).resolve(tokens[1]);
            System.out.println(path.toAbsolutePath());

            if (!Files.exists(path) || !Files.isDirectory(path)) {
                channel.write(ByteBuffer.wrap("This path is not to directory\n".getBytes(StandardCharsets.UTF_8)));
            } else  {
                currentDirectory = path.toString();
                byte[] answer = ("SERVER\\"+currentDirectory+"-> directory was changed...\n").getBytes(StandardCharsets.UTF_8);
                channel.write(ByteBuffer.wrap(answer));
            }
        }

    }

    /**
     * отправляет клиенту список доступных терминальных команд
     * @param channel
     * @throws IOException
     */
    private void printTerminalCommands(SocketChannel channel) throws IOException {
        byte[] command;
        command = String.format("%-15s\t- %s\n", "CD [ .. ]"     ,"переход в родительский каталог").getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(command));
        command = String.format("%-15s\t- %s\n", "CD [DIR]"      ,"смена текущего каталога").getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(command));
        command = String.format("%-15s\t- %s\n", "LS"            ,"вывод списка файлов в текущем каталоге").getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(command));
        command = String.format("%-15s\t- %s\n", "CAT [FILE]"    ,"вывод содержимого указанного файла в терминал").getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(command));
        command = "\n".getBytes(StandardCharsets.UTF_8);
        channel.write(ByteBuffer.wrap(command));

    }

    /**
     * отправляет список файлов в текущей директории
     * @param channel
     * @throws IOException
     */
    private void sendListOfFilesNIO(SocketChannel channel) throws IOException {

        File folder = new File(currentDirectory);
        File[] files = folder.listFiles();

        for (File fl:
                files) {
            byte[] filename = (String.format("%s",fl.getName()) + "\n").getBytes(StandardCharsets.UTF_8);

            channel.write(ByteBuffer.wrap(filename));
        }

    }


}
