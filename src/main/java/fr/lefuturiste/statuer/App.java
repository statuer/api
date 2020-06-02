package fr.lefuturiste.statuer;

import fr.lefuturiste.statuer.controllers.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.Spark;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;

import fr.lefuturiste.statuer.discord.Bot;

public class App {
  public static CheckThread checkThread;

  public static Logger logger;

  private static long startTime;

  private static Dotenv dotenv;

  private static String latchedMode = "";

  public static boolean init(String mode) {
    if (dotenv == null) {
      dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .directory(System.getProperty("user.dir"))
        .filename(mode == "test" ? ".env.test" : ".env")
        .load();
    }
    System.out.println("Latchedmode: " + latchedMode);
    if (latchedMode == mode) {
      return true;
    }
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel",
        Objects.requireNonNull(dotenv.get("LOG_LEVEL") == null ? "info" : dotenv.get("LOG_LEVEL")));
    logger = LoggerFactory.getLogger(App.class);
    String[] requiredKeys = {
      "PORT",
      "MYSQL_CONNECTION_URL",
      "MYSQL_USERNAME",
      "MYSQL_PASSWORD",
      "DISCORD_BOT_TOKEN",
      "DISCORD_CLIENT_ID"
    };
    ArrayList<String> missingKeys = new ArrayList<>();
    for (String key : requiredKeys) {
      if (dotenv.get(key) == null) {
        missingKeys.add(key);
      }
    }
    if (!missingKeys.isEmpty()) {
      logger.error("Missing keys in environment variables");
      logger.error("These environments keys are missing: " + missingKeys.toString());
      return false;
    }
    Spark.port(Integer.valueOf(Objects.requireNonNull(dotenv.get("PORT"))));
    System.setProperty("org.jboss.logging.provider", "slf4j");
    System.setProperty("user.timezone", "Europe/Paris");
    HibernateService.setConfig(dotenv.get("MYSQL_CONNECTION_URL"), dotenv.get("MYSQL_USERNAME"),
        dotenv.get("MYSQL_PASSWORD"));
    if (mode == "test") {
      HibernateService.cleanDatabase();
    }
    HibernateService.getEntityManager();

    latchedMode = mode;
    logger.info("Application intialized");
    return true;
  }

  public static void main(String[] args) {
    startTime = System.currentTimeMillis();
    if (!init("default")) {
      return;
    }
    logger.info("Starting application...");
    HibernateService.launchConnexionFailurePreventerUtil();
    try {
      new Bot(dotenv.get("DISCORD_CLIENT_ID"), dotenv.get("DISCORD_BOT_TOKEN")).start();
    } catch (Exception e) {
      logger.error("Failed to initialize the bot, it may be a command list issue");
      e.printStackTrace();
      System.exit(1);
    }
    checkThread = new CheckThread();
    // Spark.before((request, response) -> {
    // System.out.println(request.headers("Authorization"));
    // });
    Spark.get("/", (req, res) -> {
      res.status(200);
      return new JSONObject().put("success", true);
    });
    Spark.post("/graphql", "application/json", GraphQLController.execute);
    Spark.get("/query/:path", "application/json", QueryController.get);
    Spark.post("/query/:path", "application/json", QueryController.create);
    Spark.put("/query/:path", "application/json", QueryController.update);
    Spark.delete("/query/:path", "application/json", QueryController.delete);
    Spark.path("/namespace", () -> {
      Spark.get("", "application/json", NamespaceController.getMany);
      Spark.get("/:id", "application/json", NamespaceController.getOne);
      Spark.post("", "application/json", NamespaceController.store);
      Spark.put("/:id", "application/json", NamespaceController.update);
      Spark.delete("/:id", "application/json", NamespaceController.delete);
    });
    Spark.path("/project", () -> {
      Spark.get("", "application/json", ProjectController.getMany);
      Spark.get("/:id", "application/json", ProjectController.getOne);
      Spark.post("", "application/json", ProjectController.store);
      Spark.put("/:id", "application/json", ProjectController.update);
      Spark.delete("/:id", "application/json", ProjectController.delete);
    });
    Spark.path("/service", () -> {
      Spark.get("", "application/json", ServiceController.getMany);
      Spark.get("/:id", "application/json", ServiceController.getOne);
      Spark.post("", "application/json", ServiceController.store);
      Spark.put("/:id", "application/json", ServiceController.update);
      Spark.delete("/:id", "application/json", ServiceController.delete);
    });
    Spark.notFound((req, res) -> new JSONObject().put("success", false).put("error", "Not found"));
    Spark.internalServerError(
        (req, res) -> new JSONObject().put("success", false).put("error", "Internal server error"));
    Spark.awaitInitialization();
    while (checkThread.canRun) {
      try {
        checkThread.run();
      } catch (Exception checkThreadException) {
        checkThreadException.printStackTrace();
        logger.error("Check thread has exited due to an exception. Restarting in 10 seconds...");
        try {
          Thread.sleep(Duration.ofSeconds(10).toMillis());
        } catch (InterruptedException interruptedException) {
          interruptedException.printStackTrace();
        }
      }
    }
  }

  public static String returnJSON(Response response, JSONObject jsonObject) {
    response.header("Content-type", "application/json");
    return jsonObject.toString(0);
  }

  public static Duration getUpTime() {
    return Duration.ofMillis(System.currentTimeMillis() - startTime);
  }
}
