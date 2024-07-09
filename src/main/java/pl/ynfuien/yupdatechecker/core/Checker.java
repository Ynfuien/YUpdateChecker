package pl.ynfuien.yupdatechecker.core;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.bukkit.Bukkit;
import org.bukkit.World;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.modrinth.ModrinthAPI;
import pl.ynfuien.yupdatechecker.core.modrinth.model.GameVersion;
import pl.ynfuien.yupdatechecker.core.modrinth.model.Project;
import pl.ynfuien.yupdatechecker.core.modrinth.model.ProjectVersion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Checker {
    private final YUpdateChecker instance;
    private final ModrinthAPI modrinthAPI;

    private static final List<String> SUPPORTED_LOADERS = getSupportedLoaders();
    private static final List<String> MINECRAFT_VERSIONS = new ArrayList<>();
    private static final String SUPPORTED_VERSION = Bukkit.getMinecraftVersion();

    private static final Comparator<ProjectCheckResult> PROJECT_COMPARATOR =
            (left, right) -> left.project().getTitle().compareToIgnoreCase(right.project().getTitle());

    private CheckResult lastCheck = null;

    private boolean isCheckRunning = false;
    private CurrentCheck currentCheck = new CurrentCheck();

    public Checker(YUpdateChecker instance) {
        this.instance = instance;
        this.modrinthAPI = instance.getModrinthAPI();
    }

    private List<String> getMinecraftVersions() {
        List<GameVersion> versions;
        try {
            currentCheck.incrementRequests();
            versions = modrinthAPI.getGameVersionTags();
        } catch (InterruptedException | IOException e) {
//            if (e.getCause() instanceof UnknownHostException) {
//                YLogger.error("An error occurred while fetching Minecraft versions. Is there an internet connection?");
//            } else {
                YLogger.error("An error occurred while fetching Minecraft versions:");
//            }

            e.printStackTrace();
            return null;
        }

        List<String> mcVersions = new ArrayList<>();
        for (GameVersion gameVersion : versions) {
            if (!gameVersion.getVersionType().equalsIgnoreCase("release")) continue;

            String version = gameVersion.getVersion();
            mcVersions.add(version);

            if (version.equals(SUPPORTED_VERSION)) break;
        }

        return mcVersions;
    }

    public CompletableFuture<CheckResult> check() {
        if (isCheckRunning) return null;

        isCheckRunning = true;
        currentCheck.reset();
        CompletableFuture<CheckResult> future = new CompletableFuture<>();


        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            long startTimestamp = System.currentTimeMillis();

            // Get plugin and datapack files
            List<ProjectFile> pluginFiles = getPluginFiles();
            if (pluginFiles == null) {
                isCheckRunning = false;
                future.complete(null);
                return;
            }

            List<ProjectFile> dataPackFiles = getDataPackFiles();

            List<ProjectFile> files = new ArrayList<>(pluginFiles);
            files.addAll(dataPackFiles);

            currentCheck.setGoal(files.size());

            // Fetch Minecraft versions, they will be used later
            if (MINECRAFT_VERSIONS.isEmpty()) {
                List<String> versions = getMinecraftVersions();
                if (versions == null) {
                    isCheckRunning = false;
                    future.complete(null);
                    return;
                }

                MINECRAFT_VERSIONS.addAll(versions);
            }

            // Hash files
            List<String> hashes = new ArrayList<>();
            for (ProjectFile projectFile : files) {
                File file = projectFile.file();

                HashCode hash;
                try {
                    hash = Files.asByteSource(file).hash(Hashing.sha512());
                    hashes.add(hash.toString());
                } catch (IOException e) {
                    YLogger.error(String.format("An error occurred while hashing file '%s'!", file.getName()));
                    e.printStackTrace();
                }

                currentCheck.incrementProgress();
            }

            // Check hashes
            currentCheck.incrementState();
            List<ProjectVersion> currentVersions;
            try {
                currentCheck.incrementRequests();
                currentVersions = modrinthAPI.getVersionFiles(hashes);
            } catch (InterruptedException | IOException e) {
                YLogger.error("An error occurred while getting project version files from hashes.");
                e.printStackTrace();
                return;
            }
            if (currentVersions == null) return;


            // Get projects of the gotten versions
            currentCheck.incrementState();
            List<Project> projects;
            try {
                currentCheck.incrementRequests();

                List<String> projectIds = currentVersions.stream().map(ProjectVersion::getProjectId).toList();
                projects = modrinthAPI.getProjects(projectIds);
            } catch (InterruptedException | IOException e) {
                YLogger.error("An error occurred while getting projects data.");
                e.printStackTrace();
                return;
            }
            if (projects == null) return;


            //// Compose nice results
            List<ProjectCheckResult> pluginResults = new ArrayList<>();
            List<ProjectCheckResult> dataPackResults = new ArrayList<>();

            currentCheck.incrementState();
            currentCheck.setProgress(0);
            currentCheck.setGoal(projects.size());


            // Split projects per every "thread"
            List<List<Project>> projectsPerThread = new ArrayList<>();
            for (int i = 0; i < PluginConfig.threads; i++) {
                projectsPerThread.add(new ArrayList<>());
            }

            for (int i = 0; i < projects.size(); i++) {
                int threadIndex = i % PluginConfig.threads;
                projectsPerThread.get(threadIndex).add(projects.get(i));
            }

            // Run through 'em projects
            for (List<Project> projectList : projectsPerThread) {
                if (projectList.isEmpty()) continue;

                Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                    for (Project project : projectList) {
                        // Get this project's currently used version
                        ProjectVersion currentVersion = null;
                        synchronized (currentVersions) {
                            for (ProjectVersion ver : currentVersions) {
                                if (ver.getProjectId().equals(project.getId())) {
                                    currentVersion = ver;
                                    break;
                                }
                            }

                            currentVersions.remove(currentVersion);
                        }


                        // Check for newer versions
                        boolean isDataPack = currentVersion.getLoaders().contains("datapack");

                        List<ProjectVersion> newVersions;
                        try {
                            currentCheck.incrementRequests();
                            currentCheck.incrementProgress();

                            List<String> loaders = isDataPack ? List.of("datapack") : SUPPORTED_LOADERS;
                            newVersions = modrinthAPI.getProjectVersions(project.getSlug(), loaders, MINECRAFT_VERSIONS);
                        } catch (InterruptedException | IOException e) {
                            YLogger.error(String.format("An error occurred while getting project versions for '%s'!", project.getSlug()));
                            e.printStackTrace();
                            continue;
                        }
                        if (newVersions == null) continue;

                        // Create a project check result
                        ProjectCheckResult projectCheck = new ProjectCheckResult(project, currentVersion, newVersions);
                        if (isDataPack) dataPackResults.add(projectCheck);
                        else pluginResults.add(projectCheck);
                    }

                    // Check if all threads are finished
                    if (currentCheck.getProgress() != currentCheck.getGoal()) return;

                    // Sort everything and create the end result
                    pluginResults.sort(PROJECT_COMPARATOR);
                    dataPackResults.sort(PROJECT_COMPARATOR);

                    long endTimestamp = System.currentTimeMillis();
                    CheckResult.Times times = new CheckResult.Times(startTimestamp, endTimestamp);
                    CheckResult result = new CheckResult(pluginResults, pluginFiles.size(), dataPackResults, dataPackFiles.size(), times, currentCheck.getRequestsSent());
                    lastCheck = result;

                    isCheckRunning = false;
                    future.complete(result);
                });
            }
        });

        return future;
    }

    private List<ProjectFile> getPluginFiles() {
        File pluginsFolder = Bukkit.getPluginsFolder();
        File[] plugins = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));

        if (plugins == null) {
            YLogger.error("Couldn't read plugin files!");
            return null;
        }

        List<ProjectFile> result = new ArrayList<>();
        for (File plugin : plugins) {
            if (plugin.isDirectory()) continue;

            result.add(new ProjectFile(plugin, ProjectFile.Type.PLUGIN));
        }

        return result;
    }

    private List<ProjectFile> getDataPackFiles() {
        List<ProjectFile> result = new ArrayList<>();

        for (World world : Bukkit.getWorlds()) {
            File folder = world.getWorldFolder();
            File dataPacksFolder = new File(folder, "datapacks");
            if (!dataPacksFolder.exists()) continue;

            File[] zips = dataPacksFolder.listFiles((dir, name) -> name.endsWith(".zip"));
            if (zips == null) {
                YLogger.error(String.format("Couldn't read datapack files of world '%s'!", world.getName()));
                continue;
            }

            for (File zip : zips) {
                if (zip.isDirectory()) continue;

                result.add(new ProjectFile(zip, ProjectFile.Type.DATAPACK));
            }
        }

        return result;
    }

    public boolean isCheckRunning() {
        return isCheckRunning;
    }

    public CurrentCheck getCurrentCheck() {
        return currentCheck;
    }

    public CheckResult getLastCheck() {
        return lastCheck;
    }

    private static List<String> getSupportedLoaders() {
        if (isRunningPurpur()) return List.of("bukkit", "spigot", "paper", "purpur");
        if (isRunningPaper()) return List.of("bukkit", "spigot", "paper");
        if (isRunningSpigot()) return List.of("bukkit", "spigot");

        return List.of("bukkit");
    }

    private static boolean isRunningPurpur() {
        try {
            Class.forName("org.purpurmc.purpur.PurpurConfig");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isRunningPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean isRunningSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public enum CheckState {
        HASHING,
        CHECKING_HASHES,
        GETTING_PROJECTS,
        GETTING_VERSIONS
    }

    public class CurrentCheck {
        private CheckState state = CheckState.HASHING;
        private final AtomicInteger progress = new AtomicInteger();
        private final AtomicInteger goal = new AtomicInteger();
        private final AtomicInteger requestsSent = new AtomicInteger();

        private CurrentCheck() { }

        private void reset() {
            state = CheckState.HASHING;
            progress.set(0);
            goal.set(0);
            requestsSent.set(0);
        }

        private void incrementState() {
            switch (state) {
                case HASHING -> state = CheckState.CHECKING_HASHES;
                case CHECKING_HASHES -> state = CheckState.GETTING_PROJECTS;
                case GETTING_PROJECTS -> state = CheckState.GETTING_VERSIONS;
            }
        }

        private void incrementProgress() {
            progress.incrementAndGet();
        }

        private void incrementRequests() {
            requestsSent.incrementAndGet();
        }

        private void setProgress(int value) {
            progress.set(value);
        }

        private void setGoal(int value) {
            goal.set(value);
        }

        public CheckState getState() {
            return state;
        }

        public int getProgress() {
            return progress.get();
        }

        public int getGoal() {
            return goal.get();
        }

        public int getRequestsSent() {
            return requestsSent.get();
        }
    }
}
