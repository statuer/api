package fr.lefuturiste.statuer.controllers;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.DiscordContext;
import fr.lefuturiste.statuer.models.Incident;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.NamespaceStore;
import fr.lefuturiste.statuer.stores.ProjectStore;
import fr.lefuturiste.statuer.stores.QueryStore;
import fr.lefuturiste.statuer.stores.ServiceStore;
import net.dv8tion.jda.core.EmbedBuilder;
import org.hibernate.validator.internal.util.logging.formatter.DurationFormatter;
import org.json.JSONObject;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordCommandsController {

    public static Project queryProject(String[] pathDecomposed, Namespace namespace, DiscordContext context) {
        Project project = null;
        if (pathDecomposed.length >= 2)
            project = ProjectStore.getOneBySlugAndByNamespace(pathDecomposed[1], namespace);
        if (project == null && pathDecomposed.length >= 2)
            context.warn("Invalid path: project not found");
        if (pathDecomposed.length > 3)
            context.warn("Invalid path: a path cannot have more than 3 parts");
        return project;
    }

    public static void ping(DiscordContext context) {
        context.respond("Pong!");
    }

    public static void debug(DiscordContext context) {
        StringBuilder output = new StringBuilder("```\n");
        for (String commandComponent : context.getPartsAsArray())
            output.append(commandComponent).append("\n");
        output.append("```");
        context.respond(output.toString());
    }

    public static void help(DiscordContext context) {
        context.respondEmbed(new EmbedBuilder()
                .setTitle(":interrobang: Commands available")
                .setColor(context.INFO_COLOR)
                .addField("??create <path>", "Create all entities of the path", false)
                .addField("??edit <path> key1=value1 key2=value2", "Edit a path with key=value syntax", false)
                .addField("??delete <path>", "Recursively delete a path", false)
                .addField("??get <path>", "Get path's data, use get-json to get JSON instead of a embed", false)
                .addField("??status <namespace>", "Get overall status of all the services inside the namespace", false)
                .addField("??incidents <service-path>", "Get all the last 90 days incidents for the path", false)
                .addField("??refresh <service-uuid>", "Force the execution of a check on a service", false)
                .addField("??about", "Get bot's meta data", false)
                .addField("??ping", "Ping bot", false)
        );
    }

    public static void about(DiscordContext context) {
        long upTime = App.getUpTime().getSeconds();
        String upTimeHuman = String.format("%d:%02d:%02d", upTime / 3600, (upTime % 3600) / 60, (upTime % 60));
        context.respondEmbed(new EmbedBuilder()
                .setTitle("About statuer")
                .setThumbnail("https://raw.githubusercontent.com/statuer/api/master/UpDownBot.png")
                .setColor(context.INFO_COLOR)
                .addField("Version", "v1.0", false)
                .addField("Developer", "<@169164454255263745>", true)
                .addBlankField(true)
                .addField("Uptime", upTimeHuman, true)
                .addField("Logo made by", "<@287902677348777985>", true)
                .addField("GitHub", "https://github.com/lefuturiste/statuer-api", true));
    }

    public static void get(DiscordContext context) {
        if (context.getParts().size() == 1) {
            context.warn("Usage: get <path>, use get-json to get JSON instead of a embed");
            return;
        }
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
                context.respond(":paperclip: Because discord won't let me send a 2000+ chars message, I had to send you a happy little file!");
                context.getEvent().getChannel().sendFile(jsonFormatted.getBytes(), "data.json").complete();
            } else {
                context.respond(message);
            }
            return;
        }
        switch (pathDecomposed.length) {
            case 1: // show namespace details
                builder.setTitle(namespace.getLabel())
                        .setDescription("A Statuer's namespace")
                        .setColor(context.ERROR_COLOR)
                        .setThumbnail(namespace.getImageUrl())
                        .addField("#uuid", namespace.getId(), false)
                        .addField("Discord webhook", namespace.getDiscordWebhook() == null ? "None" : namespace.getHidedDiscordWebpack(), false)
                        .addField("Project count", String.valueOf(namespace.getProjects().size()), true)
                        .addField("Projects", String.join(", ",
                                namespace.getProjects().stream()
                                        .map(Project::getSlug)
                                        .collect(Collectors.joining(", "))),
                                true);
                break;
            case 2: // search for a project
                builder.setTitle(project.getLabel())
                        .setDescription("A Statuer's project")
                        .setColor(context.ERROR_COLOR)
                        .setThumbnail(project.getImageUrl())
                        .addField("#uuid", project.getId(), false)
                        .addField("Path", project.getPath(), true)
                        .addField("Services count", String.valueOf(project.getServices().size()), true)
                        .addField("Services", String.join(", ",
                                project.getServices().stream()
                                        .map(Service::getSlug)
                                        .collect(Collectors.joining(", "))),
                                false);
                break;
            case 3: // search for a service
                String formattedStatus = "";
                if (service.getStatus().equals("up"))
                    formattedStatus += ":white_check_mark: ";
                else if (service.getStatus().equals("down"))
                    formattedStatus += ":red_circle: ";
                formattedStatus += service.getStatus().substring(0, 1).toUpperCase() + service.getStatus().substring(1);
                builder.setTitle(service.getPath())
                        .setDescription("A Statuer's service")
                        .setColor(context.ERROR_COLOR)
                        .addField("#uuid", service.getId(), false)
                        .addField("Check period",
                                new DurationFormatter(Duration.ofSeconds(service.getCheckPeriod())).toString(), true)
                        .addField("Url", service.getUrl() == null ? "None" : service.getUrl(), true)
                        .addField("Type", service.getType() == null ? "None" : service.getType(), true)
                        .addField("Timeout", new DurationFormatter(Duration.ofSeconds(service.getTimeout())).toString(), true)
                        .addField("Status",
                                service.getStatus() != null ? formattedStatus : "None", true)
                        .addField("Last incident", service.getLastIncidentDate(), true)
                        .addField("Incidents", service.getIncidents().size() == 0 ? "None" : String.valueOf(service.getIncidents().size()), true)
                        .addField("Uptime (last 90 days)", service.getUptime() + " %", true);

        }
        context.respondEmbed(builder);
    }

    public static void create(DiscordContext context) {
        if (context.getParts().size() == 1) {
            context.warn("Usage: create <path>");
            return;
        }
        QueryStore.ObjectQueryResult objectQueryResult = QueryStore.getObjectsFromQuery(context.getParts().get(1));

        if (objectQueryResult == null) {
            context.warn("Invalid Path");
            return;
        }

        int createdCount = 0;
        Namespace namespace;
        if (objectQueryResult.namespace == null) {
            // create that namespace
            namespace = new Namespace();
            namespace.setId(UUID.randomUUID().toString());
            namespace.setSlug(objectQueryResult.namespaceSlug);
            NamespaceStore.persist(namespace);
            createdCount++;
        } else {
            namespace = objectQueryResult.namespace;
        }
        if (objectQueryResult.projectSlug != null) {
            Project project;
            if (objectQueryResult.project == null) {
                project = new Project();
                project.setId(UUID.randomUUID().toString());
                project.setSlug(objectQueryResult.projectSlug);
                project.setNamespace(namespace);
                ProjectStore.persist(project);
                createdCount++;
            } else {
                project = objectQueryResult.project;
            }
            if (objectQueryResult.serviceSlug != null) {
                Service service;
                if (objectQueryResult.service == null) {
                    service = new Service();
                    service.setId(UUID.randomUUID().toString());
                    service.setSlug(objectQueryResult.serviceSlug);
                    service.setProject(project);
                    ServiceStore.persist(service);
                    App.notifyUpdateOnService();
                    createdCount++;
                }
            }
        }
        context.success("Entities created: " + createdCount);
    }

    public static void edit(DiscordContext context) {
        String usage = "Usage: edit <path> key=value key1=value1 ...";
        if (context.getParts().size() <= 2) {
            context.usageString(usage);
            return;
        }
        QueryStore.ObjectQueryResult objectQueryResult = QueryStore.getObjectsFromQuery(context.getParts().get(1));

        if (objectQueryResult == null) {
            context.warn("Invalid path");
            return;
        }
        Map<String, String> parameters = new HashMap<>();
        for (String component : Arrays.copyOfRange(context.getPartsAsArray(), 2, context.getParts().size())) {
            String[] parameterComponents = component.split("=");
            if (parameterComponents.length == 2)
                parameters.put(parameterComponents[0], parameterComponents[1]);
        }
        if (parameters.size() == 0) {
            context.usageString(usage);
            return;
        }
        if (objectQueryResult.service != null) {
            // edit service
            if (parameters.containsKey("slug")) {
                objectQueryResult.service.setSlug(parameters.get("slug"));
            }
            if (parameters.containsKey("check_period") || parameters.containsKey("period")) {
                String rawPeriod = parameters.containsKey("check_period") ? parameters.get("check_period") : parameters.get("period");
                int period = 0;
                String[] periodComponents = rawPeriod.split(" ");
                for (String component : periodComponents) {
                    if (component.contains("s"))
                        period += Integer.parseInt(component.replace("s", ""));
                    if (component.contains("m"))
                        period += Integer.parseInt(component.replace("m", "")) * 60;
                    if (component.contains("h"))
                        period += Integer.parseInt(component.replace("h", "")) * 3600;
                }
                objectQueryResult.service.setCheckPeriod(period);
            }
            if (parameters.containsKey("url")) {
                objectQueryResult.service.setUrl(parameters.get("url"));
            }
            ServiceStore.persist(objectQueryResult.service);
            App.notifyUpdateOnService();
        } else if (objectQueryResult.project != null) {
            // edit project
            if (parameters.containsKey("slug"))
                objectQueryResult.project.setSlug(parameters.get("slug"));
            if (parameters.containsKey("name"))
                objectQueryResult.project.setName(parameters.get("name"));
            if (parameters.containsKey("imageUrl"))
                objectQueryResult.project.setImageUrl(parameters.get("imageUrl"));
            ProjectStore.persist(objectQueryResult.project);
        } else if (objectQueryResult.namespace != null) {
            // edit namespace
            if (parameters.containsKey("slug"))
                objectQueryResult.namespace.setSlug(parameters.get("slug"));
            if (parameters.containsKey("name"))
                objectQueryResult.namespace.setName(parameters.get("name"));
            if (parameters.containsKey("discordWebhook"))
                objectQueryResult.namespace.setDiscordWebhook(parameters.get("discordWebhook"));
            if (parameters.containsKey("imageUrl"))
                objectQueryResult.namespace.setImageUrl(parameters.get("imageUrl"));
            NamespaceStore.persist(objectQueryResult.namespace);
        } else {
            context.warn("Invalid path: entity not found");
            return;
        }

        context.success();
    }

    public static void delete(DiscordContext context) {
        if (context.getParts().size() == 1) {
            context.warn("Usage: delete <path>");
            return;
        }
        String[] pathDecomposed = context.getParts().get(1).split("\\.");
        Namespace namespace = NamespaceStore.getOneBySlug(pathDecomposed[0]);
        if (namespace == null) {
            context.warn("Invalid path: namespace not found");
            return;
        }
        Project project = queryProject(pathDecomposed, namespace, context);
        if (project == null)
            return;
        int deletedCount = 0;
        switch (pathDecomposed.length) {
            case 1:
                deletedCount = NamespaceStore.delete(namespace);
                break;
            case 2:
                deletedCount = ProjectStore.delete(project);
                break;
            case 3:
                Service service = ServiceStore.getOneBySlugAndByProject(pathDecomposed[2], project);
                if (service == null) {
                    context.warn("Invalid path: service not found");
                    return;
                }
                deletedCount = ServiceStore.delete(service);
        }
        context.success("Entities deleted: " + deletedCount);
    }

    public static void status(DiscordContext context) {
        if (context.getParts().size() == 1) {
            context.warn("Usage: status <namespace>");
            return;
        }
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

    public static void incidents(DiscordContext context) {
        if (context.getParts().size() == 1) {
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
        description
                .append("Since 90 days, we recorded **")
                .append(service.getIncidents().size() == 0 ? "zero" : String.valueOf(service.getIncidents().size()))
                .append("** incidents on this service. ")
                .append("And with a availability of **")
                .append(service.getUptime())
                .append("** % \n");
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.US)
                .withZone(ZoneId.systemDefault());
        for (Incident incident : service.getIncidents()) {
            description
                    .append(formatter.format(incident.getStartedAt()))
                    .append(" - ")
                    .append(incident.getFinishedAt() == null ? "*Now*" : formatter.format(incident.getFinishedAt()))
                    .append(" - ");

            if (incident.getFinishedAt() == null) {
                description.append("Ongoing incident");
            } else {
                description
                        .append("Last for: ")
                        .append(new DurationFormatter(Duration.between(incident.getStartedAt(), incident.getFinishedAt())).toString());
            }

            description
                    .append("\n");
        }
        builder.setTitle("Incident on " + service.getPath())
                .setDescription(description);

        context.respondEmbed(builder);
    }

    public static void refresh(DiscordContext context) {
        if (context.getParts().size() == 1) {
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
            context.success("The service was checked and we detected a change in the status, now it's " + service.getStatus() + ".");
        else
            context.success("The service was checked but we didn't detected any changes in the status, it's " + service.getStatus() + " like before.");
    }
}
