package messages;

import serverFiles.ServerFile;

import java.nio.file.Path;
import java.util.List;

public class ServerFilesListData implements CloudMessage{

    private final List<ServerFile> fileList;

    public ServerFilesListData(List<ServerFile> list) {

        this.fileList = list;

    }

    public List<ServerFile> getFileList() {
        return fileList;
    }
}
