package ginf.demosalle.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Singleton SessionFactory Hibernate.
 * Initialisé une seule fois au démarrage de l'application.
 */
public class HibernateUtil {

    private static final SessionFactory SESSION_FACTORY = build();

    private HibernateUtil() {}

    private static SessionFactory build() {
        try {
            return new Configuration()
                    .configure("hibernate.cfg.xml")
                    .buildSessionFactory();
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(
                    "Échec initialisation Hibernate : " + ex.getMessage()
            );
        }
    }

    public static SessionFactory getSessionFactory() { return SESSION_FACTORY; }

    public static void shutdown() {
        if (SESSION_FACTORY != null && !SESSION_FACTORY.isClosed()) {
            SESSION_FACTORY.close();
        }
    }
}