package server;

import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import messages.*;
import server.hibernate.DBConnector;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.UsersEntity;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ClientConnectionHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private final FilesStorage filesStorage;
    private final DBConnector dbConnector;
    private DirectoriesEntity usersHomeDirectory; //ссылка на родительскую директорию пользователя в БД
    private DirectoriesEntity currentDirectory; //ссылка на текущую директорию пользователя
    private Path userServerDirectory; //физический путь к папке файлов пользователя на сервере
    private int userID;

    public ClientConnectionHandler(FilesStorage filesStorage, DBConnector dbConnector){
        this.filesStorage = filesStorage;
        this.dbConnector = dbConnector;
    }

    /**
     * заполняет начальные настройки пользователя
     * @param login - имя пользователя
     * @throws ServerCloudException - ошибка работы с БД
     */
    private void setUserSettings(String login) throws ServerCloudException{
        DirectoriesEntity homeDir = DBConnector.getUserHomeDir(userID);
        log.debug("Ссылка на стартовую директорию: "+homeDir);
        this.currentDirectory = homeDir;
        this.usersHomeDirectory = homeDir;
        this.userServerDirectory = filesStorage.getUsersServerPath(login);

        log.debug("Каталог пользователя на сервере: "+userServerDirectory.toString());
    }


    @Override
    protected void channelRead0(ChannelHandlerContext chc, CloudMessage inMessage) throws Exception {

        log.debug("incoming message of type: " + inMessage.getClass().toString());
        if (inMessage instanceof FileDownloadRequest fdr) {

            try {
                FileTransferData fileData =
                        new FileTransferData(filesStorage.getFileData(fdr.getFileID()), fdr.getFileName());
                chc.writeAndFlush(fileData);
                log.debug("File was send to user");
            } catch (RuntimeException | IOException e) {
                log.error(e.getMessage());
                chc.writeAndFlush(new DatabaseOperationResult(false, e.getMessage()));
            }

        } else if (inMessage instanceof ServerFilesListRequest) {
            log.debug("Incoming files list request");
            chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));

        } else if (inMessage instanceof FileTransferData fileData) {

            log.debug(String.format("incoming file data: { name = %s; size = %d}",
                    fileData.getName(), fileData.getSize()));

            try {
                filesStorage.saveFile(fileData, userServerDirectory, currentDirectory, usersHomeDirectory);

                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
            } catch (IOException e) {
                log.error("Error with saving file on server!!!");
                chc.writeAndFlush(new DatabaseOperationResult(false,"Error with saving file on server!!!"));
            }

        } else if (inMessage instanceof StoragePathUpRequest) {
            log.debug("Server current path UP request. Refreshing server files list in new directory");

            if (!currentDirectory.equals(usersHomeDirectory)) {
                currentDirectory = filesStorage.currentDirectoryUP(currentDirectory);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
                log.debug("Server files list was sent.");
            } else {
                chc.writeAndFlush(new DatabaseOperationResult(false,"Can't change directory UP."));
            }

        } else if (inMessage instanceof StoragePathInRequest msg) {
            log.debug("Server current path IN request. Refreshing server files list in new directory");
            try {
                currentDirectory = filesStorage.currentDirectoryIN(msg);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                chc.writeAndFlush(new DatabaseOperationResult(false,"Can't change directory IN."));
            }

        } else if (inMessage instanceof AuthRegRequest request) {
            if (request.isOperationReg()) {
                log.debug("Attempt to reg new user");
                chc.writeAndFlush(tryToRegUser(request));
                log.debug("Answer was send to: " + chc.channel().toString());
            } else {
                log.debug("Attempt to sign in on server");
                chc.writeAndFlush(tryToAuthUser(request));
                log.debug("Answer was send to: " + chc.channel().toString());
            }
        } else if (inMessage instanceof NewDirRequest request) {
            log.debug("Attempt to create new virtual folder in current");
            if (filesStorage.createNewVirtualDir(request.getFolderName(), currentDirectory)) {
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
                log.debug("New server files list was sent.");
                chc.writeAndFlush(new DatabaseOperationResult(true, "New DIR was created on server"));
            } else {
                chc.writeAndFlush(new DatabaseOperationResult(false, "Failed to create new DIR record in DB"));
            }
        } else if (inMessage instanceof FileRenameRequest request) {
            log.debug("Attempt to rename links name");
            if (DBConnector.renameLink(request.getLinkID(), request.getNewName())) {
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
                log.debug("New links name is set");
                chc.writeAndFlush(new DatabaseOperationResult(true, "File's name was changed."));
            } else {
                chc.writeAndFlush(new DatabaseOperationResult(false, "Failed to rename file's name"));
            }
        } else if (inMessage instanceof DeleteRequest deleteRequest) {
            log.debug("Attempt to delete " + (deleteRequest.isDir() ? "directory." : "file."));
            if (DBConnector.deleteInDB(deleteRequest.isDir(), deleteRequest.getId())) {
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
                log.debug("Deleting was successfully finished");
                chc.writeAndFlush(new DatabaseOperationResult(true, "Deleting was successfully finished"));
            } else {
                chc.writeAndFlush(new DatabaseOperationResult(false, "Failed to delete " +  (deleteRequest.isDir() ? "directory." : "file.")));
            }
        } else if (inMessage instanceof FileLinkRequest request) {
            log.debug("Attempt to get crypto link on selected item");
            try {
                chc.writeAndFlush(new FileLinkData(DBConnector.getCryptoLink(request.getLinkID())));
                log.debug("Ссылка на файл сформирована и отправлена пользователю");
            } catch (ServerCloudException e) {
                chc.writeAndFlush(new DatabaseOperationResult(false, "Failed to get link on file"));
            }

        } else if (inMessage instanceof FileLinkData data) {
            log.debug("Attempt to add file to current dir by link");
            try {
                DBConnector.addLinkByCryptoString(DBConnector.decryptLink(data.getCryptoLink()), currentDirectory);
                log.debug("Ссылка успешно добавлена в каталог пользователя.");
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
                chc.writeAndFlush(new DatabaseOperationResult(true, "Link was added to current directory"));
            } catch (ServerCloudException e) {
                chc.writeAndFlush(new DatabaseOperationResult(false, "Failed to add link in current dir"));
            }
        } else {
            log.error("Unknown incoming message format!!!");
        }

    }

    /**
     * процедура пробует выполнить авторизацию пользователя
     * @param request - сообщение с клиентского приложения
     * @return подготовленное сообщение клиенту с результатом выполнения операции
     */
    private AuthRegAnswer tryToAuthUser(AuthRegRequest request) {

        try {
            this.userID = dbConnector.authentication(request.getLogin(), request.getPassword());
            setUserSettings(request.getLogin());
            log.debug("Авторизация прошла успешно. Пользовательские настройки завершены.");
        } catch (Exception e) {
            log.error("Ошибка авторизации пользователя");
            return new AuthRegAnswer(false, e.getMessage(), false);
        }

        return new AuthRegAnswer(true,"Authorization is ok", false);
    }

    /**
     * процедура пробует выполнить регистрацию нового пользователя
     * @param request - сообщение с клиентского приложения
     * @return - подготовленное сообщение киленту с результатом выполнения операции
     */
    private AuthRegAnswer tryToRegUser(AuthRegRequest request) {

        try {
            dbConnector.registration(request.getLogin(), request.getPassword());
            log.debug("Успешная регистрация");
        } catch (Exception e) {
            return new AuthRegAnswer(false, e.getMessage(), true);
        }

        return new AuthRegAnswer(true, "Пользователь успешно зарегестрирован", true);

    }
}
