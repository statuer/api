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
 * The project entity represent a group of service
 */
@Entity(name = "Project")
public class Project {

  /**
   * A UUID for the project
   */
  @Id
  @NotNull
  private String id;

  @NotNull
  @NotEmpty
  @GraphQLDescription("The usable slug of the project eg. youtube or staileu, This slug is used to identify a project")
  private String slug;

  @GraphQLDescription("The full name of the project eg. YouTube or STAIL.EU Accounts")
  private String name;

  @URL
  @GraphQLDescription("The url of the logo/icon/image that illustrate this project")
  private String imageUrl;

  @URL
  private String discordWebhook;

  @ManyToOne
  @GraphQLDescription("The parent namespace of the project")
  private Namespace namespace;

  @GraphQLDescription("All the services owned by the project")
  @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
  private Set<Service> services;

  public String getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public String getName() {
    return name;
  }

  public Namespace getNamespace() {
    return namespace;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getDiscordWebhook() {
    return discordWebhook;
  }

  public Set<Service> getServices() {
    return services;
  }

  public String getPath() {
    return this.getNamespace().getSlug() + "." + this.slug;
  }

  public String getLabel() {
    if (name == null) {
      return getPath();
    } else {
      return name;
    }
  }

  public Project setId(String id) {
    this.id = id;
    return this;
  }

  public Project setName(String name) {
    this.name = name;
    return this;
  }

  public Project setDiscordWebhook(String discordWebhook) {
    this.discordWebhook = discordWebhook;
    return this;
  }

  public Project setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  public Project setNamespace(Namespace namespace) {
    this.namespace = namespace;
    return this;
  }

  public Project setSlug(String slug) {
    this.slug = slug;
    return this;
  }

  public JSONObject toJSONObject(int depth) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("id", id);
    jsonObject.put("slug", slug);
    jsonObject.put("name", name);
    jsonObject.put("discord_webhook", discordWebhook);
    if (depth >= 1) {
      JSONArray servicesJson = new JSONArray();
      for (Service service : getServices()) {
        servicesJson.put(service.toJSONObject(depth - 1));
      }
      jsonObject.put("services", servicesJson);
      // jsonObject.put("namespace", getNamespace().toJSONObject(0));
    }
    return jsonObject;
  }
}