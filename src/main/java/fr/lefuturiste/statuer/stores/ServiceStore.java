package fr.lefuturiste.statuer.stores;

import fr.lefuturiste.statuer.models.Incident;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;

import static fr.lefuturiste.statuer.HibernateService.getEntityManager;

public class ServiceStore extends Store {
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

    public static Service getOneBySlugAndByProject(String slug, Project project) {
        EntityManager entitymanager = getEntityManager();
        try {
            CriteriaBuilder builder = entitymanager.getCriteriaBuilder();
            CriteriaQuery<Service> query = builder.createQuery(Service.class);
            Root<Service> service = query.from(Service.class);
            query.where(builder.equal(service.get("slug"), slug), builder.equal(service.get("project"), project));

            return entitymanager.createQuery(query.select(service)).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Will delete recursively the service and its incidents
     *
     * @param service Service
     */
    public static int delete(Service service) {
        int entitiesDeleted = 0;
        entitiesDeleted += Store.delete(service);
        for (Incident incident: service.getIncidents())
            entitiesDeleted += IncidentStore.delete(incident);
        return entitiesDeleted;
    }
}
