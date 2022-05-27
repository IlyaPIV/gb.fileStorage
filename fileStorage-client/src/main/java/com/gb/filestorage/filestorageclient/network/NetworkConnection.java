package com.gb.filestorage.filestorageclient.network;

import com.gb.filestorage.filestorageclient.ClientMainController;
import constants.ConnectionCommands;

import java.io.IOException;
import java.net.Socket;

public class NetworkConnection {

    private ClientMainController client;

    private ClientSettings settings;
    private boolean authenticated;
    private String login;
    private String password;


    public NetworkConnection(ClientMainController client, String login, String password) {
        this.client = client;
        this.settings = new ClientSettings();
        this.authenticated = false;
        this.password = password;
        this.login = login;
    }


    private void setAuthenticated(boolean auth){
        this.authenticated = auth;
    }


    /**
     * функция проверки состояния сетевого соккета
     * @return true если закрыт, false - если работает
     */
    public boolean isSocketClosed(){
        return settings.getSocket().isClosed();
    }


    /**
     * попытка инициализации сетевого подключения к серверу
     * @return true в случае успешного подключения
     */
    public boolean connectToServer(){
        boolean connected = false;
        try {
            this.settings.setSocket(new Socket(ClientSettings.SERVER_ADDRESS, ClientSettings.SERVER_PORT));
            this.settings.setDataStreams();
            connected = true;
        } catch (IOException e) {
            e.printStackTrace();
            //логирование ошибки
        }
        return connected;
    }

    /**
     * процедура отправки сообщений на сервер
     * @param msg - форматированная строка сообщения на сервер
     */
    private void sendMsgToServer(String msg) {
        try {
            settings.ous.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            //логирование ошибки отправки сообщения
        }
    }

    public void startWorkingThreadWithServer() {

        new Thread(()->{
           try {
               //аутентификация на сервере
               authenticationOnServer();

               //работа с сервером
                workingOnServer();

           } catch (IOException e) {
               e.printStackTrace();
           } finally {
               setAuthenticated(false);
               try {
                   this.settings.closeSocket();
               } catch (IOException e) {
                   //логирование ошибки
                   throw new RuntimeException(e);
               }
           }
        }).start();
    }


    /**
     * отправка связки логин/пароль на сервер с целью авторизации
     */
    public void tryToAuthOnServer(){
        String msg = String.format("%s~%s~%s", ConnectionCommands.AUTH_TRY, login, password);

        sendMsgToServer(msg);
    }


    /**
     * обработчик получения ответов от сервера при попытке авторизации
     * @throws IOException
     */
    private void authenticationOnServer() throws IOException{
        while (true) {
            String msg = settings.ins.readUTF();

            if (msg.equals(ConnectionCommands.END)) break;

            if (msg.equals(ConnectionCommands.DISC)) {
                Thread.currentThread().interrupt();
                break;
            }

            if (msg.startsWith(ConnectionCommands.REG_TRY)) {
                String[] splitMsg = msg.split("~");
                boolean result = splitMsg[1].equals(ConnectionCommands.OPER_OK);
                String serverMsg = splitMsg[2];
                client.setInfoText(serverMsg);
                if (result) {
                    //переключение на авторизацию
                }
            }

            if (msg.startsWith(ConnectionCommands.AUTH_TRY)) {
                String[] splitMsg = msg.split("~");
                boolean result = splitMsg[1].equals(ConnectionCommands.OPER_OK);
                String serverMsg = splitMsg[2];
                client.setInfoText(serverMsg);
                if (result) {
                    setAuthenticated(true);
                }

            }
        }
    }

    /**
     * обработчик получения ответов от сервера при установленном соединении
     * @throws IOException
     */
    private void workingOnServer() throws IOException{
        while (true) {
            String msg = settings.ins.readUTF();

            if (msg.equals(ConnectionCommands.END)) break;

            if (msg.equals(ConnectionCommands.DISC)) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    public void closeConnection() {
        sendMsgToServer(ConnectionCommands.END);
    }
}
