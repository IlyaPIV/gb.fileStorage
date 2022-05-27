import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
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
        serverLogger.log(Level.INFO,"Client "+login+" disconnected");

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
           serverLogger.log(Level.SEVERE, "Problems with disconnection user "+login);
        }
    }

    /**
     * авторизация и подключение пользователя к серверу
     */
    private void tryToConnectUser() throws SocketTimeoutException {
        server.getServerSettings().getExecutorService().execute(()->{
            try {
                try {
                    socket.setSoTimeout(120000);
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }

                this.login = "Неизвестный пользователь";

 //               while (true) {
                    this.authenticated = true;
                    server.connectUser(this);
                    serverLogger.log(Level.FINE, login+" - успешная авторизация");
 //               }

            } finally {
                try {
                    socket.setSoTimeout(0);
                } catch (SocketException e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }

    private void userWorkingConnection() throws IOException{
        while (authenticated) {
            String str = ins.readUTF();

        }
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
