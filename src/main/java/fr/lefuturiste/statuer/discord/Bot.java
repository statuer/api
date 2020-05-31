package fr.lefuturiste.statuer.discord;

import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

import fr.lefuturiste.statuer.App;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Bot {
  private String clientId;

  private String token;

  private static List<String> managerRoles = Arrays.asList("can-deploy", "can-manage", "statuer");

  private static List<String> managerCommands = Arrays.asList("create", "edit", "delete", "reset", "refresh");

  public Bot(String clientId, String token) {
    this.clientId = clientId;
    // https://discordapp.com/oauth2/authorize?client_id=INSERT_CLIENT_ID_HERE&scope=bot&permissions=0
    this.token = token;
  }

  public void start() throws NoSuchMethodException, SecurityException, LoginException, InterruptedException {
    JDA jda = new JDABuilder(AccountType.BOT).setToken(token).addEventListener(new EventListener()).buildBlocking();
    jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, "??|%%|&&|## and a lot of services"));
    CommandsList.generateCommands();
  }

  String getAuthorizeUrl() {
    int permissionInteger = 537128000;
    return "https://discordapp.com/oauth2/authorize?client_id=" + clientId + "&scope=bot&permissions="
        + permissionInteger;
  }

  private static void error(MessageChannel channel, Exception exception) {
    EmbedBuilder builder = new EmbedBuilder().setTitle(":red_circle: Exception occurred!");
    String reason = exception.getCause() != null ? exception.getCause().toString() : exception.toString();
    StringBuilder description = new StringBuilder("**" + reason + "** \n");
    StackTraceElement[] stackTrace = exception.getCause() != null ? exception.getCause().getStackTrace() : exception.getStackTrace();
    for (StackTraceElement stackElement : stackTrace) {
      String toAppend = stackElement.toString().replace("at ", "") + "\n";
      if (toAppend.length() + description.length() <= 2048) {
        description.append(toAppend);
      }
    }
    channel.sendMessage(builder.setDescription(description).build()).complete();
  }

  class EventListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
      if (event.isFromType(ChannelType.PRIVATE) || event.getAuthor().isBot())
        return;

      try {
        Message message = event.getMessage();
        if (message.getContentDisplay().length() > 2) {
          String messagePrefix = message.getContentDisplay().substring(0, 2);
          if (messagePrefix.equals("??")) {
            Context context = new Context(event);
            context.startLoading();
            String rawCommand = message.getContentDisplay().substring(2);
            String[] spacedParts = rawCommand.split(" ");

            // verify permission
            if (managerCommands.contains(spacedParts[0]) && event.getMember().getRoles().stream()
                .filter(role -> Bot.managerRoles.contains(role.getName())).collect(Collectors.toList()).size() == 0) {
              context.warn("Get the fuck out of my store, we are closed (permission issue)");
              return;
            }

            // parse all command parts
            // 'create' 'param1="something' 'else"'
            // we look for components with quotes
            StringBuilder pair = null;
            ArrayList<String> commandParts = new ArrayList<>();
            for (String commandComponent : spacedParts) {
              if (commandComponent.indexOf('"') != -1 && pair == null) {
                pair = new StringBuilder(commandComponent);
              } else if (commandComponent.indexOf('"') != -1 && pair != null) {
                commandParts.add(pair.toString().replaceAll("\"", "") + ' ' + commandComponent.replaceAll("\"", ""));
                pair = null;
              } else if (pair != null) {
                pair.append(" ").append(commandComponent);
              } else {
                commandParts.add(commandComponent);
              }
            }
            if (pair != null) {
              commandParts.add(pair.toString().replaceAll("\"", ""));
            }
            context.setParts(commandParts);

            // invoke the command handler
            // System.out.println(newCommandComponents);
            Command[] commands = CommandsList.getCommands();
            Command selectedCommand = null;
            for (Command command : commands) {
              List<String> commandAlias = Arrays.asList(command.getIdentifiers());
              if (commandAlias.contains(commandParts.get(0)))
                selectedCommand = command;
            }
            if (selectedCommand == null) {
              context.warn("Unknown command!");
            } else {
              // get if the command is valid
              // String usage = selectedCommand.getUsage();
              App.logger.debug(context.getParts().toString());
              //App.logger.debug(String.valueOf(selectedCommand.getRequiredArgumentsCount() + 1));
              if (
                context.getParts().size() < (selectedCommand.getRequiredArgumentsCount() + 1) ||
                (context.getParts().size() > (selectedCommand.getArgumentsCount() + 1) && !selectedCommand.hasInfiniteArguments())
              ) {
                context.warn("Usage: " + selectedCommand.getFullUsage());
                return;
              }

              selectedCommand.invoke(context);
            }
          }
        }
      } catch (Exception exception) {
        exception.printStackTrace();
        Bot.error(event.getChannel(), exception);
      }
    }
  }
}
