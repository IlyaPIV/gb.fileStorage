package server;

import lombok.extern.slf4j.Slf4j;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import messages.*;

import java.io.IOException;

@Slf4j
public class ClientConnectionHandler extends SimpleChannelInboundHandler<CloudMessage> {

    private FilesStorage filesStorage;

    public ClientConnectionHandler(){
        filesStorage = new FilesStorage();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext chc, CloudMessage inMessage) throws Exception {

        log.debug("incoming message of type: "+inMessage.getClass().toString());
        if (inMessage instanceof FileDownloadRequest fdr) {


            try {
                FileTransferData fileData = new FileTransferData(filesStorage.getFileData(fdr.getFileName()), fdr.getFileName());
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

            //доработать id юзера
            chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(5)));

        } else if (inMessage instanceof FileTransferData fileData) {

            log.debug(String.format("incoming file data: { name = %s; size = %d}",
                                                fileData.getName(), fileData.getSize()));

            //доработать id юзера
            try {
                filesStorage.saveFile(fileData.getName(), fileData.getData(), fileData.getSize(), 0);

                chc.writeAndFlush(new ServerFilesListData(filesStorage.getFilesOnServer(0)));
            } catch (IOException e) {
                log.error("error with saving file on server!!!");
                chc.writeAndFlush(new ErrorAnswerMessage("error with saving file on server!!!"));
            }

        }


    }
}
