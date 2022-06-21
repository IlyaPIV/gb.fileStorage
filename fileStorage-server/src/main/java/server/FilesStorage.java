package server;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import messages.FileTransferData;
import messages.StoragePathInRequest;
import server.hibernate.DBConnector;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.LinksEntity;
import serverFiles.ServerFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data @Slf4j
public class FilesStorage {

    public static final String DIRECTORY = "fileStorage-server\\server_storage\\";

    private static FilesStorage storage;

    private FilesStorage() {

    }

//    /**
//     * подготавливает информацию о файле в API формате
//     * @param path - путь к файлу
//     * @param id - айди файла
//     * @return ServerFile - инфо о файле в стандартизированном формате
//     */
//    private ServerFile prepareFileInfo(Path path, long id) {
//        ServerFile sf = new ServerFile();
//        sf.setFileName(path.getFileName().toString());
//        sf.setServerID(id);
//        sf.setDir(Files.isDirectory(path));
//        try {
//            sf.setSize(sf.isDir() ? -1L : Files.size(path));
//            sf.setLastUpdate(LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(0)));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        return sf;
//    }

    /**
     * получение ссылки на хранилище файловое
     * @return - ссылка
     */
    public static FilesStorage getFilesStorage() {
        if (storage == null) storage = new FilesStorage();
        return storage;
    }


    /**
    * подготавливает список файлов пользователя на стороне сервере
     */
    private List<ServerFile> prepareServerFiles(DirectoriesEntity currentDirectory, DirectoriesEntity homeDir){

        List<ServerFile> serverFiles = new ArrayList<>();

        if (!currentDirectory.equals(homeDir)) {
            serverFiles.add(prepareParentDirFile(currentDirectory));
            log.debug("Подготовили элемент-каталог для перехода вверх");
        }

        serverFiles.addAll(prepareFoldersInside(currentDirectory));
        log.debug("Подготовили список папок внутри текущего каталога");

        serverFiles.addAll(prepareFilesInside(currentDirectory, serverFiles.size()));

        return serverFiles;
    }

