package fr.lefuturiste.statuer.discord.commands;

import java.util.Arrays;
import java.util.List;

import fr.lefuturiste.statuer.App;
import fr.lefuturiste.statuer.discord.Command;
import fr.lefuturiste.statuer.discord.CommandsList;
import fr.lefuturiste.statuer.discord.Context;
import net.dv8tion.jda.core.EmbedBuilder;

public class OtherCommandsController extends CommandController {

  public static void debug(Context context) {
    StringBuilder output = new StringBuilder("```\n");
    for (String commandComponent : context.getPartsAsArray())
        output.append(commandComponent).append("\n");
    output.append("```");
    context.respond(output.toString());
  }

  public static void ping(Context context) {
    context.respond("Pong!");
  }

  public static void help(Context context) throws NoSuchMethodException, SecurityException {
    String prefix = "??";
    Command[] commands = CommandsList.getCommands();
    EmbedBuilder embed;
    if (context.getParts().size() == 1) {
      embed = new EmbedBuilder()
        .setTitle(":interrobang: Commands available")
        .setColor(context.INFO_COLOR);

      for (Command command : commands) {
        if (command.isVisible())
          embed.addField(prefix + command.getFullUsage(), command.getDescription(), false);
      }
    } else {
      Command selectedCommand = null;
      for (Command command : commands) {
        List<String> commandAlias = Arrays.asList(command.getIdentifiers());
        if (commandAlias.contains(context.getParts().get(1)))
          selectedCommand = command;
      }
      if (selectedCommand == null) {
        context.warn("The command \"" + context.getParts().get(1) + "\" was not found! (However the command help that you are using is right!)");
        return;
      }
      List<String> identifiers = Arrays.asList(selectedCommand.getIdentifiers());
      embed = new EmbedBuilder()
        .setTitle(":interrobang: Command description")
        .addField("Identifiers", identifiers.toString(), false)
        .addField("Usage", prefix + selectedCommand.getFullUsage(), false)
        .addField("Description", selectedCommand.getDescription(), false)
        .addField("Hidden?", selectedCommand.isVisible() ? "NO": "YES", false)
        .setColor(context.INFO_COLOR);
    }
    context.respondEmbed(embed);
  }

  public static void about(Context context) {
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
}
