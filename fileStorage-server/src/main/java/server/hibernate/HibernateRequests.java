package server.hibernate;

import org.hibernate.Session;
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
            List<UsersEntity> users = session.createQuery(queryFindByLogin(), UsersEntity.class)
                                        .setParameter("paramLogin", login)
                                        .list();
            session.getTransaction().commit();
            return users.size()!=0;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить запрос к БД!");
        }
    }


    /**
     * создание текста запроса на поиск пользователя по логину
     * @return строку с текстом запроса к БД на PostgresSQL
     */
    private static String queryFindByLogin(){
        return "FROM UsersEntity WHERE login = :paramLogin";
    }

    /**
     * создаёт и добавляет в БД нового пользователя с заданными параметрами:
     * @param login - имя пользователя
     * @param password - пароль пользователя
     * @throws RuntimeException - ошибка создания
     */
    public static void addNewUser(String login, String password) throws RuntimeException{
        try (Session session = HibernateUtil.getSession()){
            session.beginTransaction();
            session.persist(new UsersEntity(login, password));
            session.getTransaction().commit();
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
            List<UsersEntity> users = session.createQuery(queryFindByLogin(), UsersEntity.class)
                    .setParameter("paramLogin", login)
                    .list();
            session.getTransaction().commit();
            if (users.size()==0) return Integer.MIN_VALUE;
                else return users.get(0).getUserId();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить запрос к БД!");
        }
    }

    private static String queryFindByLoginAndPass(){
        return "FROM UsersEntity WHERE login = :paramLogin and password = :paramPass";
    }
}
