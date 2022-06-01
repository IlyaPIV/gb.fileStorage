package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;


public class Server {

    private ServerSettings serverSettings;
    private Socket clientSocket;

    private FilesStorage filesStorage;

    private List<ClientHandler> clients;

    public Server(){

        try {
            startServer();
            System.out.println("Server is started...");
            while (true)
            {
                clientConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
            //логирование
        } finally {
            stopServer();
        }
    }

    /**
     * запуск сервера
     * @throws IOException
     */
    private void startServer() throws IOException {
        this.serverSettings = new ServerSettings(this);
        this.serverSettings.setServerSocket(new ServerSocket(ServerSettings.SERVER_PORT));
        this.serverSettings.setExecutorService(Executors.newCachedThreadPool());

        this.clients = new CopyOnWriteArrayList<>();
        this.filesStorage = new FilesStorage();
        //logger

        //auth service

    }

    /**
     * закрытие сервера
     * @throws IOException
     */
    private void stopServer(){

        //под вопросом итератор
        for (ClientHandler ch:
             clients) {
            ch.disconnectFromServer();
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            //    server.ServerSettings.LOGGER.log(Level.SEVERE,e.getMessage());
        }

        try {
            serverSettings.getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        //    server.ServerSettings.LOGGER.log(Level.SEVERE,e.getMessage());
        }

    }

    /**
     * запуск создания нового подключения клиента к серверу
     * @throws IOException
     */
    private void clientConnection() throws IOException{
        clientSocket = serverSettings.getServerSocket().accept();
        new ClientHandler(this, clientSocket);
    }

    /**
     * возвращает серверные настройки
     * @return
     */
    public ServerSettings getServerSettings() {
        return serverSettings;
    }

    /**
     * возвращает модуль работы с файловым хранилищем
     * @return FileStorage - файловое хранилище
     */
    public FilesStorage getFilesStorage() {
        return filesStorage;
    }

    /**
     * добавление пользовательского соединения к серверу
     * @param ch
     */
    public void connectUser(ClientHandler ch){
        System.out.println("New connection is activated.");
        clients.add(ch);
    }

    /**
     * закрытие пользовательского соединения и удаление его из списка подключений
     * @param ch
     */
    public void disconnectUser(ClientHandler ch){
        clients.remove(ch);
    }
}
