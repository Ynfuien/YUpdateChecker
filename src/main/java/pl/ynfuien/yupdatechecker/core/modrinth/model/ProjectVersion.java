package pl.ynfuien.yupdatechecker.core.modrinth.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class ProjectVersion {
    private String id;
    private String projectId;
    private String authorId;

    private String name;
    private String versionNumber;
    private String changelog;

    private String versionType;
    private List<String> gameVersions;
    private List<String> loaders;

    private Instant datePublished;

    private int downloads;
    private boolean featured;

    public ProjectVersion(Map<String, Object> jsonMap) {
        id = (String) jsonMap.get("id");
        projectId = (String) jsonMap.get("project_id");
        authorId = (String) jsonMap.get("author_id");
        name = (String) jsonMap.get("name");
        versionNumber = (String) jsonMap.get("version_number");
        changelog = (String) jsonMap.get("changelog");

        versionType = (String) jsonMap.get("version_type");
        gameVersions = (ArrayList<String>) jsonMap.get("game_versions");
        loaders = (ArrayList<String>) jsonMap.get("loaders");

        datePublished = Instant.parse((String) jsonMap.get("date_published"));

        downloads = ((Double) jsonMap.get("downloads")).intValue();
        featured = (boolean) jsonMap.get("featured");
    }

    public String getId() {
        return id;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getVersionType() {
        return versionType;
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public List<String> getLoaders() {
        return loaders;
    }

    public Instant getDatePublished() {
        return datePublished;
    }

    public int getDownloads() {
        return downloads;
    }

    public boolean isFeatured() {
        return featured;
    }
}
