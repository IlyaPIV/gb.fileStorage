package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import server.hibernate.entity.DirectoriesEntity;
import server.hibernate.entity.LinksEntity;
import server.hibernate.entity.RealFilesEntity;
import server.hibernate.entity.UsersEntity;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HibernateSessionFactoryUtil {

    private static SessionFactory sessionFactory;

    private HibernateSessionFactoryUtil() {}

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration configuration = new Configuration().configure("hibernate.cfg.xml");
//                configuration.addAnnotatedClass(DirectoriesEntity.class);
//                configuration.addAnnotatedClass(LinksEntity.class);
//                configuration.addAnnotatedClass(RealFilesEntity.class);
//                configuration.addAnnotatedClass(UsersEntity.class);

                StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
                                                            .applySettings(configuration.getProperties());


                sessionFactory = configuration.buildSessionFactory(builder.build());

//                sessionFactory = new Configuration().configure().buildSessionFactory();
                log.debug("Создана Hibernate session factory");
            } catch (Exception e) {
               log.error("Ошибка создания Hibernate session factory: "+e.getMessage());
            }
        }
        return sessionFactory;
    }
}
