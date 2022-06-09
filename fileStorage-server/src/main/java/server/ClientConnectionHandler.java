package server;

import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import messages.*;
import server.hibernate.DBConnector;
import server.hibernate.entity.UsersEntity;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class ClientConnectionHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private final FilesStorage filesStorage;
    private final DBConnector dbConnector;
    private final Path usersHomeDirectory;
    private Path currentDirectory;

    private int userID;

    public ClientConnectionHandler(FilesStorage filesStorage, DBConnector dbConnector){
        this.filesStorage = filesStorage;
        this.dbConnector = dbConnector;

        this.userID = 666;

        this.currentDirectory = filesStorage.getUsersStartPath(userID);

        this.usersHomeDirectory = currentDirectory;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext chc, CloudMessage inMessage) throws Exception {

        log.debug("incoming message of type: " + inMessage.getClass().toString());
        if (inMessage instanceof FileDownloadRequest fdr) {

            try {
                FileTransferData fileData =
                        new FileTransferData(filesStorage.getFileData(fdr.getFileName(), currentDirectory),
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
            chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(userID, currentDirectory)));

        } else if (inMessage instanceof FileTransferData fileData) {

            log.debug(String.format("incoming file data: { name = %s; size = %d}",
                                                fileData.getName(), fileData.getSize()));

            /*
            //доработать id юзера
            */
            try {
                filesStorage.saveFile(fileData, userID, currentDirectory);

                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(userID, currentDirectory)));
            } catch (IOException e) {
                log.error("Error with saving file on server!!!");
                chc.writeAndFlush(new ErrorAnswerMessage("Error with saving file on server!!!"));
            }

        } else if (inMessage instanceof StoragePathUpRequest) {
            log.debug("Server current path UP request. Refreshing server files list in new directory");

            if (currentDirectory!=usersHomeDirectory) {
                currentDirectory = filesStorage.currentDirectoryUP(currentDirectory);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(userID, currentDirectory)));
            } else  {
                chc.writeAndFlush(new ErrorAnswerMessage("Can't change directory UP."));
            }

        } else if (inMessage instanceof StoragePathInRequest msg) {
            log.debug("Server current path IN request. Refreshing server files list in new directory");
            try {
                currentDirectory = filesStorage.currentDirectoryIN(msg, currentDirectory);
                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(userID, currentDirectory)));
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
        return new AuthRegAnswer(true,"all is ok", false);
    }

    private AuthRegAnswer tryToRegUser(AuthRegRequest request) {
        /*
         * место под сервис авторизации
         */
        try {
            dbConnector.addUser(request.getLogin(), request.getPassword());
        } catch (Exception e) {
            return new AuthRegAnswer(false, e.getMessage(), true);
        }

//        if (dbConnector.findUserByLogin(request.getLogin()) == null) {
//            log.debug("Пользователь с таким именем не найден");
//        }
        return new AuthRegAnswer(false, "Can't reg new user - service is offline.", true);

    }
}
