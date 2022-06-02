package com.gb.filestorage.filestorageclient.terminal_NIO;

import com.gb.filestorage.filestorageclient.ClientMainController;
import com.gb.filestorage.filestorageclient.network_IO.ClientSettings;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class TerminalClient {

    private SocketChannel sc;
    private InetSocketAddress address;

    private ClientMainController clientController;

    Thread serverListener;

    public TerminalClient(ClientMainController cmc) throws IOException {
        clientController = cmc;
        sc = SocketChannel.open();
        address = new InetSocketAddress(ClientSettings.SERVER_ADDRESS, ClientSettings.SERVER_PORT);
    }

    public void start() throws IOException {
        //пробуем подключиться к серверу
        if (!sc.connect(address)) {
            while (!sc.finishConnect()) {
                System.out.println("Идет попытка подключения...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        serverListener = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    try {
                        int read = sc.read(buffer);
                        if (read > 0) {
                            clientController.terminalDisplay.appendText(new String(buffer.array()));
                        }
                    } catch (IOException e) {
                       // throw new RuntimeException(e);
                        break;
                    }
                }
            }
        });
        serverListener.setDaemon(true);
        serverListener.start();
    }

    public void sendMsgToServer(String msg) {

        ByteBuffer buf = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));

        try {
            sc.write(buf);
        } catch (IOException e) {
            clientController.terminalDisplay.appendText("ошибка отправки сообщения на сервер\n");
        }

    }



    public void stop() throws IOException {
        //sc.finishConnect();
      //  serverListener.interrupt();
        sc.close();
    }

    public boolean isRunning(){
        return sc.isOpen();
    }


}
