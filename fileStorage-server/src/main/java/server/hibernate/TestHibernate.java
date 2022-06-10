package server.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import server.hibernate.entity.UsersEntity;

public class TestHibernate {
    public static void main(String[] args) {



        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();

        DBConnector connectorDB = new DBConnector();

        session.get(UsersEntity.class, 1L);
        connectorDB.addUser("Vasia","123");
     //   System.out.println(connectorDB.findUserByLogin("Vasia"));

    }
}
