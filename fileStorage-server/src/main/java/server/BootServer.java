package server;

import server.old.NioServer;

import java.io.IOException;

public class BootServer {
    public static void main(String[] args) {
        //new Server();

//        try {
//            NioServer ns = new NioServer();
//            ns.start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        new NettyServer();

    }
}
