package fr.lefuturiste.statuer.controllers;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.DiscordContext;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.NamespaceStore;
import fr.lefuturiste.statuer.stores.ProjectStore;
import fr.lefuturiste.statuer.stores.QueryStore;
import fr.lefuturiste.statuer.stores.ServiceStore;
import net.dv8tion.jda.core.EmbedBuilder;
import org.hibernate.validator.internal.util.logging.formatter.DurationFormatter;

import java.awt.*;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordCommandsController {

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
                .setColor(Color.decode("#2980b9"))
                .addField("??create <path>", "Create all entities of the path", false)
                .addField("??edit <path> key1=value1 key2=value2", "Edit a path with key=value syntax", false)
                .addField("??delete <path>", "Recursively delete a path", false)
                .addField("??get <path>", "Get path's data", false)
                .addField("??about", "Get bot's meta data", false)
                .addField("??ping", "Ping bot", false)
        );
    }

    public static void about(DiscordContext context) {
        long upTime = App.getUpTime().getSeconds();
        String upTimeHuman = String.format("%d:%02d:%02d", upTime / 3600, (upTime % 3600) / 60, (upTime % 60));
        context.respondEmbed(new EmbedBuilder()
                .setTitle("About statuer")
                .setColor(Color.decode("#2980b9"))
                .addField("Version", "v1.0", false)
                .addField("Developer", "<@169164454255263745>", true)
                .addBlankField(true)
                .addField("Uptime", upTimeHuman, true)
                .addField("GitHub", "https://github.com/lefuturiste/statuer-api", false));
    }

    public static void get(DiscordContext context) {
        if (context.getParts().size() == 1) {
            context.warn("Usage: get <path>");
            return;
        }
        String[] pathDecomposed = context.getParts().get(1).split("\\.");
        Namespace namespace = NamespaceStore.getOneBySlug(pathDecomposed[0]);
        if (namespace == null) {
            context.warn("Invalid path: namespace not found");
            return;
        }
        EmbedBuilder builder = new EmbedBuilder();
        Project project = null;
        if (pathDecomposed.length >= 2)
            project = ProjectStore.getOneBySlugAndByNamespace(pathDecomposed[1], namespace);
        if (project == null && pathDecomposed.length >= 2) {
            context.warn("Invalid path: project not found");
            return;
        }
        if (pathDecomposed.length > 3) {
            context.warn("Invalid path: a path cannot have more than 3 parts");
            return;
        }
        switch (pathDecomposed.length) {
            case 1: // show namespace details
                builder.setTitle(namespace.getName())
                        .setDescription("A Statuer's namespace")
                        .setColor(Color.decode("#e74c3c"))
                        .setThumbnail(namespace.getImageUrl())
                        .addField("#uuid", namespace.getId(), false)
                        .addField("Discord webhook", namespace.getDiscordWebhook() == null ? "None" : namespace.getHidedDiscordWebpack(), false)
                        .addField("Project count", String.valueOf(namespace.getProjects().size()), true)
                        .addField("Projects", String.join(", ",
                                namespace.getProjects().stream()
                                        .map(Project::getName)
                                        .collect(Collectors.joining(", "))),
                                true);
                break;
            case 2: // search for a project
                assert project != null;
                builder.setTitle(project.getName())
                        .setDescription("A Statuer's project")
                        .setColor(Color.decode("#e74c3c"))
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
                Service service = ServiceStore.getOneBySlugAndByProject(pathDecomposed[2], project);
                if (service == null) {
                    context.warn("Invalid path: service not found");
                    return;
                }
                builder.setTitle(service.getPath())
                        .setDescription("A Statuer's service")
                        .setColor(Color.decode("#e74c3c"))
                        .addField("#uuid", service.getId(), true)
                        .addField("Check period",
                                new DurationFormatter(Duration.ofSeconds(service.getCheckPeriod())).toString(), true)
                        .addField("Url", service.getUrl() == null ? "None" : service.getUrl(), true)
                        .addField("Type", service.getType() == null ? "None" : service.getType(), true)
                        .addField("Status",
                                service.getStatus() != null ?
                                        service.getStatus().substring(0, 1).toUpperCase() + service.getStatus().substring(1)
                                        : "None", true)
                        .addField("Last incident", service.getLastIncident() == null ? "None" : DateTimeFormatter.ISO_INSTANT.format(service.getLastIncident().getFinishedAt()), true)
                        .addField("Uptime", String.valueOf(service.getUptime()), true);

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
                    createdCount++;
                }
            }
        }
        context.success("Entities created: " + createdCount);
    }

    public static void edit(DiscordContext context) {
        if (context.getParts().size() <= 2) {
            context.usageString("Usage: edit <path> key=value key1=value1 ...");
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
            parameters.put(parameterComponents[0], parameterComponents[1]);
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
                        period += Integer.valueOf(component.replace("s", ""));
                    if (component.contains("m"))
                        period += Integer.valueOf(component.replace("m", "")) * 60;
                    if (component.contains("h"))
                        period += Integer.valueOf(component.replace("h", "")) * 3600;
                }
                objectQueryResult.service.setCheckPeriod(period);
            }
            if (parameters.containsKey("url")) {
                objectQueryResult.service.setUrl(parameters.get("url"));
            }
            ServiceStore.persist(objectQueryResult.service);
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
        Project project = null;
        if (pathDecomposed.length >= 2)
            project = ProjectStore.getOneBySlugAndByNamespace(pathDecomposed[1], namespace);
        if (project == null && pathDecomposed.length >= 2) {
            context.warn("Invalid path: project not found");
            return;
        }
        if (pathDecomposed.length > 3) {
            context.warn("Invalid path: a path cannot have more than 3 parts");
            return;
        }
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
}