    /**
     *
     * @param currentDirectory - Ссылка на текущую директорию пользователя на сервере
     * @return List - обработанный список файлов в текущей директории
     */
    private List<ServerFile> prepareFilesInside(DirectoriesEntity currentDirectory, int currentSize) {
        List<ServerFile> list = new ArrayList<>();
        try {
            for (LinksEntity link:
                    DBConnector.getLinksInsideDir(currentDirectory)) {

                ServerFile newFile = new ServerFile();
                newFile.setPoz(currentSize);
                newFile.setLastUpdate(link.getLastUpdate());
                newFile.setFileName(link.getLinkName());
                newFile.setDir(false);
                newFile.setServerID(link.getFileId());
                newFile.setSize(DBConnector.getFileSize(link.getFileId()));
                newFile.setLinkID(link.getLinkId());
                list.add(newFile);
                currentSize++;
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }

        return list;
    }

    private List<ServerFile> prepareFoldersInside(DirectoriesEntity currentDirectory) {
        List<ServerFile> list = new ArrayList<>();
        int counter = 1;
        try {
            for (DirectoriesEntity dir:
                DBConnector.getDirsInside(currentDirectory)) {

                ServerFile newDir = new ServerFile();
                newDir.setPoz(counter);
                newDir.setLastUpdate(dir.getDateTime());
                newDir.setFileName(dir.getDirName());
                newDir.setDir(true);
                newDir.setServerID(dir.getDirId());
                newDir.setSize(-1L);
                newDir.setLinkID(0);

                list.add(newDir);
                counter++;
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
        return list;
    }

    private ServerFile prepareParentDirFile(DirectoriesEntity currentDirectory) {
        ServerFile parent = new ServerFile();
        parent.setDir(true);
        parent.setFileName(ServerFile.HOME_DIR_NAME);
        parent.setSize(-1L);
        parent.setPoz(0);
        parent.setServerID(currentDirectory.getDirId());
        parent.setLastUpdate(currentDirectory.getDateTime());
        parent.setLinkID(0);

        return parent;
    }

    /**
     * обработка и вызов функции получения списка файлов
     * @param currentDirectory - ссылка на запись в БД текущей директории пользователя (
     * @return List<ServerFile> - список пользовательских файлов
     */
    public List<ServerFile> getFilesOnServer(DirectoriesEntity currentDirectory, DirectoriesEntity homeDirectory) {
        return prepareServerFiles(currentDirectory, homeDirectory);
    }

    /**
     * функция возвращает содержимое выбранного на клиенте файла из данных сервера
     * @param fileID - id файла в таблице реальных файлов БД
     * @return byte[] - массив байтов содержимого файла
     * @throws ServerCloudException - в случае если выбран файл или не удалось найти физический файл на сервере
     * @throws IOException - ошибка считывания данных файла
     */
    public byte[] getFileData(long fileID) throws ServerCloudException, IOException {

        //в будущем заменится на фактический путь к файлу из SQL
        Path pathToFile = Path.of(DIRECTORY).resolve(DBConnector.getServerPathToFile(fileID)).normalize();

        //проверки
        if (!Files.exists(pathToFile)){
            throw new ServerCloudException("Указанный файл не найден на сервере!");
        }
        if (Files.isDirectory(pathToFile)) {
            throw new ServerCloudException("Выбранный файл является директорией!");
        }

        //получение данных
        return Files.readAllBytes(pathToFile);
    }


    /**
     * сохраняет файл на сервере в директорию пользователя
     * @param fileData - входящие данные
     * @param serverDirectory - физический путь к папке на сервере
     * @param dbLinkDirectory - ссылка БД на текущую директорию сохранения ссылки
     * @param dbFileDirectory - ссылка БД на текущую директорию сохранения файла
     * @throws IOException - в случае ошибки записи данных в файл на сервере
     */
    public void saveFile(FileTransferData fileData, Path serverDirectory
                                , DirectoriesEntity dbLinkDirectory
                                , DirectoriesEntity dbFileDirectory) throws IOException{

        Files.write(serverDirectory.resolve(fileData.getName()), fileData.getData());

        try {
            DBConnector.saveNewFile(fileData.getName(), dbLinkDirectory, dbFileDirectory);
        } catch (ServerCloudException e) {
            log.error(e.getMessage());
            Files.delete(serverDirectory.resolve(fileData.getName()));
        }
    }

    /**
     * переходит в родительскую директорию текущей папки пользователя по таблице в БД
     * @param currentDirectory - ссылка на текущую директорию, открытую на клиенте
     */
    public DirectoriesEntity currentDirectoryUP(DirectoriesEntity currentDirectory) throws RuntimeException{
        return DBConnector.returnParentDirectory(currentDirectory);
    }


    /**
    * обработка команды перехода внутрь указанного каталога
     * @param msg - сообщение от клиента с информацией о цели перехода
     */
    public DirectoriesEntity currentDirectoryIN(StoragePathInRequest msg) throws RuntimeException{

        try {
            return DBConnector.getChildDir(msg.getDirID());
        } catch (Exception e) {
            throw new RuntimeException("Не удалось перейти внутрь каталога");
        }

    }

    /**
     * инициализация стартовых параметров пользователя
     * @param login - String - имя пользователя в базе SQL, а так же имя каталога в директории файлохранилища
     */
    public Path getUsersServerPath(String login) {
        Path dirPath = Path.of(DIRECTORY).normalize().resolve(login);
        if (new File(dirPath.toString()).mkdir()) log.debug("new directory was created.");

        log.debug("User's home DIR on server is: "+dirPath.toAbsolutePath());
        return dirPath;
    }

    /**
     * создаёт в БД запись о новом каталоге
     * @param folderName - имя новой директории
     * @param currentDirectory - ссылка на текущий каталог пользователя
     * @return true - если всё прошло успешно
     * false - если сделать запись не удалось
     */
    public boolean createNewVirtualDir(String folderName, DirectoriesEntity currentDirectory) {
        try {
            DBConnector.createNewDir(folderName, currentDirectory.getDirId(), currentDirectory.getUserId());
            return true;
        } catch (ServerCloudException e) {
            log.error("Failed to create new DIR record in DB");
            return false;
        }
    }

    public void deleteRealFile(String fileName, String dirName) throws ServerCloudException{
        Path filePath = Path.of(DIRECTORY).normalize().resolve(dirName).resolve(fileName);
        try {
          Files.delete(filePath);
        } catch (IOException e) {
            throw new ServerCloudException("Failed to delete file: " + filePath.toString());
        }
    }
}
