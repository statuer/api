package fr.lefuturiste.statuer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import fr.lefuturiste.statuer.discord.Command;

/**
 * Will test the command components parsing thing
 * And the command hadnler thing
 */
public class DiscordTest 
{
    @Test
    public void shouldParseUsage()
    {
      Command command = new Command("hello").withUsage("<required1> <required2> <optional1>? <optional2>?");
      Map<String, Boolean> args = command.getArguments();
      assertEquals(4, command.getArgumentsCount());
      assertFalse(command.getArguments().isEmpty());
      assertTrue(args.get("required1"));
      assertTrue(args.get("required2"));
      assertFalse(args.get("optional1"));
      assertFalse(args.get("optional2"));
      assertEquals(2, command.getRequiredArgumentsCount());
      assertEquals(2, command.getOptionalArgumentsCount());
      assertFalse(command.hasInfiniteArguments());
      assertEquals("hello <required1> <required2> <optional1>? <optional2>?", command.getFullUsage());

      command = new Command("hello-world").withUsage("<required1>");
      assertEquals(1, command.getArgumentsCount());
      assertTrue(command.getArguments().get("required1"));
      assertEquals(1, command.getRequiredArgumentsCount());
      assertEquals(0, command.getOptionalArgumentsCount());
      assertFalse(command.hasInfiniteArguments());
      assertEquals("hello-world <required1>", command.getFullUsage());

      command = new Command("no-args");
      assertTrue(command.getArguments().isEmpty());
      assertEquals(0, command.getArgumentsCount());
      assertEquals(0, command.getRequiredArgumentsCount());
      assertEquals(0, command.getOptionalArgumentsCount());
      assertFalse(command.hasInfiniteArguments());
      assertEquals("no-args", command.getFullUsage());

      command = new Command("infinite").withUsage("<require1> <infinite>?...");
      assertFalse(command.getArguments().isEmpty());
      assertEquals(2, command.getArgumentsCount());
      assertEquals(1, command.getRequiredArgumentsCount());
      assertEquals(1, command.getOptionalArgumentsCount());
      assertTrue(command.hasInfiniteArguments());
      assertEquals("infinite <require1> <infinite>?...", command.getFullUsage());
    }
}
