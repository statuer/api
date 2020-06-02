package fr.lefuturiste.statuer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import fr.lefuturiste.statuer.models.*;
import fr.lefuturiste.statuer.stores.*;

/**
 * Test the uptime computation logic
 */
public class UptimeTest
{
    @Before
    public void init() {
      assertTrue(App.init("test"));
    }
    
    @Test
    public void shouldReturnFullUptime()
    {
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);
      Incident incident = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(now)
        .setFinishedAt(now);
      IncidentStore.persist(incident, false);
      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident);
      assertEquals(1.00, uptime, 0.001);
    }

    @Test
    public void shouldReturnHalfUptime()
    {
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Instant startedAt = now.minus(Duration.ofDays(45));
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);
      Incident incident = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(now);
      IncidentStore.persist(incident, false);
      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident);
      assertEquals(0.5, uptime, 0.001);
    }

    @Test
    public void shouldReturnThreeFourthUptime()
    {
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Instant startedAt = now.minus(Duration.ofDays(22).plus(Duration.ofHours(12)));
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);
      Incident incident = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(now);
      IncidentStore.persist(incident, false);
      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident);
      assertEquals(0.75, uptime, 0.001);
    }

    @Test
    public void shouldReturnZeroUptime()
    {
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Instant startedAt = now.minus(Duration.ofDays(90));
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);
      Incident incident = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(now);
      IncidentStore.persist(incident, false);
      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident);
      assertEquals(0, uptime, 0.00001);
    }

    @Test
    public void shouldTakeInAccountManyIncidents()
    {
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);

      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Instant startedAt = now.minus(Duration.ofHours(55));
      Incident incident1 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(now);
      IncidentStore.persist(incident1, false);

      startedAt = now.minus(Duration.ofHours(10));
      Incident incident2 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(now);
      IncidentStore.persist(incident2, false);

      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident1);
      IncidentStore.delete(incident2);
      assertEquals(0.96990740741, uptime, 0.00001);
    }

    @Test
    public void shouldTakeInAccountIncidentThatStartedPreviously()
    {
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);

      // incident 1: lasted for 15 days but have only 5 days in the interval
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
      Instant startedAt = now.minus(Duration.ofDays(100));
      Incident incident1 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(startedAt.plus(Duration.ofDays(15)));
      IncidentStore.persist(incident1, false);

      // incident 2: lasted for 89 hours but in the middle of the 90 days interval
      startedAt = now.minus(Duration.ofDays(40));
      Incident incident2 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(startedAt.plus(Duration.ofHours(89)));
      IncidentStore.persist(incident2, false);

      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident1);
      IncidentStore.delete(incident2);

      // incident1: 5 days in the interval so 24*5 = 120
      // incident2: 89 hours in the interval
      // total: 120 + 89 = 209 hours
      // so uptime = 1 - 209/2160 = 0.90324074074074
      assertEquals(0.90324074074074, uptime, 0.00001);
    }

    @Test
    public void shouldTakeInAccountOngoingIncident()
    {
      Service service = new Service().generateId().setSlug("a");
      ServiceStore.persist(service, false);
      Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());

      // incident 1: ongoing since 250 hours so last 250 hours
      Instant startedAt = now.minus(Duration.ofHours(250));
      Incident incident1 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt);
      IncidentStore.persist(incident1, false);

      // incident 2: lasted 100 hours in the interval
      startedAt = now.minus(Duration.ofDays(20));
      Incident incident2 = new Incident()
        .generateId()
        .setService(service)
        .setStartedAt(startedAt)
        .setFinishedAt(startedAt.plus(Duration.ofHours(100)));
      IncidentStore.persist(incident2, false);

      float uptime = Uptime.computeUptime(service);
      IncidentStore.delete(incident1);
      IncidentStore.delete(incident2);

      // incident1: 250 hours, ongoing
      // incident2: 100 hours in the interval
      // total: 250 + 100 = 350 hours
      // so uptime = 1 - 350/2160 = 0.83796296296296
      assertEquals(0.83796296296296, uptime, 0.00001);
    }
}
