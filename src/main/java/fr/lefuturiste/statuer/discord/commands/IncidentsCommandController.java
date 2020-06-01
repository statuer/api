package fr.lefuturiste.statuer.discord.commands;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.UUID;

import fr.lefuturiste.statuer.DurationFormatter;
import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.*;
import fr.lefuturiste.statuer.stores.*;
import net.dv8tion.jda.core.EmbedBuilder;

public class IncidentsCommandController extends CommandController {

  public static void incidents(Context context) {
    if (context.getParts().size() != 2) {
      context.warn("Usage: incidents <service-path>");
      return;
    }
    String[] pathDecomposed = context.getParts().get(1).split("\\.");
    if (pathDecomposed.length != 3) {
      context.warn("Invalid service path: you must specify a project or a service");
      return;
    }
    Namespace namespace = NamespaceStore.getOneBySlug(pathDecomposed[0]);
    if (namespace == null) {
      context.warn("Invalid path: namespace not found");
      return;
    }
    Project project = ProjectStore.getOneBySlugAndByNamespace(pathDecomposed[1], namespace);
    if (project == null) {
      context.warn("Invalid path: project not found");
      return;
    }
    Service service = ServiceStore.getOneBySlugAndByProject(pathDecomposed[2], project);
    if (service == null) {
      context.warn("Invalid path: service not found");
      return;
    }
    EmbedBuilder builder = new EmbedBuilder();
    StringBuilder description = new StringBuilder();
    description.append("Since 90 days, we recorded **")
        .append(service.getIncidents().size() == 0 ? "zero" : String.valueOf(service.getIncidents().size()))
        .append("** incidents on this service. ").append("And with a availability of **").append(service.getUptime())
        .append("** % \n");
    DateTimeFormatter formatter = DateTimeFormatter
      .ofLocalizedDateTime(FormatStyle.SHORT)
      .withLocale(Locale.US)
      .withZone(ZoneId.systemDefault());
    for (Incident incident : service.getIncidents()) {
      description.append(formatter.format(incident.getStartedAt())).append(" - ")
          .append(incident.getFinishedAt() == null ? "*Now*" : formatter.format(incident.getFinishedAt())).append(" - ")
          .append(incident.getReason() == null ? "null" : incident.getReason().getMessage()).append(" - ");

      if (incident.getFinishedAt() == null) {
        description.append("Ongoing incident");
      } else {
        description
          .append("Last for: ")
          .append(DurationFormatter.format(Duration.between(incident.getStartedAt(), incident.getFinishedAt())));
      }

      description.append("\n");
    }
    builder.setTitle("Incident on " + service.getPath()).setDescription(description);

    context.respondEmbed(builder);
  }

  public static void incident(Context context) {

    Incident incident = IncidentStore.getOne(UUID.fromString(context.getPart(1)));
    if (incident == null) {
      context.warn("Unknown UUID");
      return;
    }
    EmbedBuilder builder = new EmbedBuilder()
      .setTitle("Detail for incident #" + context.getPart(1))
      .setDescription("Hope it is fixed!")
      .addField("Service path", incident.getService().getPath(), true)
      .addField("Service url", incident.getService().getUrl(), true)
      .addBlankField(true)
      .addField("Started at", incident.getStartedAt().toString(), true)
      .addField("Finished at", incident.getFinishedAt() == null ? "Ongoing": incident.getFinishedAt().toString(), true)
      .addBlankField(true)
      .addField("Name", incident.getName() == null ? "None" : incident.getName(), true)
      .addField("Impact", incident.getImpact() == null ? "None" : incident.getImpact(), true)
      .addBlankField(true)
      .addField("Description", incident.getDescription() == null ? "None" : incident.getDescription(), false)
      .addField("Reason code", incident.getReason() == null ? "None" : incident.getReason().getCode(), false)
      .addField("Reason message", incident.getReason() == null ? "None" : incident.getReason().getMessage(), false)
      .addField("Debug", (incident.getReason() == null || incident.getReason().getDebug() == null) ? "None" : incident.getReason().getDebug(), false);

    context.respondEmbed(builder);
  }
}