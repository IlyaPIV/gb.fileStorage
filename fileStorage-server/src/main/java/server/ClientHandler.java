package server;

import constants.ConnectionCommands;
import serverFiles.ServerFile;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

public class ClientHandler {

    private Server server;
    private Socket socket;
    private DataInputStream ins;
    private DataOutputStream ous;

    private ObjectOutputStream objous;

    private RandomAccessFile fwr;
    private DataInputStream fdins;

    private boolean authenticated;
    private String login;
    private Logger serverLogger;

    public ClientHandler(Server server, Socket socket){

        try {

            handlerSettings(server, socket);

            server.getServerSettings().getExecutorService().execute( ()-> {

                try {
                    tryToConnectUser();
                    userWorkingConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                    //логирование ошибки
                } finally {
                    disconnectClient();
                }

            });

        } catch (IOException e) {
            e.printStackTrace();
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
        this.objous = new ObjectOutputStream(socket.getOutputStream());

        this.fdins = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

    }

    /**
     * закрывает пользовательское подключение
     */
    private void disconnectClient(){

        server.disconnectUser(this);
        //serverLogger.log(Level.INFO,"Client "+login+" disconnected");

        try {

            /**
             * не знаю надо ли потоки закрывать отдельно или достаточно закрыть сокет
             */

//            ins.close();
//            ous.close();
//            objous.close();
//            fdins.close();
//            fwr.close();

            socket.close();

            System.out.println("Connection with user is closed");
        } catch (IOException e) {
            e.printStackTrace();
           //serverLogger.log(Level.SEVERE, "Problems with disconnection user "+login);
        }
    }

    /**
     * авторизация и подключение пользователя к серверу
     */
    private void tryToConnectUser() throws IOException{

        this.authenticated = true;  //временно

        try {
            socket.setSoTimeout(120000);

            while (true) {
                String msg = ins.readUTF();

                /**
                 * temp
                 */
                System.out.println("auth msg:" + msg);
                /**fggfh**/

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
        } finally {
            try {
                socket.setSoTimeout(0);
            } catch (SocketException e) {
                e.printStackTrace();
            }

        }

    }

    private void userWorkingConnection() throws IOException{
        while (authenticated) {
            String msg = ins.readUTF();

            /**
             * temp
             */
            System.out.println("work msg:" + msg);
            /**fggfh**/

            if (msg.equals(ConnectionCommands.END)) {
                sendMsgToClient(ConnectionCommands.END);
                this.authenticated = false;
                break;
            }

            if (msg.startsWith(ConnectionCommands.FILE_UPLOAD)) {
                uploadFileToServer(msg);
                continue;
            }

            if (msg.startsWith(ConnectionCommands.FILE_DOWNLOAD)) {
                break;
            }

            if (msg.startsWith(ConnectionCommands.FILE_DELETE)) {
                break;
            }

            if (msg.startsWith(ConnectionCommands.FILE_RENAME)) {
                break;
            }

            if (msg.startsWith(ConnectionCommands.FILE_SHARE)) {
                break;
            }

            if (msg.equals(ConnectionCommands.GET_FILES_LIST)) {
                sendUsersListOfFilesToClient();
            }
        }

    }

    /**
     * процедура загрузки файла на сервер
     * @param msg - строка сообщения с клиентского приложения
     */
    private void uploadFileToServer(String msg) {
        String[] tokens = msg.split("~");
        String filename = tokens[1];
        long filesize = Long.parseLong(tokens[2]);

        String filepath = Paths.get(FilesStorage.DIRECTORY).resolve(filename).toString();

        byte[] dataBytes = new byte[1024];
        File newFile = new File(filepath);
        int num = 0;

        try (RandomAccessFile fwr = new RandomAccessFile(newFile, "rw")) {
            long bytesLeft = filesize;

            while (fdins.available()>0 && bytesLeft>0) {
                int countThisTime = (int) Math.min(bytesLeft, dataBytes.length);
                num = fdins.read(dataBytes,0,countThisTime);
                fwr.write(dataBytes,0, num);
                fwr.skipBytes(num);
                bytesLeft-=countThisTime;
            }

        } catch (IOException e) {
            e.printStackTrace();
            //логирование

        }

        long newFileSize = 0;
        try {
            newFileSize = Files.size(newFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("New fire recieved. Size on client = %d. Size on server = %d\t", filesize, newFileSize);
        if (newFileSize==filesize) {
            sendMsgToClient(String.format("%s~%s~%d", ConnectionCommands.FILE_UPLOAD, ConnectionCommands.OPER_OK, newFileSize));
        } else {
            sendMsgToClient(String.format("%s~%s~%d", ConnectionCommands.FILE_UPLOAD, ConnectionCommands.OPER_FAIL, 0));
        }
    }


    /**
     * отправляет на клиентское приложение список загруженных файлов пользователя на сервере
     * ПРИМЕЧАНИЕ: не помешала бы оптимизация
     */
    private void sendUsersListOfFilesToClient() {
        //по хорошему не мешала бы оптимизация на случай потери пакета при передаче
        FilesStorage serverFS = server.getFilesStorage();

        List<ServerFile> serverFileList = serverFS.getFilesOnServer(0);

        sendMsgToClient(String.format("%s~%s~%d", ConnectionCommands.GET_FILES_LIST,ConnectionCommands.OPER_START,serverFileList.size()));

        for (ServerFile sf:
                serverFileList) {
            System.out.println(sf);

            sendFileInfoToClient(sf);

        }

        System.out.println("Список файлов отправлен");
    }

    /**
     * отправляет информацию о файле на клиента
     * @param sf
     */
    private void sendFileInfoToClient(ServerFile sf) {

        try {
            objous.writeObject(sf);
            objous.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * отправка сообщения/команды на клиент
     */
    public void sendMsgToClient(String msg){
        try {
            ous.writeUTF(msg);
            ous.flush();
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
