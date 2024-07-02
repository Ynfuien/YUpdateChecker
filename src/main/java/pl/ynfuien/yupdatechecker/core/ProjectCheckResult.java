package pl.ynfuien.yupdatechecker.core;

import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import org.bukkit.Bukkit;
import pl.ynfuien.yupdatechecker.config.PluginConfig;

import java.util.ArrayList;
import java.util.List;

//public record ProjectCheckResult(Project project, ProjectVersion currentVersion, ProjectVersion latestVersion, int versionsBehind) {
//    public boolean upToDate() {
//        return versionsBehind == 0;
//    }
//}

public class ProjectCheckResult {
    private final static String MINECRAFT_VERSION = Bukkit.getMinecraftVersion();

    private final Project project;
    private final ProjectVersion currentVersion;
    private final List<ProjectVersion> newerVersions;
    private final ProjectVersion latestVersion;

    private final int versionsBehind;
    private boolean newerReleaseForNewerMcVersion;
    private boolean newerReleaseForDifferentChannel;

    public ProjectCheckResult(Project project, ProjectVersion currentVersion, List<ProjectVersion> newerVersions) {
        this.project = project;
        this.currentVersion = currentVersion;
        this.newerVersions = newerVersions;

        List<ProjectVersion> fittingVersions = new ArrayList<>();
        for (ProjectVersion version : newerVersions) {
            if (version.getId().equalsIgnoreCase(currentVersion.getId())) break;
            if (!PluginConfig.considerChannels.contains(version.getVersionType())) {
                newerReleaseForDifferentChannel = true;
                continue;
            }

            if (!version.getGameVersions().contains(MINECRAFT_VERSION)) {
                newerReleaseForNewerMcVersion = true;
                continue;
            }

            fittingVersions.add(version);
        }

        latestVersion = fittingVersions.isEmpty() ? currentVersion : fittingVersions.get(0);
        versionsBehind = fittingVersions.size();
    }

    public Project project() {
        return project;
    }

    public ProjectVersion currentVersion() {
        return currentVersion;
    }

    public List<ProjectVersion> newerVersions() {
        return newerVersions;
    }

    public ProjectVersion latestVersion() {
        return latestVersion;
    }

    public int versionsBehind() {
        return versionsBehind;
    }

    public boolean isNewerReleaseForNewerMcVersion() {
        return newerReleaseForNewerMcVersion;
    }

    public boolean isNewerReleaseForDifferentChannel() {
        return newerReleaseForDifferentChannel;
    }

    public boolean upToDate() {
        return versionsBehind == 0;
    }
}