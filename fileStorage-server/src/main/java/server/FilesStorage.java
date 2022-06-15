package server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import messages.FileTransferData;
import messages.StoragePathInRequest;
import serverFiles.ServerFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Data @Slf4j
public class FilesStorage {

    public static final String DIRECTORY = "fileStorage-server\\server_storage\\";

    public FilesStorage() {

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
        sf.setDir(Files.isDirectory(path));
        try {
            sf.setSize(sf.isDir() ? -1L : Files.size(path));
            sf.setLastUpdate(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return sf;
    }


    /**
    * подготавливает список файлов пользователя на стороне сервере
     */
    private List<ServerFile> prepareServerFiles(int userID, Path currentDirectory){

        List<ServerFile> serverFiles = new ArrayList<>();

        long id = 1;

        if (!currentDirectory.equals(getUsersStartPath(userID))) {
            serverFiles.add(prepareParentDirFile(currentDirectory));
        }

        File folder = new File(currentDirectory.toString());
        File[] files = folder.listFiles();

        assert files != null;
        for (File fl:
                 files) {
                    id++;
                    serverFiles.add(prepareFileInfo(fl.toPath(),id));
            }

        return serverFiles;
    }

    private ServerFile prepareParentDirFile(Path currentDirectory) {
        ServerFile parent = new ServerFile();
        parent.setDir(true);
        parent.setFileName(ServerFile.HOME_DIR_NAME);
        parent.setSize(-1L);
        parent.setServerID(0);
        try {
            parent.setLastUpdate(LocalDateTime.ofInstant(Files.getLastModifiedTime(currentDirectory).toInstant(), ZoneOffset.ofHours(0)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return parent;
    }

    /**
     * обработка и вызов функции получения списка файлов
     * @param userID - id пользователя
     * @return List<ServerFile> - список пользовательских файлов
     */
    public List<ServerFile> getFilesOnServer(int userID, Path currentDirectory) {
        return prepareServerFiles(userID, currentDirectory);
    }


    /**
     * функция возвращает содержимое выбранного на клиенте файла из данных сервера
     * @param fileName - имя файла в интерфейсе пользователя на стороне клиента
     * @return byte[] - массив байтов содержимого файла
     * @throws RuntimeException - в случае если выбран файл или не удалось найти физический файл на сервере
     * @throws IOException - ошибка считывания данных файла
     */
    public byte[] getFileData(String fileName, Path currentDirectory) throws RuntimeException, IOException {

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
     * @param fileData - входящие данные
     * @param userID - ID пользователя
     * @throws IOException - в случае ошибки записи данных в файл на сервере
     */
    public void saveFile(FileTransferData fileData, int userID, Path currentDirectory) throws IOException{

        /*
        / тут будет SQL с userID
         */
        Files.write(currentDirectory.resolve(fileData.getName()), fileData.getData());

    }

    /**
     * переходит в родительскую директорию текущей папки пользователя
     */
    public Path currentDirectoryUP(Path currentDirectory) throws RuntimeException{
        /*
        / место под SQL в будущем
         */
        return currentDirectory.getParent();
    }


    /**
     * инициализация стартовых параметров пользователя
     * @param userID - id пользователя в базе SQL
     */
    public Path getUsersStartPath(int userID) {

        /*
         *позже тут будет SQL данные получение
         */
        return Path.of(DIRECTORY);
    }

    /*
    *
     */
    public Path currentDirectoryIN(StoragePathInRequest msg, Path current) throws RuntimeException{

        try {
            Path newPath = current.resolve(msg.getDirName());
            return newPath;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось перейти внутрь каталога");
        }

    }

    public Path getUsersServerPath(String login) {
        Path dirPath = Path.of(DIRECTORY).normalize().resolve(login);
        if (new File(dirPath.toString()).mkdir()) log.debug("new directory was created.");

        log.debug("User's home DIR on server is: "+dirPath.toAbsolutePath());
        return dirPath;
    }
}
