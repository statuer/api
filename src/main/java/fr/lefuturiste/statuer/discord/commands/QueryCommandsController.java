package fr.lefuturiste.statuer.discord.commands;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.NamespaceStore;
import fr.lefuturiste.statuer.stores.ServiceStore;
import net.dv8tion.jda.core.EmbedBuilder;
import org.hibernate.validator.internal.util.logging.formatter.DurationFormatter;
import org.json.JSONObject;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.stream.Collectors;

public class QueryCommandsController extends CommandController {

  public static void get(Context context) {
    String[] pathDecomposed = context.getParts().get(1).split("\\.");
    Namespace namespace = NamespaceStore.getOneBySlug(pathDecomposed[0]);
    if (namespace == null) {
      context.warn("Invalid path: namespace not found");
      return;
    }
    EmbedBuilder builder = new EmbedBuilder();
    Project project = queryProject(pathDecomposed, namespace, context);
    if (pathDecomposed.length >= 2 && project == null)
      return;
    Service service = null;
    if (pathDecomposed.length == 3) {
      service = ServiceStore.getOneBySlugAndByProject(pathDecomposed[2], project);
      if (service == null) {
        context.warn("Invalid path: service not found");
        return;
      }
    }
    if (context.getParts().get(0).contains("json")) {
      JSONObject jsonResponse = new JSONObject();
      if (pathDecomposed.length == 1)
        jsonResponse = namespace.toJSONObject(2);
      if (pathDecomposed.length == 2)
        jsonResponse = project.toJSONObject(1);
      if (pathDecomposed.length == 3)
        jsonResponse = service.toJSONObject(1);
      String jsonFormatted = jsonResponse.toString(4);
      String message = "```json\n" + jsonFormatted + "\n```";
      if (message.length() >= 2000) {
        context.respond(
            ":paperclip: Because discord won't let me send a 2000+ chars message, I had to send you a happy little file!");
        context.getEvent().getChannel().sendFile(jsonFormatted.getBytes(), "data.json").complete();
      } else {
        context.respond(message);
      }
      return;
    }
    switch (pathDecomposed.length) {
      case 1: // show namespace details
        builder.setTitle(namespace.getLabel()).setDescription("A Statuer's namespace").setColor(context.ERROR_COLOR)
            .setThumbnail(namespace.getImageUrl()).addField("#uuid", namespace.getId(), false)
            .addField("Discord webhook",
                namespace.getDiscordWebhook() == null ? "None" : namespace.getHidedDiscordWebpack(), false)
            .addField("Project count", String.valueOf(namespace.getProjects().size()), true).addField("Projects", String
                .join(", ", namespace.getProjects().stream().map(Project::getSlug).collect(Collectors.joining(", "))),
                true);
        break;
      case 2: // search for a project
        builder.setTitle(project.getLabel()).setDescription("A Statuer's project").setColor(context.ERROR_COLOR)
            .setThumbnail(project.getImageUrl()).addField("#uuid", project.getId(), false)
            .addField("Path", project.getPath(), true)
            .addField("Services count", String.valueOf(project.getServices().size()), true).addField("Services", String
                .join(", ", project.getServices().stream().map(Service::getSlug).collect(Collectors.joining(", "))),
                false);
        break;
      case 3: // search for a service
        String formattedStatus = "";
        if (service.getStatus() == null)
          formattedStatus = "None";
        else if (service.getStatus().equals("up"))
          formattedStatus += ":white_check_mark: ";
        else if (service.getStatus().equals("down"))
          formattedStatus += ":red_circle: ";
        if (service.getStatus() != null)
          formattedStatus += service.getStatus().substring(0, 1).toUpperCase() + service.getStatus().substring(1);
        DateTimeFormatter formatter = DateTimeFormatter
          .ofLocalizedDateTime(FormatStyle.SHORT)
          .withLocale(Locale.US)
          .withZone(ZoneId.systemDefault());
        builder.setTitle(service.getPath()).setDescription("A Statuer's service").setColor(context.ERROR_COLOR)
            .addField("#uuid", service.getId(), false)
            .addField("Check period", new DurationFormatter(Duration.ofSeconds(service.getCheckPeriod())).toString(),
                true)
            .addField("Url", service.getUrl() == null ? "None" : service.getUrl(), true)
            .addField("Type", service.getType() == null ? "None" : service.getType(), true)
            .addField("Timeout", new DurationFormatter(Duration.ofSeconds(service.getTimeout())).toString(), true)
            .addField("Status", formattedStatus, true).addField("Last down at", service.getLastDownAt() != null ? formatter.format(service.getLastDownAt()) : "None", true)
            .addField("Incidents",
                service.getIncidents().size() == 0 ? "None" : String.valueOf(service.getIncidents().size()), true)
            .addField("Uptime (last 90 days)", String.valueOf(service.getUptime()), true);
    }
    context.respondEmbed(builder);
  }

}
