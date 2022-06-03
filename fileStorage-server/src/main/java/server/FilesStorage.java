package server;

import lombok.Data;
import serverFiles.ServerFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Data
public class FilesStorage {

    public static final String DIRECTORY = "fileStorage-server\\server_storage\\";

    private Path currentDirectory;

    public FilesStorage() {
        this.currentDirectory = Path.of(DIRECTORY);
    }

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

        File folder = new File(currentDirectory.toString());
        File[] files = folder.listFiles();

        assert files != null;
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


    /**
     * функция возвращает содержимое выбранного на клиенте файла из данных сервера
     * @param fileName - имя файла в интерфейсе пользователя на стороне клиента
     * @return byte[] - массив байтов содержимого файла
     * @throws RuntimeException - в случае если выбран файл или не удалось найти физический файл на сервере
     * @throws IOException - ошибка считывания данных файла
     */
    public byte[] getFileData(String fileName) throws RuntimeException, IOException {

        //в будущем заменится на фактический путь к файлу из SQL
        Path pathToFile = currentDirectory.resolve(fileName);

        //проверки
        if (!Files.exists(pathToFile)){
            throw new RuntimeException("Указанный файл не найден на сервере!");
        }
        if (Files.isDirectory(pathToFile)) {
            throw new RuntimeException("Выбранный файл является директорией!");
        }

        //получение данных
        return Files.readAllBytes(pathToFile);
    }


    /**
     * сохраняет файл на сервере в директорию пользователя
     * @param name - имя файла
     * @param data - массив байтов файла
     * @param size - размер в байтах
     * @param id - ID пользователя
     * @throws IOException - в случае ошибки записи данных в файл на сервере
     */
    public void saveFile(String name, byte[] data, long size, int id) throws IOException{

        Files.write(currentDirectory.resolve(name), data);

    }
}
