package fr.lefuturiste.statuer.discord;

import fr.lefuturiste.statuer.discord.commands.*;

public class CommandsList {
  private static Command[] commands = null;

  public static void generateCommands() throws NoSuchMethodException, SecurityException {
    commands = new Command[] {
      /**
       * Random commands
       */
      new Command("ping", OtherCommandsController.class).withDescription("PING to check if the bot is alive"),
      
      new Command("help", OtherCommandsController.class).withDescription("Show this help")
          .withUsage("<commandName>?"),

      new Command(new String[] { "about", "what" }, OtherCommandsController.class)
          .withDescription("Get bot's meta data"),

      new Command(new String[] { "debug", "debug-cmd" }, OtherCommandsController.class).withVisibility(false)
          .withDescription("Debug command parsing").withUsage("<arg1>?..."),

      /**
       * Query commands
       */
      new Command(new String[] { "get", "view", "get-as-json", "get-json" }, QueryCommandsController.class)
        .withDescription("View a path, use get-json to get JSON output").withUsage("<path>"),

      /**
       * Mutation commands
       */
      new Command(new String[] { "create", "store" }, MutationCommandsController.class)
        .withDescription("Create all entities of the path").withUsage("<path>"),
      
      new Command(new String[] { "update", "edit", "set" }, MutationCommandsController.class)
      .withDescription("Edit a path with key=value syntax").withUsage("<path> <key=value>..."),

      new Command(new String[] { "delete", "del", "remove" }, MutationCommandsController.class)
        .withDescription("Recursively delete a path").withUsage("<path>"),

      /***/
      new Command("status", StatusCommandController.class)
        .withDescription("Get overall status of all the services inside the namespace")
        .withUsage("<namespace-path>"),

      new Command("incidents", IncidentsCommandController.class)
        .withDescription("Get all the last 90 days incidents for a service")
        .withUsage("<service-path>"),

      new Command("incident", IncidentsCommandController.class)
      .withDescription("Get details of a incident")
      .withUsage("<incident-uuid>"),

      new Command("reset", ResetCommandController.class)
        .withDescription("Reset the uptime and delete all incidents of a service")
        .withUsage("<service-uuid>"),
      
      new Command("refresh", RefreshCommandController.class)
        .withVisibility(false) // Force the execution of a check on a service
        .withUsage("<service-uuid>")
        
    };
  }

  public static Command[] getCommands() {
    return commands;
  }
}