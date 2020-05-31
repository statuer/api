package fr.lefuturiste.statuer.discord.commands;

import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.discord.commands.CommandController;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.NamespaceStore;
import net.dv8tion.jda.core.EmbedBuilder;

public class StatusCommandController extends CommandController {

  public static void status(Context context) {
    Namespace namespace = NamespaceStore.getOneBySlug(context.getParts().get(1));
    if (namespace == null) {
      context.warn("Invalid path: namespace not found");
      return;
    }
    // we will iterates in all the project, then in all the services
    EmbedBuilder builder = new EmbedBuilder();
    builder.setTitle("Current status of " + namespace.getLabel());
    boolean hasIssues = false;
    StringBuilder description = new StringBuilder();
    for (Project project : namespace.getProjects()) {
      for (Service service : project.getServices()) {
        String status = "";
        if (service.isAvailable() == null) {
          status += ":question:";
        } else if (service.isAvailable()) {
          status += ":white_check_mark: ";
        } else if (!service.isAvailable()) {
          status += ":red_circle:";
          hasIssues = true;
        }
        String name = service.getProject().getSlug() + "." + service.getSlug();
        description.append(status).append(" — **").append(name).append("** \n");
      }
      if (namespace.getProjects().size() > 1)
        description.append("\n");
    }
    if (hasIssues) {
      builder.setColor(context.ERROR_COLOR);
    } else {
      builder.setColor(context.SUCCESS_COLOR);
    }
    builder.setDescription(description.toString());
    context.respondEmbed(builder);
  }
}