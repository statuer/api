package fr.lefuturiste.statuer.discord.commands;

import java.util.UUID;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.InvalidInspectionResultException;
import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.ServiceStore;

public class RefreshCommandController extends CommandController {

  public static void refresh(Context context) throws InvalidInspectionResultException {
    if (context.getParts().size() != 2) {
      context.warn("Usage: incidents <service-uuid>");
      return;
    }
    Service service = ServiceStore.getOne(UUID.fromString(context.getParts().get(1)));
    if (service == null) {
      context.warn("Invalid UUID: service not found, make sure to use a UUID and not a path!");
      return;
    }
    boolean statusUpdated = App.checkThread.checkService(service, true);
    if (statusUpdated)
      context.success(
          "The service was checked and we detected a change in the status, now it's " + service.getStatus() + ".");
    else
      context.success("The service was checked but we didn't detected any changes in the status, it's "
          + service.getStatus() + " like before.");
  }

}