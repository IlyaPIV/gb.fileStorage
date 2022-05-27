import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class Server {

    private ServerSettings serverSettings;
    private Socket clientSocket;

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

        } finally {
            System.out.println("Server is stopped...");
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
        //logger

        //clients list

    }

    /**
     * закрытие сервера
     * @throws IOException
     */
    private void stopServer(){

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            //    ServerSettings.LOGGER.log(Level.SEVERE,e.getMessage());
        }

        try {
            serverSettings.getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        //    ServerSettings.LOGGER.log(Level.SEVERE,e.getMessage());
        }


    }

    private void clientConnection() throws IOException{
        clientSocket = serverSettings.getServerSocket().accept();
        new ClientHandler(this, clientSocket);
    }

    public ServerSettings getServerSettings() {
        return serverSettings;
    }

    public void connectUser(ClientHandler ch){
        clients.add(ch);
    }

    public void disconnectUser(ClientHandler ch){
        clients.remove(ch);
    }
}
