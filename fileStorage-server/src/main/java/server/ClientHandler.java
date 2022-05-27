package server;

import constants.ConnectionCommands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream ins;
    private DataOutputStream ous;

    private boolean authenticated;
    private String login;
    private Logger serverLogger;

    public ClientHandler(Server server, Socket socket){

        try {

            handlerSettings(server, socket);

            tryToConnectUser();

            userWorkingConnection();

        } catch (SocketTimeoutException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        } finally {
            disconnectClient();
        }

    }

    /**
     * заполняет базовые настройки сервера
    * */
    private void handlerSettings(Server server, Socket socket) throws IOException{
        this.server = server;
        this.socket = socket;
        this.serverLogger = ServerSettings.LOGGER;
        this.ins = new DataInputStream(socket.getInputStream());
        this.ous = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * закрывает пользовательское подключение
     */
    private void disconnectClient(){

        server.disconnectUser(this);
        //serverLogger.log(Level.INFO,"Client "+login+" disconnected");


        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
           //serverLogger.log(Level.SEVERE, "Problems with disconnection user "+login);
        }
    }

    /**
     * авторизация и подключение пользователя к серверу
     */
    private void tryToConnectUser(){
        server.getServerSettings().getExecutorService().execute(()->{
            try {
                socket.setSoTimeout(120000);

                while (true) {
                    String msg = ins.readUTF();

                    if (msg.equals(ConnectionCommands.END)) {
                        sendMsgToClient(ConnectionCommands.END);
                        break;
                    }

                    if (msg.startsWith(ConnectionCommands.REG_TRY)) {

                        String[] tokens = msg.split("~");
                        if (tokens.length<3) continue;
                        //регистрация пользователей


                        String result = ConnectionCommands.OPER_FAIL;
                        String message = "nickname is already used";

                        sendMsgToClient(String.format("%s~%s~%s",ConnectionCommands.REG_TRY,result,message));
                        break;
                    }

                    if (msg.startsWith(ConnectionCommands.AUTH_TRY)) {

                        String[] tokens = msg.split("~");
                        if (tokens.length<3) continue;

                        //авторизация пользователя
                        this.login = tokens[1];
                        this.authenticated = true;
                        server.connectUser(this);

                        String result = ConnectionCommands.OPER_OK;
                        String message = "Connection is created";

                        sendMsgToClient(String.format("%s~%s~%s",ConnectionCommands.AUTH_TRY,result,message));
                        break;
                    }

                }


            }
            catch (SocketException e) {
                sendMsgToClient(ConnectionCommands.END);
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    socket.setSoTimeout(0);
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    private void userWorkingConnection() throws IOException{
        while (authenticated) {
            String msg = ins.readUTF();

            if (msg.equals(ConnectionCommands.END)) {
                sendMsgToClient(ConnectionCommands.END);
                break;
            }
        }

    }

    /**
     * отправка сообщения/команды на клиент
     */
    public void sendMsgToClient(String msg){
        try {
            ous.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            //добавить логирование
        }
    }

    /**
     * принудительное отключение клиентского подключения в случае завершения сервера
     */
    public void disconnectFromServer() {
        sendMsgToClient(ConnectionCommands.DISC);
        disconnectClient();
    }
}
