package fr.lefuturiste.statuer;

import fr.lefuturiste.statuer.models.Incident;
import fr.lefuturiste.statuer.models.Service;
import static fr.lefuturiste.statuer.HibernateService.getEntityManager;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class Uptime {
  /**
   * Return the last 90's day uptime percentage for this service
   * 
   * @param service
   * @return float
   */
  public static float computeUptime(Service service) {
    EntityManager entitymanager = getEntityManager();
    
    // latch the duration of the last 90 days in a Duration object
    Instant startOfRange = Instant.now().minus(Duration.ofDays(90));
    Instant endOfRange = Instant.now();
    Duration rangeDuration = Duration.between(startOfRange, endOfRange);

    // fetch all the 90 days last incidents for this service
    TypedQuery<Incident> query = entitymanager
        .createQuery(
          "from Incident where startedAt > :ago or startedAt < :now and service_id = :id",
          Incident.class
        );
    query.setParameter("ago", startOfRange);
    query.setParameter("now", endOfRange);
    query.setParameter("id", service.getId());
    List<Incident> incidents = query.getResultList();

    Duration totalDownDuration = Duration.ofSeconds(0);
    for (Incident incident : incidents) {
      // for each incident, add up to the totalDownDuration, the duration of the incidents
      if (incident.getStartedAt().isBefore(startOfRange)) {
        totalDownDuration = totalDownDuration
          .plus(Duration.between(startOfRange, incident.getFinishedAt()));
      } else if (incident.getFinishedAt() == null) {
        totalDownDuration = totalDownDuration
          .plus(Duration.between(incident.getStartedAt(), Instant.now()));
      } else {
        totalDownDuration = totalDownDuration
          .plus(Duration.between(incident.getStartedAt(), incident.getFinishedAt()));
      }
    }
    // we compute the percentage using the two values
    float totalDownDurationSeconds = (float) totalDownDuration.getSeconds();
    float rangeDurationSeconds = (float) rangeDuration.getSeconds();
    // App.logger.debug("totalDownDurationSeconds: " + totalDownDurationSeconds + " - rangeDurationSeconds: "
    //     + rangeDurationSeconds);
    float percentage = (float) (1.0 - totalDownDurationSeconds / rangeDurationSeconds);
    // App.logger.debug("Percentage: " + String.valueOf(percentage));
    BigDecimal numberBigDecimal = new BigDecimal(percentage);
    numberBigDecimal = numberBigDecimal.setScale(8, RoundingMode.HALF_UP);
    // App.logger.debug("Updated uptime to: " + numberBigDecimal);
    return numberBigDecimal.floatValue();
  }
}