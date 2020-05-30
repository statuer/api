package fr.lefuturiste.statuer.models;

import org.hibernate.validator.constraints.URL;
import org.json.JSONObject;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.introproventures.graphql.jpa.query.annotation.GraphQLDescription;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * The entity service represent most of the time a server like an HTTP server, a Database or all others kinds of sockets
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
     * The number represent the period in seconds (eg. 60,120,180,240,300,420,600,900,1800,3600)
     */
    @GraphQLDescription("The period of the check in seconds, the more this number goes up the less often the service is check")
    private Integer checkPeriod = 120;

    @URL
    @GraphQLDescription("The uri where the check thread will check if weather or not the service is up")
    private String url;

    @GraphQLDescription("The type of checker which will be used eg. http, ping, mysql etc...")
    private String type;

    @GraphQLDescription("The network timeout to specify when the checker will give up and set the service as down")
    private Integer timeout;

    @GraphQLDescription("The number of attemps before the service is declared as DOWN")
    private Integer attempts = 3;

    @GraphQLDescription("The timestamp when the service was checked for the last time")
    private Instant lastCheckAt;

    @GraphQLDescription("The timestamp when the service was down for the last time")
    private Instant lastDownAt;

    @GraphQLDescription("The percentage of the ratio of the UP duration over the TOTAL duration")
    private float uptime;

    @ManyToOne
    private Project project;

    @OneToMany(mappedBy="service", cascade=CascadeType.REMOVE, fetch=FetchType.LAZY)
    private Set<Incident> incidents;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public JSONObject toJSONObject(int deep) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("slug", slug);
        jsonObject.put("url", url);
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

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getCheckPeriod() {
        return checkPeriod;
    }

    public void setCheckPeriod(int checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    public Boolean isAvailable() {
        return (status == null) ? null : status.equals("up");
    }

    public void setAvailable(Boolean available) {
        status = available ? "up" : "down";
    }

    public void setStatus(String newStatus) {
        status = newStatus;
    }

    public String getStatus() {
        return status;
    }

    public String getDiscordWebhook() {
        return discordWebhook;
    }

    public void setDiscordWebhook(String discordWebhook) {
        this.discordWebhook = discordWebhook;
    }

    public Instant getLastCheckAt() {
        return lastCheckAt;
    }

    public void setLastCheckAt(Instant lastCheckAt) {
        this.lastCheckAt = lastCheckAt;
    }

    public String getPath() {
        return getProject().getPath() + "." + this.slug;
    }

    public Instant getLastDownAt() {
        return lastDownAt;
    }

    public void setLastDownAt(Instant lastDownAt) {
        this.lastDownAt = lastDownAt;
    }

    public Set<Incident> getIncidents() {
        return incidents;
    }

    public void setIncidents(Set<Incident> incidents) {
        this.incidents = incidents;
    }

    /**
     * The last incident of a service is the only incident in the incident's service list who has a null finishedAt field
     *
     * @return Incident
     */
    public Incident getLastIncident() {
        for (Incident incident : incidents) {
            if (incident.getFinishedAt() == null)
                return incident;
        }
        return null;
    }

    public float getUptime() {
        return uptime;
    }

    public void setUptime(float uptime) {
        this.uptime = uptime;
    }

    public String getLastIncidentDate() {
        Incident lastIncident = getLastIncident();
        return lastIncident == null ? "None" : lastIncident.getFinishedAt() == null ? "Ongoing incident" : DateTimeFormatter.ISO_INSTANT.format(getLastIncident().getFinishedAt());
    }
}