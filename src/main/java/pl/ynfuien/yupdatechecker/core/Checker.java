package pl.ynfuien.yupdatechecker.core;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import masecla.modrinth4j.endpoints.version.GetProjectVersions;
import masecla.modrinth4j.main.ModrinthAPI;
import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.tags.GameVersion;
import masecla.modrinth4j.model.version.FileHash;
import masecla.modrinth4j.model.version.ProjectVersion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.config.PluginConfig;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Checker {
    private final YUpdateChecker instance;
    private final ModrinthAPI modrinthAPI;

    private static final List<String> SUPPORTED_LOADERS = getSupportedLoaders();
    private static final List<String> MINECRAFT_VERSIONS = new ArrayList<>();
    private static final String SUPPORTED_VERSION = Bukkit.getMinecraftVersion();

    private CheckResult lastCheck = null;

    private boolean isCheckRunning = false;
    private int currentCheckState = 0;
    private int currentCheckGoal = 0;

    public Checker(YUpdateChecker instance) {
        this.instance = instance;
        this.modrinthAPI = instance.getModrinthAPI();
    }

    private List<String> getMinecraftVersions() {
        List<GameVersion> versions;
        try {
            versions = modrinthAPI.tags().getGameVersions().get();
        } catch (InterruptedException | ExecutionException e) {
            if (e.getCause() instanceof UnknownHostException) {
                YLogger.error("An error occurred while fetching Minecraft versions. Is there an internet connection?");
            } else {
                YLogger.error("An error occurred while fetching Minecraft versions:");
            }

            e.printStackTrace();
            return null;
        }

        List<String> mcVersions = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            GameVersion gameVersion = versions.get(i);
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
        CompletableFuture<CheckResult> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
            long startTimestamp = System.currentTimeMillis();

            List<ProjectFile> pluginFiles = getPluginFiles();
            if (pluginFiles == null) {
                isCheckRunning = false;
                future.complete(null);
                return;
            }

            List<ProjectFile> dataPackFiles = getDataPackFiles();

            List<ProjectFile> files = new ArrayList<>(pluginFiles);
            files.addAll(dataPackFiles);

            List<ProjectCheckResult> pluginResults = new ArrayList<>();
            List<ProjectCheckResult> dataPackResults = new ArrayList<>();
            currentCheckState = 0;
            currentCheckGoal = files.size();

            // Fetch newer Minecraft versions
            if (MINECRAFT_VERSIONS.isEmpty()) {
                List<String> versions = getMinecraftVersions();
                if (versions == null) {
                    isCheckRunning = false;
                    future.complete(null);
                    return;
                }

                MINECRAFT_VERSIONS.addAll(versions);
            }

            List<List<ProjectFile>> threadFiles = new ArrayList<>();
            for (int i = 0; i < PluginConfig.threads; i++) {
                threadFiles.add(new ArrayList<>());
            }

            for (int i = 0; i < files.size(); i++) {
                int threadIndex = i % threadFiles.size();
                threadFiles.get(threadIndex).add(files.get(i));
            }


            for (List<ProjectFile> fileList : threadFiles) {
                Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                    for (ProjectFile file : fileList) {
                        ProjectCheckResult result = checkFile(file);
                        currentCheckState++;
                        if (result == null) continue;

                        if (file.type().equals(ProjectFile.Type.PLUGIN)) {
                            pluginResults.add(result);
                            continue;
                        }

                        dataPackResults.add(result);
                    }

                    if (currentCheckState != currentCheckGoal) return;

                    Comparator<ProjectCheckResult> comparator = (left, right) -> {
                        return left.project().getTitle().compareToIgnoreCase(right.project().getTitle());
                    };

                    pluginResults.sort(comparator);
                    dataPackResults.sort(comparator);

                    long endTimestamp = System.currentTimeMillis();
                    CheckResult.Times times = new CheckResult.Times(startTimestamp, endTimestamp);
                    CheckResult result = new CheckResult(pluginResults, pluginFiles.size(), dataPackResults, dataPackFiles.size(), times);
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

    private ProjectCheckResult checkFile(ProjectFile projectFile) {
        File file = projectFile.file();
        ProjectFile.Type type = projectFile.type();

        HashCode hash;
        try {
            hash = Files.asByteSource(file).hash(Hashing.sha512());
        } catch (IOException e) {
            YLogger.error(String.format("An error occurred while hashing file '%s'!", file.getName()));
            e.printStackTrace();
            return null;
        }


        ProjectVersion currentVersion;
        try {
            currentVersion = modrinthAPI.versions().files().getVersionByHash(FileHash.SHA512, hash.toString()).get();
        } catch (InterruptedException|ExecutionException e) {
            YLogger.error(String.format("An error occurred while getting current project version of file '%s'!", file.getName()));
            e.printStackTrace();
            return null;
        }
        if (currentVersion == null) return null;


        Project project;
        try {
            project = modrinthAPI.projects().get(currentVersion.getProjectId()).get();
        } catch (InterruptedException|ExecutionException e) {
            YLogger.error(String.format("An error occurred while getting project with id '%s'!", currentVersion.getProjectId()));
            e.printStackTrace();
            return null;
        }
        if (project == null) return null;

        boolean isDataPack = type.equals(ProjectFile.Type.DATAPACK);
        List<ProjectVersion> versions;
        try {
            GetProjectVersions.GetProjectVersionsRequest request = GetProjectVersions.GetProjectVersionsRequest
                    .builder()
                    .loaders(isDataPack ? List.of("datapack") : SUPPORTED_LOADERS)
                    .gameVersions(MINECRAFT_VERSIONS)
                    .build();
            versions = modrinthAPI.versions().getProjectVersions(project.getSlug(), request).get();
        } catch (InterruptedException|ExecutionException e) {
            YLogger.error(String.format("An error occurred while getting project versions for '%s'!", project.getSlug()));
            e.printStackTrace();
            return null;
        }
        if (versions == null) return null;

        return new ProjectCheckResult(project, currentVersion, versions);
    }

    public boolean isCheckRunning() {
        return isCheckRunning;
    }

    public int getCurrentCheckState() {
        return currentCheckState;
    }

    public int getCurrentCheckGoal() {
        return currentCheckGoal;
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
}
