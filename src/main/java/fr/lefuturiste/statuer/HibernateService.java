package fr.lefuturiste.statuer;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import javax.persistence.EntityManager;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

public class HibernateService {

    private static EntityManager entityManager = null;
    private static String connectionUrl;
    private static String username;
    private static String password;

    public static void setConfig(String connectionUrl, String username, String password) {
        HibernateService.connectionUrl = connectionUrl + "?autoReconnect=true&useSSL=false";
        HibernateService.username = username;
        HibernateService.password = password;
    }

    public static EntityManager getEntityManager() {
        if (entityManager == null) {
            Configuration configuration = new Configuration().configure();
            App.logger.info("Using connection url " + connectionUrl);
            App.logger.info("Using connection username " + username);
            App.logger.info("Using connection username " + password.substring(0, 1) + password.substring(1).replaceAll("[a-zA-Z0-9]", "*"));
            configuration.setProperty("hibernate.connection.url", connectionUrl);
            configuration.setProperty("hibernate.connection.username", username);
            configuration.setProperty("hibernate.connection.password", password);
            String debugConfig = "Hibernate settings " +
                    configuration.getProperty("connection.url") +
                    " " +
                    configuration.getProperty("hibernate.connection.username") +
                    " " +
                    configuration.getProperty("hibernate.connection.password");
            App.logger.debug(debugConfig);
            SessionFactory sessionFactory = configuration.buildSessionFactory();
            entityManager = sessionFactory.createEntityManager();
        }
        return entityManager;
    }

    private static void refreshConnexion()
    {
        EntityManager entitymanager = getEntityManager();
        entitymanager.createQuery("select 1 from Namespace");
    }

    public static void launchConnexionFailurePreventerUtil()
    {
        System.out.println("launchConnexionFailurePreventerUtil called");
        long duration = Duration.ofMinutes(5).toMillis();
        System.out.println("launchConnexionFailurePreventerUtil with duration " + String.valueOf(duration));
        Timer timer = new Timer();
        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                System.out.println("REFRESH_CONNEXION");
                System.out.println("REFRESH_CONNEXION_DATABASE");
                System.out.println("RECONNECTION");
                HibernateService.refreshConnexion();
            }
        }, 0, duration);
    }
}
