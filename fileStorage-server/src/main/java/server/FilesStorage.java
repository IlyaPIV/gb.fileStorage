package server;

import serverFiles.ServerFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class FilesStorage {

    private static final String DIRECTORY = "server/storage/";

    private ServerFile prepareFileInfo(Path path, long id) {
        ServerFile sf = new ServerFile();
        sf.setFileName(path.getFileName().toString());
        sf.setServerID(id);
        try {
            sf.setSize(Files.size(path));
            sf.setLastUpdate(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sf;
    }

    private List<ServerFile> prepareServerFiles(int userID){

        List<ServerFile> serverFiles = new ArrayList<>();
        long id = 0;
        try {
            List<Path> files = Files.list(Path.of(DIRECTORY)).toList();
            for (Path pf :
                 files) {
                id++;
                serverFiles.add(prepareFileInfo(pf,id));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return serverFiles;
    }

    public List<ServerFile> getFilesOnServer(int userID) {
        return prepareServerFiles(userID);
    }
}
