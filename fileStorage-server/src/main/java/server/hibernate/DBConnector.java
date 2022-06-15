package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import server.AuthService;
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

    public static DirectoriesEntity getUserHomeDir(int userID) throws RuntimeException{
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


    public static void saveNewFile(String name, DirectoriesEntity dbDirectory) throws ServerCloudException {
        int idFile = HibernateRequests.createRealFileInfo(name, dbDirectory.getDirId());
        HibernateRequests.createLinkToFile(name, idFile, dbDirectory.getDirId());
    }

}
