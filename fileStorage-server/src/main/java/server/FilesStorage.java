package server;

import serverFiles.ServerFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class FilesStorage {

    public static final String DIRECTORY = "fileStorage-server/server_storage/";



    /**
     * подготавливает информацию о файле в API формате
     * @param path - путь к файлу
     * @param id - айди файла
     * @return ServerFile - инфо о файле в стандартизированном формате
     */
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


    /**
    * подготавливает список файлов пользователя на стороне сервере
     */
    private List<ServerFile> prepareServerFiles(int userID){

        List<ServerFile> serverFiles = new ArrayList<>();
        long id = 0;
//        try {

            File folder = new File(DIRECTORY);
            File[] files = folder.listFiles();

            for (File fl:
                 files) {
                if (!fl.isDirectory()) {
                    id++;
                    serverFiles.add(prepareFileInfo(fl.toPath(),id));
                }
            }

        return serverFiles;
    }

    /**
     * обработка и вызов функции получения списка файлов
     * @param userID - id пользователя
     * @return List<ServerFile> - список пользовательских файлов
     */
    public List<ServerFile> getFilesOnServer(int userID) {
        return prepareServerFiles(userID);
    }


}
