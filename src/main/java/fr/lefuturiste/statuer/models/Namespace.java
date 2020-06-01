package fr.lefuturiste.statuer.models;

import org.hibernate.validator.constraints.URL;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.introproventures.graphql.jpa.query.annotation.GraphQLDescription;

import java.util.Set;

/**
 * The namespace entity represent a organization, a corporation eg. Github,
 * Google or ONU
 */
@Entity(name = "Namespace")
public class Namespace {

  /**
   * A UUID for the namespace
   */
  @Id
  @NotNull
  private String id;

  @NotNull
  @NotEmpty
  @GraphQLDescription("The usable slug of the namespace eg. stc or google")
  private String slug;

  @GraphQLDescription("The full name of the namespace eg. STAN-TAb Corp. or Google Inc.")
  private String name;

  @URL
  @GraphQLDescription("The url of the logo/icon/image that illustrate this namespace")
  private String imageUrl;

  @URL
  private String discordWebhook;

  @OneToMany(mappedBy = "namespace", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  @GraphQLDescription("All the projects of the namespace")
  private Set<Project> projects;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public Set<Project> getProjects() {
    return projects;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getDiscordWebhook() {
    return discordWebhook;
  }

  public String getHidedDiscordWebpack() {
    return discordWebhook.substring(0, discordWebhook.length() - 30) + "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";
  }

  public String getLabel() {
    return name == null ? getSlug() : getName();
  }

  public Namespace setId(String id) {
    this.id = id;
    return this;
  }

  public Namespace setName(String name) {
    this.name = name;
    return this;
  }

  public Namespace setSlug(String slug) {
    this.slug = slug;
    return this;
  }

  public Namespace addProject(Project project) {
    this.projects.add(project);
    return this;
  }

  public Namespace setDiscordWebhook(String discordWebhook) {
    this.discordWebhook = discordWebhook;
    return this;
  }

  public Namespace setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  public JSONObject toJSONObject(int depth) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id);
    jsonObject.put("slug", slug);
    jsonObject.put("discord_webhook", discordWebhook);
    jsonObject.put("image_url", imageUrl);
    jsonObject.put("name", name);
    if (depth >= 1) {
      JSONArray projectsJson = new JSONArray();
      for (Project project : getProjects())
        projectsJson.put(project.toJSONObject(depth - 1));
      jsonObject.put("projects", projectsJson);
    }
    return jsonObject;
  }

}