package pl.ynfuien.yupdatechecker.core.modrinth.model;

import com.google.gson.internal.LinkedTreeMap;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class Project {
    private final String id;
    private final String slug;
    private final String title;
    private final String description;
    private final String body;

    private final String issuesUrl;
    private final String sourceUrl;
    private final String wikiUrl;
    private final String discordUrl;
    private final String iconUrl;

    private final String projectType;
    private final List<String> versions;
    private final List<String> gameVersions;
    private final List<String> loaders;

    private final int downloads;
    private final int followers;

    private final int color;

    private final String team;

    private final Instant published;
    private final Instant updated;

    private final License license;


    public Project(Map<String, Object> jsonMap) {
        id = (String) jsonMap.get("id");
        slug = (String) jsonMap.get("slug");
        title = (String) jsonMap.get("title");
        description = (String) jsonMap.get("description");
        body = (String) jsonMap.get("body");
        issuesUrl = (String) jsonMap.get("issues_url");
        sourceUrl = (String) jsonMap.get("source_url");
        wikiUrl = (String) jsonMap.get("wiki_url");
        discordUrl = (String) jsonMap.get("discord_url");
        iconUrl = (String) jsonMap.get("icon_url");
        projectType = (String) jsonMap.get("project_type");

        versions = (ArrayList<String>) jsonMap.get("versions");
        gameVersions = (ArrayList<String>) jsonMap.get("game_versions");
        loaders = (ArrayList<String>) jsonMap.get("loaders");

        downloads = ((Double) jsonMap.get("downloads")).intValue();
        followers = ((Double) jsonMap.get("followers")).intValue();
        color = ((Double) jsonMap.get("color")).intValue();
        team = (String) jsonMap.get("team");

        published = Instant.parse((String) jsonMap.get("published"));
        updated = Instant.parse((String) jsonMap.get("updated"));

        LinkedTreeMap<String, String> treeMap = (LinkedTreeMap<String, String>) jsonMap.get("license");
        license = new License(treeMap.get("id"), treeMap.get("name"), treeMap.get("url"));
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBody() {
        return body;
    }

    public String getIssuesUrl() {
        return issuesUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getWikiUrl() {
        return wikiUrl;
    }

    public String getDiscordUrl() {
        return discordUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getProjectType() {
        return projectType;
    }

    public List<String> getVersions() {
        return versions;
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public List<String> getLoaders() {
        return loaders;
    }

    public int getDownloads() {
        return downloads;
    }

    public int getFollowers() {
        return followers;
    }

    public int getColor() {
        return color;
    }

    public String getTeam() {
        return team;
    }

    public Instant getPublished() {
        return published;
    }

    public Instant getUpdated() {
        return updated;
    }

    public License getLicense() {
        return license;
    }

    public record License(String id, String name, String url) {}
}
