package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import server.AuthService;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.UsersEntity;

@Slf4j
public class DBConnector implements AuthService {

    public DBConnector() {

    }

    /*
    * ============================ AUTH SERVICE =========================
     */

    @Override
    public void registration(String login, String password) throws RuntimeException{

        if (!HibernateRequests.isUserAlreadyExists(login)) {
            log.debug("Пользователь с таким именем в базе не найден!");
            int id = HibernateRequests.addNewUser(login, password);
            log.debug("Зарегестрирован новый пользователь! ID = " + id);
            //получить ID нового пользователя и подготовить его стартовую директорию в БД
            HibernateRequests.createUserHomeDir(id, login);
            log.debug("Добавлена в БД ссылка на стартовую директорию пользователя.");

        } else {
            log.debug("Пользователь с таким именем уже существует!");
            throw new RuntimeException("Пользователь с таким именем уже существует!");
        }

    }


    @Override
    public int authentication(String login, String password) {
        if (HibernateRequests.isUserAlreadyExists(login)) {
            log.debug("Пользователь существует - проверяем пароль");

            int userId = HibernateRequests.getUserID(login, password);
            if (userId==Integer.MIN_VALUE) {
                log.debug("Имя или пароль пользователя не верны!");
                throw new RuntimeException("Имя или пароль пользователя не верны! Попробуйте ещё");
            } else {
                log.debug(String.format("ID пользователя = %s. Введённые данные верны", userId));
                return userId;
            }

        } else {
            log.debug("Пользователь с таким именем не найден в базе");
            throw new RuntimeException("Пользователь с таким именем не найден в базе");
        }
    }

    /*
     * ============================ SERVER COMMANDS SERVICE =========================
     */

    public static DirectoriesEntity getUserHomeDir(int userID) throws RuntimeException{
        return HibernateRequests.getUserHomeDir(userID);
    }

}
