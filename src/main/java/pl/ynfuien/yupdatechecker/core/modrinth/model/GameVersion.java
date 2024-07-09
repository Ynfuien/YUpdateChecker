package pl.ynfuien.yupdatechecker.core.modrinth.model;

import java.time.Instant;
import java.util.Map;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class GameVersion {
    private final String version;
    private final String versionType;
    private final Instant date;
    private final boolean major;

    public GameVersion(Map<String, Object> jsonMap) {
        version = (String) jsonMap.get("version");
        versionType = (String) jsonMap.get("version_type");
        date = Instant.parse((String) jsonMap.get("date"));
        major = (boolean) jsonMap.get("major");
    }

    public String getVersion() {
        return version;
    }

    public String getVersionType() {
        return versionType;
    }

    public Instant getDate() {
        return date;
    }

    public boolean isMajor() {
        return major;
    }
}
