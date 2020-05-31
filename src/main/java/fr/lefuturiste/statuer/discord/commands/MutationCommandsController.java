package fr.lefuturiste.statuer.discord.commands;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.models.Service;
import fr.lefuturiste.statuer.stores.NamespaceStore;
import fr.lefuturiste.statuer.stores.ProjectStore;
import fr.lefuturiste.statuer.stores.QueryStore;
import fr.lefuturiste.statuer.stores.ServiceStore;
import java.util.*;

public class MutationCommandsController extends CommandController {

  public static void create(Context context) {
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

  public static void update(Context context) {
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
        String rawPeriod = parameters.containsKey("check_period") ? parameters.get("check_period")
            : parameters.get("period");
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

  public static void delete(Context context) {
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

}
