package server.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;


@Slf4j
public class HibernateUtil {

    private static SessionFactory sessionFactory;

    static {
        final StandardServiceRegistry registryBuilder = new StandardServiceRegistryBuilder()
                .configure()
                .build();

        try{
            sessionFactory = new MetadataSources(registryBuilder).buildMetadata().buildSessionFactory();
            log.debug("Создана Hibernate session factory");
        } catch (Exception e) {
            log.error("Ошибка создания Hibernate session factory: "+e.getMessage());
            StandardServiceRegistryBuilder.destroy(registryBuilder);
        }
    }

    private HibernateUtil() {}

    public static Session getSession(){
        return sessionFactory.openSession();
    }
}
