package fr.lefuturiste.statuer;

import fr.lefuturiste.statuer.checker.HttpChecker;
import fr.lefuturiste.statuer.models.Incident;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.notifier.DiscordNotifier;
import fr.lefuturiste.statuer.stores.IncidentStore;
import fr.lefuturiste.statuer.stores.ServiceStore;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CheckThread implements Runnable {

  private final DiscordNotifier discordNotifier;
  public volatile boolean canRun = true;

  CheckThread() {
    discordNotifier = new DiscordNotifier();
  }

  @Override
  public void run() {
    Duration sleepDuration = Duration.ofSeconds(10);
    App.logger.info("Starting check thread with sleepDuration of " + sleepDuration.toString());
    // load the services into a store in memory
    // have a clock
    // at each pulse, look in the store for each service if the time is elapsed
    // if time elapsed perform check
    // update the check time in the memory and in the db
    while (true) {
      List<Service> services = ServiceStore.getMany();
      for (Service service : services) {
        try {
          checkService(service, false);
        } catch (InvalidInspectionResultException e) {
          e.printStackTrace();
          canRun = false;
          return;
        }
      }

      try {
        Thread.sleep(sleepDuration.toMillis());
      } catch (InterruptedException e) {
        e.printStackTrace();
        canRun = false;
        return;
      }
    }
  }

  /**
   * Will check a specified service
   *
   * @param service      service to check
   * @param ignorePeriod if true, the service check period is ignored
   * @return return true if the service's status changed
   * @throws InvalidInspectionResultException
   */
  public boolean checkService(Service service, boolean ignorePeriod)
      throws InvalidInspectionResultException {
    service.inspect();
    boolean statusChanged = false;
    if (service.getUrl() == null || service.getUrl().equals("")) {
      App.logger.debug("- Skipped service " + service.getPath() + " (no url)");
      return false;
    }
    App.logger.debug("- Checking service " + service.getPath());

    int checkPeriod = service.getCheckPeriod();
    // we lower the checkperiod if the service is in the process of verifying the availability
    if (!service.isAvailable() && service.getCurrentAttempts() >= 1) {
      checkPeriod = 10;
    }
    // if the time between now and last checked at is more or equal than the time of
    // check_period go check it
    // we do a additional check to account for a null lastCheckAt field (because of a non saved lastCheckAt field)
    Instant lastCheck = service.getLastCheckAt() != null ? service.getLastCheckAt() : Instant.now().minus(Duration.ofSeconds(checkPeriod));
    Duration durationSinceLastCheck = Duration.between(lastCheck, Instant.now());
    if (durationSinceLastCheck.getSeconds() >= checkPeriod || ignorePeriod) {
      if (ignorePeriod)
        App.logger.debug("Service was forced to be checked by ignoring period");
      else
        App.logger.debug("    Not checked since: " + durationSinceLastCheck.getSeconds());
      
      // we need to check the service
      boolean isAnAttempt = false;
      Incident lastIncident = null;
      HttpChecker checker = new HttpChecker(service.getTimeout());
      boolean isAvailable = checker.isAvailable(service);
      boolean persistService = false;

      App.logger.debug("    This service has " + (service.getOngoingIncident() != null ? "at least one" : "no") + " ongoing incidents");
      App.logger.debug("    Service available: " + isAvailable);

      if ((service.isAvailable() != null && service.isAvailable() != isAvailable)
          || (service.isAvailable() == null && !isAvailable)) {
        // status has changed
        statusChanged = true;
        service.setAvailable(isAvailable);

        //App.logger.info("Status of service " + service.getPath() + " changed to " + (isAvailable ? "UP" : "DOWN"));

        if (!isAvailable) {
          App.logger.debug("    Changed as DOWN: We need to make sure before declaring a incident, the current attempt counter was reset");
          // reset current attempt counter, this is the start of a new era!
          service.setCurrentAttempts(0);
          // the status changed as down, we increment the attempt counter before actually creating a incident.
          // we also lower the service refresh rate to 20s
          // we need to lower the check period for this thing
          isAnAttempt = true;
        } else {
          App.logger.debug("    Changed as UP: End of the incident");
          //  END OF THE INCIDENT: Status as changed as UP, we can update our incident to indicate the end of it.
          lastIncident = service.getOngoingIncident();
          if (lastIncident != null)
            lastIncident.setFinishedAt(Instant.now());
          persistService = true;
        }
      }
      // if the service was resetted and we have a null isAvailable field, we set it
      if (service.isAvailable() == null) {
        service.setAvailable(isAvailable);
        persistService = true;
      }

      if (!statusChanged && !service.isAvailable() && service.getOngoingIncident() == null) {
        // if the status did not changed; the service is down  and we have NO ongoing incident
        // we increment the current attempt
        isAnAttempt = true;
      }
      
      if (isAnAttempt) {
        App.logger.debug("    Increment attempts");
        service.incrementCurrentAttempts();
        persistService = true;
        App.logger.debug("    Now, " + String.valueOf(service.getCurrentAttempts()) + " current attempts");
        // if the current attemps for the service exeed the limit we declare a incident
        if (service.getCurrentAttempts() >= service.getMaxAttempts()) {
          // status as changed as DOWN
          App.logger.debug("    Creation of a incident");
          Instant downInstant = Instant.now();
          service.setLastDownAt(downInstant).setCurrentAttempts(0);
          // reset current attempt counter to not retriger the attempt
          // we can create a incident
          lastIncident = new Incident()
            .setId(UUID.randomUUID().toString())
            .setStartedAt(downInstant)
            .setService(service)
            .setReason(checker.getReason());
          service.addIncident(lastIncident);
        }
      }

      // if the lastIncident is non null that mean that the service is:
      // either really DOWN exeeded the limit
      // OR has finished an incident
      if (lastIncident != null) {
        // We persist the last incident and we regenerate the uptime field
        IncidentStore.persist(lastIncident, false);
        App.logger.debug("    Persisted incident " + lastIncident.getId());
        service.setUptime(Uptime.computeUptime(service));
        App.logger.debug("    Uptime computed to " + String.valueOf(service.getUptime()));
        // we can now notify of the incident (updated or created)
        discordNotifier.notify(lastIncident);
        persistService = true;
      }

      // we update the last check at status for local purpose
      service.setLastCheckAt(Instant.now());

      // we only need to persist the service if : a incident was created OR a incident is over
      if (persistService) {
        ServiceStore.persist(service, false);
        App.logger.debug("    Service persisted");
      }
    } else {
      App.logger.debug("    Service already checked");
    }
    return statusChanged;
  }
}
