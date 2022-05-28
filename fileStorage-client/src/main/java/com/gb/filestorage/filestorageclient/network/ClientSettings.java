package com.gb.filestorage.filestorageclient.network;

import java.io.*;
import java.net.Socket;

public class ClientSettings {

    public static final int SERVER_PORT = 8189;
    public static final String SERVER_ADDRESS = "localhost";

    private Socket socket;

    public DataInputStream ins;
    public DataOutputStream ous;

    public BufferedInputStream bfins;
    public BufferedOutputStream bfous;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void closeSocket() throws IOException{
        this.socket.close();
    }

    public void setDataStreams() throws IOException {
        this.ins = new DataInputStream(socket.getInputStream());
        this.ous = new DataOutputStream(socket.getOutputStream());
        this.bfins = new BufferedInputStream(socket.getInputStream());
        this.bfous = new BufferedOutputStream(socket.getOutputStream());
    }


}
