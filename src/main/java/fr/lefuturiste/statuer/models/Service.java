package fr.lefuturiste.statuer.models;

import org.hibernate.validator.constraints.URL;
import org.json.JSONObject;

import fr.lefuturiste.statuer.InvalidInspectionResultException;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.introproventures.graphql.jpa.query.annotation.GraphQLDescription;
import com.introproventures.graphql.jpa.query.annotation.GraphQLIgnore;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The entity service represent most of the time a server like an HTTP server, a
 * Database or all others kinds of sockets
 */
@Entity(name = "Service")
public class Service {

  /**
   * A UUID for the project
   */
  @Id
  @NotNull
  private String id;

  @NotNull
  @NotEmpty
  @GraphQLDescription("The usable slug of the service eg. api, database or frontend, his slug is used to identify a service")
  private String slug;

  @URL
  private String discordWebhook;

  @GraphQLDescription("UP or DOWN")
  private String status;

  /**
   * The number represent the period in seconds (eg.
   * 60,120,180,240,300,420,600,900,1800,3600)
   */
  @GraphQLDescription("The period of the check in seconds, the more this number goes up the less often the service is check")
  private Integer checkPeriod = 120;

  @URL
  @GraphQLDescription("The uri where the check thread will check if weather or not the service is up")
  private String url;

  @GraphQLDescription("The type of checker which will be used eg. http, ping, mysql etc...")
  private String type;

  @GraphQLDescription("The network timeout in seconds to specify when the checker will give up and set the service as down")
  private Integer timeout = 15;

  @GraphQLDescription("The number of attemps before the service is declared as DOWN")
  private Integer maxAttempts = 3;

  /**
   * A field used to describe at which attempt the checker is
   */
  @GraphQLIgnore
  private Integer currentAttempts = 0;

  @GraphQLDescription("The timestamp when the service was checked for the last time")
  private Instant lastCheckAt;

  @GraphQLDescription("The timestamp when the service was down for the last time")
  private Instant lastDownAt;

  @GraphQLDescription("The percentage of the ratio of the UP duration over the TOTAL duration")
  private float uptime = 1;

  @ManyToOne
  private Project project;

  @OneToMany(mappedBy = "service", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  @OrderBy("finishedAt DESC")
  private Set<Incident> incidents;

  public Service() {
    id = UUID.randomUUID().toString();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSlug() {
    return slug;
  }

  public Project getProject() {
    return project;
  }

  public JSONObject toJSONObject(int deep) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id);
    jsonObject.put("slug", slug);
    jsonObject.put("url", url);
    jsonObject.put("attempts", maxAttempts);
    jsonObject.put("type", type);
    jsonObject.put("is_available", isAvailable());
    jsonObject.put("status", status);
    jsonObject.put("check_period", checkPeriod);
    jsonObject.put("discord_webhook", discordWebhook);
    jsonObject.put("last_checked_at", lastCheckAt != null ? lastCheckAt.toString() : null);
    jsonObject.put("incidents", incidents.size());
    jsonObject.put("uptime", uptime);
    jsonObject.put("timeout", timeout);
    if (deep == 1) {
      jsonObject.put("project", getProject().toJSONObject(0));
    }
    return jsonObject;
  }

  public int getTimeout() {
    return (timeout == null || timeout <= 0) ? 15 : timeout;
  }

  public String getType() {
    return type;
  }

  public String getUrl() {
    return url;
  }

  public int getCheckPeriod() {
    return checkPeriod;
  }

  public Boolean isAvailable() {
    return (status == null) ? null : status.equals("up");
  }

  /**
   * The last incident of a service is the only incident in the incident's service
   * list who has a null finishedAt field Return null if the service doesn't have
   * any ongoing incidents
   *
   * @return Incident The ongoing incident
   */
  public Incident getOngoingIncident() {
    for (Incident incident : incidents) {
      if (incident.getFinishedAt() == null) {
        return incident;
      }
    }
    return null;
  }

  public float getUptime() {
    return uptime;
  }

  public int getMaxAttempts() {
    return maxAttempts == null ? 3 : maxAttempts;
  }

  public int getCurrentAttempts() {
    return currentAttempts;
  }

  public String getPath() {
    return getProject().getPath() + "." + this.slug;
  }

  public Instant getLastDownAt() {
    return lastDownAt;
  }

  public Set<Incident> getIncidents() {
    return incidents;
  }

  public String getStatus() {
    return status;
  }

  public String getDiscordWebhook() {
    return discordWebhook;
  }

  public Instant getLastCheckAt() {
    return lastCheckAt;
  }

  public Service setSlug(String slug) {
    this.slug = slug;
    return this;
  }

  public Service setType(String type) {
    this.type = type;
    return this;
  }

  public Service setTimeout(int timeout) {
    this.timeout = timeout;
    return this;
  }

  public Service setProject(Project project) {
    this.project = project;
    return this;
  }

  public Service setUrl(String url) {
    this.url = url;
    return this;
  }

  public Service setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
    return this;
  }

  public Service setCurrentAttempts(int currentAttempts) {
    this.currentAttempts = currentAttempts;
    return this;
  }

  public Service incrementCurrentAttempts() {
    this.currentAttempts++;
    return this;
  }

  public Service setCheckPeriod(int checkPeriod) {
    this.checkPeriod = checkPeriod;
    return this;
  }

  public Service setAvailable(Boolean available) {
    status = available ? "up" : "down";
    return this;
  }

  public Service setStatus(String newStatus) {
    status = newStatus;
    return this;
  }

  public Service setDiscordWebhook(String discordWebhook) {
    this.discordWebhook = discordWebhook;
    return this;
  }

  public Service setLastCheckAt(Instant lastCheckAt) {
    this.lastCheckAt = lastCheckAt;
    return this;
  }

  public Service setLastDownAt(Instant lastDownAt) {
    this.lastDownAt = lastDownAt;
    return this;
  }

  public Service setIncidents(Set<Incident> incidents) {
    this.incidents = incidents;
    return this;
  }

  public Service setUptime(float uptime) {
    this.uptime = uptime;
    return this;
  }

  public boolean inspect() throws InvalidInspectionResultException {
    int ongoingIncidents = 0;
    for (Incident incident : incidents) {
      if (incident.getFinishedAt() == null) {
        ongoingIncidents++;
      }
    }
    if (ongoingIncidents > 1) {
      StringBuilder message = new StringBuilder();
      message.append("Entity inspect error: the service " + this.getPath() + " - " + this.getId());
      message.append(", has an incorect amount of incidents (");
      message.append(ongoingIncidents);
      message.append(")");

      throw new InvalidInspectionResultException(message.toString());
    }
    return true;
  }

  public Service addIncident(Incident incident) {
    if (incidents == null) {
      incidents = new HashSet<>();
    }
    incidents.add(incident);
    return this;
  }
}