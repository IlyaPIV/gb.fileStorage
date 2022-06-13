package server.hibernate;

import org.hibernate.Session;

import server.hibernate.entity.UsersEntity;

import java.util.List;

public class TestHibernate {
    public static void main(String[] args) {



        try (Session session = HibernateUtil.getSession()) {
//        Session session = new Configuration().configure().buildSessionFactory().getCurrentSession();

            System.out.println("============ ПОИСК: ============");
            session.beginTransaction();
            UsersEntity usersEntity = session.get(UsersEntity.class, 1L);
            System.out.println(usersEntity);



            System.out.println("============ ДОБАВЛЕНИЕ: ============");
            session.save(new UsersEntity("Petia", "pass"));


//            System.out.println("============ ВЫБОРКА: ============");
//            List<UsersEntity> users = session.createQuery(HibernateRequests.isUserAlreadyExists("Petia"),UsersEntity.class).list();
//            for (UsersEntity user:
//                 users) {
//                System.out.println(user);
//            }

            session.getTransaction().commit();
        }

    }
}
