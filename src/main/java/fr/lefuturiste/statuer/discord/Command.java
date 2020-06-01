package fr.lefuturiste.statuer.discord;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.lefuturiste.statuer.discord.commands.CommandController;

public class Command {
  private String[] identifiers = {};

  private Class<? extends CommandController> handlerClass;

  private Method handlerMethod;

  private boolean writeAccess = false;

  private boolean visibility = true;

  private String usage = "";

  private String description;

  private Map<String, Boolean> arguments = new HashMap<>();

  private boolean infiniteArguments = false;

  public Command(String identifier) {
    this.identifiers = new String[]{identifier};
  }

  public Command(String identifier, Class<? extends CommandController> handlerClass)
      throws NoSuchMethodException, SecurityException {
    this(new String[] { identifier }, handlerClass, identifier, false);
  }

  public Command(String identifier, Class<? extends CommandController> handlerClass, String handlerMethod)
      throws NoSuchMethodException, SecurityException {
    this(new String[] { identifier }, handlerClass, handlerMethod, false);
  }

  public Command(String[] identifiers, Class<? extends CommandController> handlerClass)
      throws NoSuchMethodException, SecurityException {
    this(identifiers, handlerClass, identifiers[0], false);
  }

  public Command(String[] identifiers, Class<? extends CommandController> handlerClass, String handlerMethod)
      throws NoSuchMethodException, SecurityException {
    this(identifiers, handlerClass, handlerMethod, false);
  }

  public Command(String[] identifiers, Class<? extends CommandController> handlerClass, String handlerMethod, boolean writeAccess)
      throws NoSuchMethodException, SecurityException {
    this.identifiers = identifiers;
    this.writeAccess = writeAccess;
    this.handlerClass = handlerClass;
    this.handlerMethod = handlerClass.getMethod(handlerMethod, Context.class);
  }

  public void invoke(Context context)
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    handlerMethod.invoke(handlerClass, context);
  }

  public Command setHandler(Class<? extends CommandController> handlerClass, String handlerMethod)
      throws NoSuchMethodException, SecurityException {
    this.handlerClass = handlerClass;
    this.handlerMethod = handlerClass.getMethod(handlerMethod, Context.class);
    return this;
  }

  public Command withUsage(String usage)
  {
    this.usage = usage;
    parseArguments();
    return this;
  }

  public Command withDescription(String description)
  {
    this.description = description;
    return this;
  }

  public Command withVisibility(boolean visibility)
  {
    this.visibility = visibility;
    return this;
  }
  
  public String getUsage()
  {
    return usage;
  }

  public String getDescription()
  {
    return description;
  }

  public boolean doesRequireWriteAccess() {
    return writeAccess;
  }

  public boolean hasInfiniteArguments() {
    return infiniteArguments;
  }

  public String[] getIdentifiers() {
    return identifiers;
  }

  public boolean isVisible() {
    return visibility;
  }

  public void parseArguments() {
    Pattern r = Pattern.compile("<([a-zA-Z0-9=\\-_]+)>(\\?)?(\\.\\.\\.)?");
    arguments = new HashMap<>();
    Matcher m = r.matcher(usage);
    while (m.find()) {
      arguments.put(m.group(1), m.group(2) == null);
      infiniteArguments = m.group(3) != null && m.group(3).equals("...");
    }
  }

  public Map<String, Boolean> getArguments() {
    return arguments;
  }

  public int getArgumentsCount() {
    return arguments.size();
  }

  public int getRequiredArgumentsCount() {
    if (arguments == null) {
      return 0;
    }
    return (int) arguments
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue())
      .count();
  }

  public int getOptionalArgumentsCount() {
    return arguments.size() - getRequiredArgumentsCount();
  }

  public String getFullUsage() {
    return identifiers[0] + (arguments.isEmpty() ? "" : " " + usage);
  }

} 