package com.gb.filestorage.filestorageclient.network_Netty;

import com.gb.filestorage.filestorageclient.ClientMainController;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import lombok.extern.slf4j.Slf4j;
import messages.*;
import serverFiles.ServerFile;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class NettyConnection {


    public static final int SERVER_PORT = 8189;
    public static final String SERVER_ADDRESS = "localhost";

    private final ObjectDecoderInputStream inS;
    private final ObjectEncoderOutputStream outS;
    private final ClientMainController clientUI;

    private boolean isAuthorizated;


    public NettyConnection(ClientMainController UI) throws IOException {

        try {
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            this.outS = new ObjectEncoderOutputStream(socket.getOutputStream());
            this.inS = new ObjectDecoderInputStream(socket.getInputStream());
            this.clientUI = UI;
        } catch (IOException e) {
            //log.error("Error to open connection to server!!!");
            throw new IOException("Error to open connection to server!");
        }


    }

    /**
     * процедура прослушки входящего канала на сообщения в бесконечном цикле
     */
    public void messageReader(){

            log.debug("Messages' listener channel is created");
            try {
                while (true) {
                    try {
                        CloudMessage inMsg = readMsg();
                        log.debug("Incoming message from server { " + inMsg.getClass().getSimpleName() + " }");
                        if (inMsg instanceof ServerFilesListData sl) {
                            updateServerFilesList(sl);
                        } else if (inMsg instanceof FileTransferData) {
                            writeFileToClient((FileTransferData) inMsg);
                        }
                        clientUI.updateClientList(Path.of(clientUI.getCurrentPath()));
                    } catch (IOException e) {
                        log.error("Error with reading input channel");
                    } catch (ClassNotFoundException e) {
                        log.error("Incoming message wrong format");
                    }
                }
            } catch (Exception e) {
                log.error("Connection with server is lost");
            }
    }

    /**
     * сохраняет файл, полученный с сервера
     * @param inMsg -
     */
    private void writeFileToClient(FileTransferData inMsg) {
        Path newFilePath = Path.of(clientUI.getCurrentPath()).resolve(inMsg.getName());
        if (Files.exists(newFilePath)) {
            log.debug("Файл с таким именем уже существует: "+newFilePath.toString());

            /*
            тут будет обработка такого события
             */
        } else {
            try {
                Files.write(newFilePath, inMsg.getData());
                log.debug("Файл сохранён на диске");
                clientUI.setInfoText("File was saved in current directory!");
            } catch (IOException e) {
                log.error("Ошибка сохранения файла на диске");
                clientUI.setInfoText("Failed to save downloaded file");
            }
        }

    }

    /**
     * считывает сообщение из входящего канала
     * @return - сообщение сервера, реализующее интерфейс CloudMessage
     * @throws IOException - ошибка чтения входящего сообщения
     * @throws ClassNotFoundException - ошибка каста входящего сообщения
     */
    private CloudMessage readMsg() throws IOException, ClassNotFoundException {
        return (CloudMessage) inS.readObject();
    }

    /**
     * отправляет сообщение серверу в преобразованном формате
     * @param message - сообщение одного из классов, реализующих интерфейс CloudMessage
     * @throws IOException - ошибка отправки сообщения
     */
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

    /**
     * отправляет запрос на сервер о предоставлении списка файлов пользователя
     * @throws IOException - ошибка при отправке сообщения на сервер
     */
    public void sendFilesListRequest() throws IOException{
        write(new ServerFilesListRequest());
    }

    /**
     * обновляет данные в ТЧ "файлы на сервере" полученными данными с сервера
     * @param serverFilesListData - сообщение от сервера, содержащее в том числе список файлов на сервере
     */
    private void updateServerFilesList(ServerFilesListData serverFilesListData){
        clientUI.updateServerList(serverFilesListData.getFileList());
    }

    /**
     * отправляет файл на сервер
     * @param pathToFile - путь к файлу на клиентской стороне
     * @throws IOException - в случае какой либо ошибки при отправке сообщения на сервер
     */
    public void sendFileToServer(Path pathToFile) throws IOException {
        write(new FileTransferData(pathToFile));
    }

    /**
     * отправляет запрос на скачивание файла со стороны сервера
     * @param fileName - имя файла на сервере в текущей папке пользователя
     * @param fileID - id файла на сервере (заготовка на будущее, когда подключится SQL)
     * @throws IOException - в случае какой либо ошибки при отправке сообщения на сервер
     */
    public void getFileFromServer(String fileName, long fileID) throws IOException {
        write(new FileDownloadRequest(fileName, fileID));
    }

    public void sendPathChangeRequest(ServerFile sf) throws IOException {
        if (sf.getFileName().equals(ServerFile.HOME_DIR_NAME)) {
            try {
                write(new StoragePathUpRequest());
            } catch (IOException e) {
                log.error("ошибка отправки сообщения Path UP");
                throw new IOException("Failed to send Path UP request");
            }
        } else {
            try {
                write(new StoragePathInRequest(sf.getFileName(), sf.getServerID()));
            } catch (IOException e) {
                log.error("Ошибка отправки сообщения Path IN");
                throw new IOException("Failed to send Path IN request");
            }
        }
    }
}
