package messages;

import serverFiles.ServerFile;

import java.nio.file.Path;
import java.util.List;

public class ServerFilesListData implements CloudMessage{

    List<ServerFile> fileList;

    public ServerFilesListData(List<ServerFile> list) {

        this.fileList = list;

    }
}
