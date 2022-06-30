package server.old;

import server.FilesStorage;
import server.ServerSettings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;


public class IOServer {

    private ServerSettings serverSettings;
    private Socket clientSocket;

    private FilesStorage filesStorage;

    private List<IOClientHandler> clients;

    public IOServer(){

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

        this.serverSettings = new ServerSettings();
        this.serverSettings.setServerSocket(new ServerSocket(ServerSettings.SERVER_PORT));
        this.serverSettings.setExecutorService(Executors.newCachedThreadPool());

        this.clients = new CopyOnWriteArrayList<>();
        this.filesStorage = FilesStorage.getFilesStorage();
        //logger

        //auth service

    }

    /**
     * закрытие сервера
     */
    private void stopServer(){

        //под вопросом итератор
        for (IOClientHandler ch:
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
     * @throws IOException - ошибка
     */
    private void clientConnection() throws IOException{
        clientSocket = serverSettings.getServerSocket().accept();
        new IOClientHandler(this, clientSocket);
    }

    /**
     * возвращает серверные настройки
     * @return ссылку на настройки
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
     * НЕ ИСПОЛЬЗУЕТСЯ
     * добавление пользовательского соединения к серверу
     * @param ch IO клиент хендлер
     */
    public void connectUser(IOClientHandler ch){
        System.out.println("New connection is activated.");
        clients.add(ch);
    }

    /**
     * НЕ ИСПОЛЬЗУЕТСЯ
     * закрытие пользовательского соединения и удаление его из списка подключений
     * @param ch - хендрел
     */
    public void disconnectUser(IOClientHandler ch){
        clients.remove(ch);
    }
}
