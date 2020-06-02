package fr.lefuturiste.statuer.models;

import fr.lefuturiste.statuer.models.type.IncidentReason;
import fr.lefuturiste.statuer.models.type.IncidentReasonConverter;
import org.json.JSONObject;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.introproventures.graphql.jpa.query.annotation.GraphQLDescription;

import java.time.Instant;
import java.util.UUID;

/**
 * The incident entity represent a period where a service was down. It is
 * defined with the duration of the down time and the service affected In the
 * future, a incident will be able to regroup many services instead of one
 */
@Entity(name = "Incident")
public class Incident {

  /**
   * A UUID for the namespace
   */
  @Id
  @NotNull
  private String id;

  @GraphQLDescription("A non autofilled name to describe the incident eg. 'CloudFlare maintenance' (manual)")
  private String name;

  /**
   * A json encoded object that describe the reason of the incident reason:
   * invalid-code|timeout|failed message: Got 200 expected 400 status code
   * message: Time limit exceeded (2000ms)
   */
  @Convert(converter = IncidentReasonConverter.class)
  @GraphQLDescription("A JSON object that describe why the checker has failed to get the service")
  @Column(columnDefinition="TEXT")
  private IncidentReason reason;

  @GraphQLDescription("A non autofilled to describe the description of the incident (manual)")
  private String description;

  private String impact; // 'high' or 'low'

  @NotNull
  @GraphQLDescription("A timestamp that describe when the incident started")
  private Instant startedAt;

  @GraphQLDescription("A timestamp that describe when the incident finished, if null that mean that the incident is ongoing")
  private Instant finishedAt;

  @ManyToOne
  @GraphQLDescription("The service which is concerned by the incident")
  private Service service;

  public Incident() {
    id = UUID.randomUUID().toString();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getImpact() {
    return impact;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public Service getService() {
    return service;
  }

  public IncidentReason getReason() {
    return reason;
  }

  public JSONObject toJSONObject(int deep) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id);
    jsonObject.put("name", name);
    jsonObject.put("description", description);
    jsonObject.put("impact", impact);
    jsonObject.put("started_at", startedAt);
    jsonObject.put("finished_at", finishedAt);
    if (deep == 1) {
      jsonObject.put("service", getService().toJSONObject(0));
    }
    return jsonObject;
  }

  public Incident setId(String id) {
    this.id = id;
    return this;
  }

  public Incident setImpact(String impact) {
    this.impact = impact;
    return this;
  }

  public Incident setDescription(String description) {
    this.description = description;
    return this;
  }

  public Incident setName(String name) {
    this.name = name;
    return this;
  }

  public Incident setService(Service service) {
    this.service = service;
    return this;
  }

  public Incident setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public Incident setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  public Incident setReason(IncidentReason reason) {
    this.reason = reason;
    return this;
  }
}