package pl.ynfuien.yupdatechecker.core.modrinth.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class ProjectVersion {
    private final String id;
    private final String projectId;
    private final String authorId;

    private final String name;
    private final String versionNumber;
    private final String changelog;

    private final String versionType;
    private final List<String> gameVersions;
    private final List<String> loaders;

    private final Instant datePublished;

    private final int downloads;
    private final boolean featured;

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
