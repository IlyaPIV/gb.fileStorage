package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import server.hibernate.entity.UsersEntity;

import java.util.List;

@Slf4j
public class DBConnector {

    private final SessionFactory sessionFactory;

    public DBConnector() {
        this.sessionFactory = HibernateSessionFactoryUtil.getSessionFactory();
    }


    public void addUser(String login, String password) {

        Session session = sessionFactory.getCurrentSession();
        try {
            session.beginTransaction();
            UsersEntity newUser = new UsersEntity(login, password);
            log.debug(newUser.toString());
            session.save(newUser);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            session.getTransaction().commit();
        }


        //session.close();
    }

    /**
     * поиск существующей записи в БД по имени пользователя
     * @param login - строковое значение с именем пользователя
     * @return - ссылка на запись в БД (NULL в случае отстуствии записей)
     */

    public UsersEntity findUserByLogin(String login) {

        Session session = sessionFactory.getCurrentSession();

        UsersEntity userDB = null;
        try {
            session.beginTransaction();
            userDB = session.createQuery(UsersEntity.queryPostgresFindByLogin(login), UsersEntity.class)
                    .getSingleResult();
         //   List<UsersEntity> users = session.createQuery(UsersEntity.queryPostgresFindByLogin(login), UsersEntity.class).list();

        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            session.getTransaction().commit();
        }

//        session.close();

//        userDB = users.get(0);
        log.debug("Fonded user from DB = "+userDB);

        return userDB;
    }
}
