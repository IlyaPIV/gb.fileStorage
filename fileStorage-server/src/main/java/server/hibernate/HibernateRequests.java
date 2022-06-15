package server.hibernate;

import org.hibernate.Session;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.UsersEntity;
import java.util.List;

public class HibernateRequests {


    /*
     * ================   USERS  ================
     */

    /**
     * поиск существующей записи в БД по имени пользователя
     * @param login - строковое значение с именем пользователя
     * @return - ссылка на запись в БД (NULL в случае отстуствии записей)
     */
    public static boolean isUserAlreadyExists(String login) throws RuntimeException{
        try (Session session = HibernateUtil.getSession()){
            session.beginTransaction();
            List<UsersEntity> users = session.createQuery(queryUsersFindByLogin(), UsersEntity.class)
                                        .setParameter("paramLogin", login)
                                        .list();
            session.getTransaction().commit();
            return users.size()!=0;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить запрос к БД!");
        }
    }

    /**
     * создаёт и добавляет в БД нового пользователя с заданными параметрами:
     * @param login - имя пользователя
     * @param password - пароль пользователя
     * @throws RuntimeException - ошибка создания
     */
    public static int addNewUser(String login, String password) throws RuntimeException{
        try (Session session = HibernateUtil.getSession()){
            session.beginTransaction();
            UsersEntity newUser = new UsersEntity(login, password);
            session.persist(newUser);
            session.getTransaction().commit();

            return newUser.getUserId();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось записать пользователя в БД!");
        }
    }

    /**
     * выполняет поиск в таблице пользователей с отбором по логину и паролю
     * @param login - имя пользователя
     * @param password - пароль пользователя
     * @return - ID пользователя (если данные верны)
     * MIN_VALUE - если запись с такими данными не найдена
     * @throws RuntimeException - если не удалось выполнить запрос
     */
    public static int getUserID(String login, String password) throws RuntimeException {
        try (Session session = HibernateUtil.getSession()){
            session.beginTransaction();
            List<UsersEntity> users = session.createQuery(queryUsersFindByLoginAndPass(), UsersEntity.class)
                    .setParameter("paramLogin", login)
                    .setParameter("paramPass", password)
                    .list();
            session.getTransaction().commit();
            if (users.size()==0) return Integer.MIN_VALUE;
                else return users.get(0).getUserId();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить запрос к БД!");
        }
    }

    /**
     * создание текста запроса на поиск пользователя по логину
     * @return строку с текстом запроса к БД на PostgresSQL
     */
    private static String queryUsersFindByLogin(){
        return "FROM UsersEntity WHERE login = :paramLogin";
    }

    /**
     * создание текста запроса на поиск пользователя по логину и паролю
     * @return строку с текстом запроса к БД на PostgresSQL
     */
    private static String queryUsersFindByLoginAndPass(){
        return "FROM UsersEntity WHERE login = :paramLogin and password = :paramPass";
    }


    /*
     * ================   DIRECTORIES  ================
     */

    /**
     * добавляет в БД стартовую директорию пользователя
     * @param userID - id пользователя
     * @param login - имя пользователя - используется для имени стартовой директории
     */
    public static void createUserHomeDir(int userID, String login) {
        try (Session session = HibernateUtil.getSession()) {
            session.beginTransaction();
            session.persist(new DirectoriesEntity(userID, login));
            session.getTransaction().commit();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить запрос к БД!");
        }
    }

    /**
     * возвращает ссылку на стартовую директорию пользователя
     * @param userID - id пользователя
     * @return - ссылка на запись БД
     * @throws RuntimeException - в случае если кол-во записей в результате запроса не равно 1
     */
    public static DirectoriesEntity getUserHomeDir(int userID) throws RuntimeException {
        try (Session session = HibernateUtil.getSession()){
            session.beginTransaction();
            List<DirectoriesEntity> list = session.createQuery(queryDirectoriesFindByUser(), DirectoriesEntity.class)
                            .setParameter("userID", userID)
                            .list();
            session.getTransaction().commit();
            if (list.size()!=1) throw new RuntimeException("Ошибка при получении данных их БД - не найдена запись");
                else return list.get(0);
        }
    }

    /**
     * формирует текст запроса поиска записи родительского каталога пользователя
     * @return - String - текст запроса
     */
    private static String queryDirectoriesFindByUser() {
        return "FROM DirectoriesEntity WHERE userId = :userID and parentDir = null";
    }
}
