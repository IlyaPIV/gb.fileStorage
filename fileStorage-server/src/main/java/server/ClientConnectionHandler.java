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
    private DirectoriesEntity usersHomeDirectory;
    private DirectoriesEntity currentDirectory;
    private Path userServerDirectory;
    private int userID;

    public ClientConnectionHandler(FilesStorage filesStorage, DBConnector dbConnector){
        this.filesStorage = filesStorage;
        this.dbConnector = dbConnector;
    }

    private void setUserSettings(String login) throws RuntimeException{
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
                /**
                 * требуется поменять получение пути файла
                 */
                FileTransferData fileData =
                        new FileTransferData(filesStorage.getFileData(fdr.getFileName(), userServerDirectory),
                                                                                        fdr.getFileName());
                chc.writeAndFlush(fileData);
                log.debug("File was send to user");
            } catch (RuntimeException e) {
                log.warn(e.getMessage());
                chc.writeAndFlush(new ErrorAnswerMessage(e.getMessage()));
            } catch (IOException e) {
                log.error(e.getMessage());
                chc.writeAndFlush(new ErrorAnswerMessage(e.getMessage()));
            }

        } else if (inMessage instanceof ServerFilesListRequest) {
            /*
            //доработать id юзера
            */
            chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));

        } else if (inMessage instanceof FileTransferData fileData) {

            log.debug(String.format("incoming file data: { name = %s; size = %d}",
                                                fileData.getName(), fileData.getSize()));

            /*
            //доработать id юзера
            */
            try {
                filesStorage.saveFile(fileData, userServerDirectory, currentDirectory);

                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
            } catch (IOException e) {
                log.error("Error with saving file on server!!!");
                chc.writeAndFlush(new ErrorAnswerMessage("Error with saving file on server!!!"));
            }

        } else if (inMessage instanceof StoragePathUpRequest) {
            log.debug("Server current path UP request. Refreshing server files list in new directory");

            if (!currentDirectory.equals(usersHomeDirectory)) {
                currentDirectory = filesStorage.currentDirectoryUP(currentDirectory);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
            } else  {
                chc.writeAndFlush(new ErrorAnswerMessage("Can't change directory UP."));
            }

        } else if (inMessage instanceof StoragePathInRequest msg) {
            log.debug("Server current path IN request. Refreshing server files list in new directory");
            try {
                currentDirectory = filesStorage.currentDirectoryIN(msg);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(currentDirectory, usersHomeDirectory)));
            } catch (RuntimeException e) {
                log.error(e.getMessage());
                chc.writeAndFlush(new ErrorAnswerMessage("Can't change directory IN."));
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
        }


    }

    private AuthRegAnswer tryToAuthUser(AuthRegRequest request) {
        /*
         * место под сервис авторизации
         */
        try {
            this.userID = dbConnector.authentication(request.getLogin(), request.getPassword());
            setUserSettings(request.getLogin());
            log.debug("Авторизация прошла успешно. Пользовательские настройки завершены.");
        } catch (Exception e) {
            return new AuthRegAnswer(false, e.getMessage(), false);
        }

        return new AuthRegAnswer(true,"all is ok", false);
    }

    private AuthRegAnswer tryToRegUser(AuthRegRequest request) {

        try {
            dbConnector.registration(request.getLogin(), request.getPassword());
        } catch (Exception e) {
            return new AuthRegAnswer(false, e.getMessage(), true);
        }

        return new AuthRegAnswer(true, "Пользователь успешно зарегестрирован", true);

    }
}
