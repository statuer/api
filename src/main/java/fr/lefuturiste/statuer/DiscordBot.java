package fr.lefuturiste.statuer;

import fr.lefuturiste.statuer.controllers.DiscordCommandsController;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordBot {
    private JDA jda;

    private String clientId;

    private String token;

    private static List<String> managerRoles = Arrays.asList("can-deploy", "can-manage", "statuer");

    private static List<String> managerCommands = Arrays.asList("create", "edit", "delete");

    private String[][] allowedCommands = {
            {"ping"}, {"help"}, {"about"}, {"debug"},
            {"get", "view"}, {"create", "store"}, {"edit", "update", "set"}, {"delete", "del", "remove"},
            {"status"}, {"incidents"}, {"refresh"}
    };

    DiscordBot(String clientId, String token) {
        this.clientId = clientId;
        //  https://discordapp.com/oauth2/authorize?client_id=INSERT_CLIENT_ID_HERE&scope=bot&permissions=0
        this.token = token;
    }

    void start() {
        try {
            jda = new JDABuilder(AccountType.BOT)
                    .setToken(token)
                    .addEventListener(new EventListener())
                    .buildBlocking();
            jda.getPresence().setGame(Game.of(Game.GameType.WATCHING, "??|%%|&&|## and a lot of services"));
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    String getAuthorizeUrl() {
        int permissionInteger = 537128000;
        return "https://discordapp.com/oauth2/authorize?client_id=" + clientId + "&scope=bot&permissions=" + permissionInteger;
    }

    private static void error(MessageChannel channel, Exception exception) {
        EmbedBuilder builder = new EmbedBuilder().setTitle(":red_circle: Exception occurred!");
        StringBuilder description = new StringBuilder("**" + exception.toString() + "** \n");
        for (StackTraceElement stackElement : exception.getStackTrace())
            description.append(stackElement.toString().replace("at ", "")).append("\n");
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
                        DiscordContext context = new DiscordContext(event);
                        context.startLoading();
                        String rawCommand = message.getContentDisplay().substring(2);
                        String[] spacedParts = rawCommand.split(" ");

                        // verify permission
                        if (managerCommands.contains(spacedParts[0]) &&
                                event.getMember().getRoles().stream().filter(
                                        role -> DiscordBot.managerRoles.contains(role.getName())
                                ).collect(Collectors.toList()).size() == 0) {
                            context.warn("Get the fuck out of my store, we are closed (permission issue)");
                            return;
                        }

                        // 'create' 'param1="something' 'else"'
                        // we look for components with quotes
                        StringBuilder pair = null;
                        ArrayList<String> commandParts = new ArrayList<>();
                        for (String commandComponent : spacedParts) {
                            if (commandComponent.indexOf('"') != -1 && pair == null) {
                                pair = new StringBuilder(commandComponent);
                            } else if (commandComponent.indexOf('"') != -1 && pair != null) {
                                commandParts.add(
                                        pair.toString().replaceAll("\"", "") +
                                                ' ' + commandComponent.replaceAll("\"", "")
                                );
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

                        // System.out.println(newCommandComponents);
                        String commandName = null;
                        for (String[] command : allowedCommands) {
                            List<String> commandAlias = Arrays.asList(command);
                            if (commandAlias.contains(commandParts.get(0)))
                                commandName = commandAlias.get(0);
                        }
                        if (commandName == null) {
                            context.warn("Unknown command!");
                        } else {
                            DiscordCommandsController.class
                                    .getDeclaredMethod(commandName, DiscordContext.class)
                                    .invoke(DiscordCommandsController.class, context);
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
                DiscordBot.error(event.getChannel(), exception);
            }
        }
    }
}
