package fr.lefuturiste.statuer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Before;
import org.junit.Test;

import fr.lefuturiste.statuer.models.*;
import fr.lefuturiste.statuer.stores.*;

/**
 * The goal here is to test the business logic of the checker thread
 */
public class CheckerTest 
{
    @Before
    public void init() {
      assertTrue(App.init("test"));
    }

    @Test
    public void shouldReportADownService() throws InvalidInspectionResultException
    {
      CheckThread checkThread = new CheckThread();
      // create a service
      Namespace namespace = new Namespace().generateId().setSlug("hello");
      NamespaceStore.persist(namespace, false);
      
      Project project = new Project().generateId().setSlug("hello").setNamespace(namespace);
      ProjectStore.persist(project, false);

      Service service = new Service()
        .generateId()
        .setProject(project)
        .setSlug("hello")
        .setUrl("http://localhost:3120")
        .setStatus("up")
        .setCheckPeriod(1)
        .setMaxAttempts(1);
      ServiceStore.persist(service, false);
      assertNull(service.getIncidents());
      //assertEquals(3, service.getPath().split(".").length);
      // we don't set the last checked at field so the service should be checked immidialtly
      // the service should be down as we don't start the http server on port 3120
      // the service pass from UP to DOWN:
      // because we only have 1 in max attemps we should directly
      // receive a notification
      // have the creation of a incident
      assertNotNull(checkThread);
      assertNotNull(service);
      assertTrue(checkThread.checkService(service, false));
      assertEquals(1, service.getIncidents().size());
      Incident incident = service.getIncidents().iterator().next();
      assertEquals(Instant.now().getEpochSecond(), incident.getStartedAt().getEpochSecond(), 3);
      assertNull(incident.getFinishedAt());
      assertEquals("http-checker-error", incident.getReason().getCode());
    }
}
