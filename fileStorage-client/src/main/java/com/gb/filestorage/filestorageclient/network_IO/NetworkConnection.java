package com.gb.filestorage.filestorageclient.network_IO;

import com.gb.filestorage.filestorageclient.ClientMainController;
import constants.ConnectionCommands;
import messages.DeleteRequest;
import serverFiles.ServerFile;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

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


    public void setAuthenticated(boolean auth){
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
     * проверяет создан ли соккет подключения
     * @return true если соккет инициализирован, false - если равен NULL
     */
    public boolean isSocketInit(){
        return settings.getSocket() != null;
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
    public void sendMsgToServer(String msg) {
        try {
            settings.ous.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            //логирование ошибки отправки сообщения

        }
    }

    /**
     * процедура запуска рабочего цикла взаимодействия с сервером
     */
    public void startWorkingThreadWithServer() {
        Thread wthrd = new Thread(()->{
           try {
               //аутентификация на сервере
               authenticationOnServer();

               //получение списка файлов с сервера
               updateServersFilesList();

               //работа с сервером
                workingOnServer();

           } catch (IOException e) {
               e.printStackTrace();
           } finally {
//               closeConnection();
               try {
                   this.settings.closeSocket();
               } catch (IOException e) {
                   //логирование ошибки
                   throw new RuntimeException(e);
               }
           }
        });
//        wthrd.setDaemon(true);
        wthrd.start();
    }

    /**
     * отправка сообщения на сервер команды получения списка файлов
     */
    public void updateServersFilesList() {
        sendMsgToServer(ConnectionCommands.GET_FILES_LIST);

        System.out.println("хочу список файлов с сервера!!!");
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

            System.out.println("auth msg:" + msg);

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
                break;
            }

            if (msg.startsWith(ConnectionCommands.AUTH_TRY)) {
                String[] splitMsg = msg.split("~");
                boolean result = splitMsg[1].equals(ConnectionCommands.OPER_OK);
                String serverMsg = splitMsg[2];
                client.setInfoText(serverMsg);
                if (result) {
                    setAuthenticated(true);
                }
                break;
            }
        }
    }

    /**
     * обработчик получения ответов от сервера при установленном соединении
     * @throws IOException
     */
    private void workingOnServer() throws IOException{
        System.out.println("working thread");
        while (true) {
            String msg = settings.ins.readUTF();

            System.out.println("work msg:" + msg);

            if (msg.equals(ConnectionCommands.END)) break;

            if (msg.equals(ConnectionCommands.DISC)) {
                Thread.currentThread().interrupt();
                break;
            }

            if (msg.startsWith(ConnectionCommands.FILE_UPLOAD)) {
                String[] tokens = msg.split("~");
                if (tokens.length != 3) {
                    continue;
                }
                if (tokens[1].equals(ConnectionCommands.OPER_OK)) {
                    client.setInfoText("operation - OK");
                    updateServersFilesList();
                } else if (tokens[1].equals(ConnectionCommands.OPER_FAIL)) {
                    client.setInfoText("operation - failed");
                } else {
                    client.setInfoText("Failed to load servers' file list");
                };
                continue;
            }

            if (msg.startsWith(ConnectionCommands.FILE_DOWNLOAD)) {
                continue;
            }

            if (msg.startsWith(ConnectionCommands.GET_FILES_LIST)) {
                String[] tokens = msg.split("~");
                if (tokens.length != 3) {
                    continue;
                }
                if (tokens[1].equals(ConnectionCommands.OPER_START)) {
                    client.serverFilesTable.getItems().clear();
                    int listSize = Integer.parseInt(tokens[2]);

                    for (int i = 0; i < listSize; i++) {
                        try {
                            ServerFile serverFileInfo = (ServerFile) settings.objins.readObject();
                            System.out.println(serverFileInfo);
                            client.serverFilesTable.getItems().add(serverFileInfo);

                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }

                } else if (tokens[1].equals(ConnectionCommands.OPER_OK)) {
                    client.setInfoText("operation - OK");
                } else if (tokens[1].equals(ConnectionCommands.OPER_FAIL)) {
                    client.setInfoText("operation - failed");
                } else {
                    client.setInfoText("Failed to load servers' file list");
                };
            }
        }
    }

    /**
     * закрытие сетевого подключения с сервером
     */
    public void closeConnection() {
        sendMsgToServer(ConnectionCommands.END);
    }

    /**
     * отправка файла на сервер
     * @param fileFullPath - адрес файла на клиенте
     */
    public void fileSendToServer(Path fileFullPath, String filename) {


        byte[] dataBytes = new byte[1024];
        File fileToSend = new File(fileFullPath.toString());
        long fileSize = 0;
        try {
            fileSize = Files.size(fileFullPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        sendMsgToServer(String.format("%s~%s~%d",ConnectionCommands.FILE_UPLOAD,filename,fileSize));
        OutputStream fdout = settings.fdout;

        try (InputStream fins = new FileInputStream(fileToSend)){

            while (fins.available()>0) {
                int n = fins.read(dataBytes);
                fdout.write(dataBytes);
                fdout.flush();
            }
            fdout.flush();
            System.out.println("Отправка файла завершена");
        } catch (IOException e) {
            e.printStackTrace();
            //логирование
        }
    }

    /**
     * получение файла с сервера
     * @param directory папка на клиенте куда скачать файл
     * @param filename имя загружаемого файла
     */
    public void fileGetFromServer(Path directory, String filename) {
        sendMsgToServer(ConnectionCommands.FILE_DOWNLOAD);
    }

    /**
     * переименовывает файл на сервер
     */
    public void fileOnServerRename(){
        sendMsgToServer(ConnectionCommands.FILE_RENAME);
    }

    /**
     * удаляет файл на сервере (пользовательиский экземпляр)
     */
    public void fileOnServerDelete(){
        sendMsgToServer(ConnectionCommands.FILE_DELETE);
    }

    /**
     * получает ссылку на файл на сервере
     */
    public void fileOnServerShare(){
        sendMsgToServer(ConnectionCommands.FILE_SHARE);
    }

}
