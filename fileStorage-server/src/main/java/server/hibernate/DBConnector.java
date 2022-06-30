package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import server.CryptoService;
import server.FilesStorage;
import server.ServerCloudException;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.LinksEntity;
import server.hibernate.entity.RealFilesEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class DBConnector implements AuthService {

    public DBConnector() {

    }


    /*
    * ============================ AUTH SERVICE =========================
     */

    @Override
    public void registration(String login, String password) throws ServerCloudException{

        if (!HibernateRequests.isUserAlreadyExists(login)) {
            log.debug("Пользователь с таким именем в базе не найден!");
            int id = HibernateRequests.addNewUser(login, password);
            log.debug("Зарегестрирован новый пользователь! ID = " + id);
            //получить ID нового пользователя и подготовить его стартовую директорию в БД
            HibernateRequests.createUserHomeDir(id, login);
            log.debug("Добавлена в БД ссылка на стартовую директорию пользователя.");

        } else {
            log.debug("Пользователь с таким именем уже существует!");
            throw new ServerCloudException("Пользователь с таким именем уже существует!");
        }

    }


    @Override
    public int authentication(String login, String password) throws ServerCloudException{
        if (HibernateRequests.isUserAlreadyExists(login)) {
            log.debug("Пользователь существует - проверяем пароль");

            int userId = HibernateRequests.getUserID(login, password);
            if (userId==Integer.MIN_VALUE) {
                log.debug("Имя или пароль пользователя не верны!");
                throw new ServerCloudException("Имя или пароль пользователя не верны! Попробуйте ещё");
            } else {
                log.debug(String.format("ID пользователя = %s. Введённые данные верны", userId));
                return userId;
            }

        } else {
            log.debug("Пользователь с таким именем не найден в базе");
            throw new ServerCloudException("Пользователь с таким именем не найден в базе");
        }
    }

    /*
     * ============================ SERVER COMMANDS SERVICE =========================
     */



    /**
     * возвращает начальную директорию пользователя
     * @param userID - id пользователя в БД
     * @return ссылку на директорию в БД
     */
    public static DirectoriesEntity getUserHomeDir(int userID) throws ServerCloudException{
        return HibernateRequests.getUserHomeDir(userID);
    }

    /**
     * обработка запроса перехода внутрь каталога на сервере
     * @param dirID - айди новой директории
     * @return ссылку на новую директорию
     */
    public static DirectoriesEntity getChildDir(long dirID) throws RuntimeException{
        return HibernateRequests.getDirectoryByID((int) dirID);
    }


    /**
     * возвращает родительский каталог текущей директории
     * @param currentDirectory - ссылка на текущую директорию
     * @return ссылка на родителя
     */
    public static DirectoriesEntity returnParentDirectory(DirectoriesEntity currentDirectory) throws RuntimeException{
        return HibernateRequests.getDirectoryByID(currentDirectory.getParentDir());
    }

    /**
     * возвращает список дочерних директорий указанного каталога пользователя
     * @param currentDirectory - ссылка на родительскую директорию
     * @return List - список директорий дочерних
     * @throws RuntimeException - в случае ошибки исполнения запроса к БД
     */
    public static List<DirectoriesEntity> getDirsInside(DirectoriesEntity currentDirectory) throws RuntimeException{
        return HibernateRequests.getListChildDirs(currentDirectory);
    }

    /**
     * возвращает список ссылок на файлы в указанной директории
     * @param currentDirectory - текущий каталог пользователя
     * @return List - список ссылок на файлы
     */
    public static List<LinksEntity> getLinksInsideDir(DirectoriesEntity currentDirectory) throws RuntimeException{
        return HibernateRequests.getListChildLinks(currentDirectory);
    }

    /**
     * возвращает размер файла в байтах по ID
     * @param fileId - айди файла в БД
     * @return long - физический размер файла
     * @throws RuntimeException - ошибка получения размера файла
     */
    public static long getFileSize(int fileId) {

        RealFilesEntity realFile = HibernateRequests.getFileByID(fileId);
        DirectoriesEntity dirFile = HibernateRequests.getDirectoryByID(realFile.getDirectoryId());

        try {
            return Files.size(Path.of(FilesStorage.DIRECTORY).normalize()
                                                                .resolve(dirFile.getDirName())
                                                                .resolve(realFile.getName()));
        } catch (Exception e) {
            throw new RuntimeException("Не удалось получить размер файла с ID =" + fileId);
        }

    }

    /**
     * сохраняет в БД записи о новом файле
     * @param name - имя файла
     * @param dbLinkDirectory - виртуальный каталог в БД, где лежит ссылка на файл
     * @param dbFileDirectory - виртуальный каталог в БД, где физически лежит файл на сервере
     * @throws ServerCloudException - ошибка выполнения операции
     */
    public static void saveNewFile(String name
                                    , DirectoriesEntity dbLinkDirectory
                                    , DirectoriesEntity dbFileDirectory) throws ServerCloudException {
        int idFile = HibernateRequests.createRealFileInfo(name, dbFileDirectory.getDirId());
        try {
            HibernateRequests.createLinkToFile(name, idFile, dbLinkDirectory.getDirId());
        } catch (ServerCloudException e) {
            //удалить запись в БД с файлом
            HibernateRequests.deleteRealFileInfo(idFile);
            throw e;
        }

    }

    /**
     * возвращает строковый путь к файлу (каталог + настоящее имя файла)
     * @param fileID - айди файла в БД
     * @return String - строка с нормализованным путём к файлу
     * @throws ServerCloudException - ошибка при работе с БД
     */
    public static String getServerPathToFile(long fileID) throws ServerCloudException{

        RealFilesEntity file = HibernateRequests.getFileByID((int) fileID);
        if (file != null) {
            return Path.of(HibernateRequests.getDirectoryByID(file.getDirectoryId()).getDirName())
                                                    .resolve(file.getName())
                                                    .normalize().toString();
        } else {
            throw new ServerCloudException("Не удалось найти данные о файле с id = " + fileID + " в БД!");
        }
    }

    /**
     *  делает запись в БД о новой вложенной папке в текущем каталоге пользователя
     */
    public static void createNewDir(String folderName, int dirId, int userId) throws ServerCloudException{
        HibernateRequests.createNewDirectory(folderName, dirId, userId);
    }

    /**
     * меняет имя ссылки в БД
     * @param linkID - id записи в таблице ссылок
     * @param newName - новое имя записи
     * @return - true в случае успеха
     * false - в случае ошибки в ходе транзакций
     */
    public static boolean renameLink(int linkID, String newName) {
        try {
            HibernateRequests.changeLinkName(linkID, newName);
        } catch (ServerCloudException e) {
            log.error(e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * операция удаления в БД
     * @param isDir - признак удаления каталога
     * @param id - id ссылки/каталога
     * @return true - в случае успешного выполнения всех операций
     * false - в противном случае
     */
    public static boolean deleteInDB(boolean isDir, int id) {
        try {
            if (isDir) { HibernateRequests.deleteDirAndLinks(id); }
                    else { HibernateRequests.deleteLinkOnFile(id); }
            return true;
        } catch (ServerCloudException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    /**
     * удаление физически файла с жёсткого диска
     * @param name - имя файла
     * @param dirName - имя каталога
     */
    public static void deleteRealFile(String name, String dirName) throws ServerCloudException{
        FilesStorage.getFilesStorage().deleteRealFile(name, dirName);
    }

    /**
     * получает зашифрованную строку с данными о запрошенном файле по ссылке
     * @param linkID - id запрашиваемой ссылки
     * @return String с зашифрованной ссылкой на файл в БД
     * @throws ServerCloudException - ошибка шифрования сообщения
     */
    public static String getCryptoLink(int linkID) throws ServerCloudException{

        LinksEntity link = HibernateRequests.getLinkByID(linkID);
        RealFilesEntity realFiles = HibernateRequests.getFileByID(link.getFileId());

        String textLink = String.format("%d~%s", realFiles.getFileId(), link.getLinkName());
        try {
            return CryptoService.getService().encryptString(textLink);
        } catch (Exception e) {
            throw new ServerCloudException("Не удалось сформировать крипто ссылку");
        }

    }

    /**
     * расшифровывает строку в строку с данными о файле (id файла и имя ссылки)
     * @param link - строковое зашифрованное сообщение
     * @return String - расшифрованное сообщение в виде строки с данными
     * @throws ServerCloudException - ошибка выполнения операции расшифровки
     */
    public static String decryptLink(String link) throws ServerCloudException {
        return CryptoService.getService().decryptString(link);
    }

    /**
     * подготавливает и отправляет запрос на запись в БД новой ссылки на файл
     * @param decryptLink - строка с данными из ссылки
     * @param currentDirectory - текущая виртуальная директория пользователя
     * @throws ServerCloudException - ошибка выполнения операции
     */
    public static void addLinkByCryptoString(String decryptLink, DirectoriesEntity currentDirectory) throws ServerCloudException {
        String[] tokens = decryptLink.split("~");
        int fileID = Integer.parseInt(tokens[0]);
        String linkName = tokens[1];
        HibernateRequests.createLinkToFile(linkName, fileID, currentDirectory.getDirId());
    }


}
