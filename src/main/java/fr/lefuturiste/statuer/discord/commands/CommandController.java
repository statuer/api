package fr.lefuturiste.statuer.discord.commands;

import fr.lefuturiste.statuer.discord.Context;
import fr.lefuturiste.statuer.models.Namespace;
import fr.lefuturiste.statuer.models.Project;
import fr.lefuturiste.statuer.stores.ProjectStore;

public class CommandController {
  
  public static Project queryProject(String[] pathDecomposed, Namespace namespace, Context context) {
    Project project = null;
    if (pathDecomposed.length >= 2)
        project = ProjectStore.getOneBySlugAndByNamespace(pathDecomposed[1], namespace);
    if (project == null && pathDecomposed.length >= 2)
        context.warn("Invalid path: project not found");
    if (pathDecomposed.length > 3)
        context.warn("Invalid path: a path cannot have more than 3 parts");
    return project;
  }

}