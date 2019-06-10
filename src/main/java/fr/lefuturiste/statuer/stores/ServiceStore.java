package fr.lefuturiste.statuer.stores;

import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.UUID;

import static fr.lefuturiste.statuer.HibernateService.getEntityManager;

public class ServiceStore {
    public static List<Service> getMany() {
        EntityManager entitymanager = getEntityManager();
        return entitymanager.createQuery("from Service", Service.class).getResultList();
    }

    public static Service getOne(UUID uuid) {
        EntityManager entitymanager = getEntityManager();
        try {
            return entitymanager
                    .createQuery("from Service where id = :id", Service.class)
                    .setParameter("id", uuid.toString())
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public static void persist(Service service) {
        persist(service, true);
    }

    public static void persist(Service service, boolean clear) {
        EntityManager entitymanager = getEntityManager();
        entitymanager.getTransaction().begin();
        entitymanager.persist(service);
        entitymanager.getTransaction().commit();
        if (clear) {
            entitymanager.clear();
        }
    }

    public static void delete(Service service) {
        EntityManager entitymanager = getEntityManager();
        entitymanager.getTransaction().begin();
        entitymanager.remove(service);
        entitymanager.getTransaction().commit();
        entitymanager.clear();
    }

    public static Service getOneByNameAndByProject(String name, Project project) {
        EntityManager entitymanager = getEntityManager();
        try {
            return entitymanager
                    .createQuery("from Service where name = :name and project = :project", Service.class)
                    .setParameter("name", name)
                    .setParameter("project", project)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
