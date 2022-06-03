package com.gb.filestorage.filestorageclient.network_Netty;

import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import lombok.extern.slf4j.Slf4j;
import messages.CloudMessage;
import messages.ServerFilesListData;
import messages.ServerFilesListRequest;

import java.io.IOException;
import java.net.Socket;

@Slf4j
public class NettyConnection {

    public static final int SERVER_PORT = 8189;
    public static final String SERVER_ADDRESS = "localhost";

    private ObjectDecoderInputStream inS;
    private ObjectEncoderOutputStream outS;

    private boolean isAuthorizated;

    public NettyConnection() throws IOException {

        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            this.outS = new ObjectEncoderOutputStream(socket.getOutputStream());
            this.inS = new ObjectDecoderInputStream(socket.getInputStream());

        } catch (IOException e) {
            //log.error("Error to open connection to server!!!");
            throw new IOException("Error to open connection to server!");
        }

    }

    public void messageReader(){

            while (true) {
                try {
                    CloudMessage inMsg = readMsg();

                    if (inMsg instanceof ServerFilesListData sl) {
                        log.debug("Список файлов на сервере");
                    }

                } catch (IOException e) {
                    log.error("Error with reading input channel");
                } catch (ClassNotFoundException e) {
                    log.error("Incoming message wrong format");
                }
            }

    }

    private CloudMessage readMsg() throws IOException, ClassNotFoundException {
        log.debug("Incoming message from server");
        return (CloudMessage) inS.readObject();
    }

    private void write(CloudMessage message) throws IOException {
        outS.writeObject(message);
        outS.flush();
    }

    public boolean isAuthorizated() {
        return isAuthorizated;
    }

    public void setAuthorizated(boolean authorizated) {
        isAuthorizated = authorizated;
    }

    public void sendFilesListRequest() throws IOException{
        write(new ServerFilesListRequest());
    }
}
