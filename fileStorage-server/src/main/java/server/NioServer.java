package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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
            System.out.println("recieved msg from client: " + clientMsg);

            byte[] answer = ("SERVER/"+currentDirectory+"-> "+clientMsg+"\n").getBytes(StandardCharsets.UTF_8);

            channel.write(ByteBuffer.wrap(answer));

        }

    }



}
