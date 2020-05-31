package fr.lefuturiste.statuer.discord.commands;

import java.util.Collections;
import java.util.UUID;

import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.ServiceStore;

public class ResetCommandController extends CommandController {

  public static void reset(Context context) {
    Service service = ServiceStore.getOne(UUID.fromString(context.getParts().get(1)));
    if (service == null) {
        context.warn("Invalid UUID: service not found, make sure to use a UUID and not a path!");
        return;
    }
    int incidentCount = service.getIncidents().size();
    service.setIncidents(Collections.emptySet());

    service.setUptime(0);
    service.setStatus(null);
    service.setLastCheckAt(null);
    service.setLastDownAt(null);

    context.success("Successfully deleted all " + incidentCount + " incidents. The fields uptime, lastCheckAt, lastDownAt and status were also reseted to their original values.");
  }
}
