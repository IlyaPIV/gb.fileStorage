package server;

import server.old.IOServer;

import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class ServerSettings {

    public static final int SERVER_PORT = 8189;
    private final server.old.IOServer IOServer;

    private ServerSocket serverSocket;

    private ExecutorService executorService;

    public static final Logger LOGGER = Logger.getLogger(IOServer.class.getName());

    public ServerSettings(IOServer IOServer) {
        this.IOServer = IOServer;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }


    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }


}
