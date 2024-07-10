package pl.ynfuien.yupdatechecker.core.modrinth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.moznion.uribuildertiny.URIBuilderTiny;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.core.modrinth.model.GameVersion;
import pl.ynfuien.yupdatechecker.core.modrinth.model.Project;
import pl.ynfuien.yupdatechecker.core.modrinth.model.ProjectVersion;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class ModrinthAPI {
    private final static String BASE_URL = "https://api.modrinth.com/v2";

    private final HttpClient httpClient;
    private final String userAgent;

    private final AtomicInteger ratelimitLimit = new AtomicInteger(300);
    private final AtomicInteger ratelimitLeft = new AtomicInteger(300);
    private final AtomicInteger ratelimitReset = new AtomicInteger(60);

    private final static Gson GSON = new Gson();
    private final static Type GSON_MAP_MAP_TYPE = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
    private final static Type GSON_ARRAY_TYPE = new TypeToken<List<Map<String, Object>>>(){}.getType();

    public ModrinthAPI(UserAgent agent) {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        userAgent = agent.build();
    }

    public List<Project> getProjects(List<String> ids) throws InterruptedException, IOException {
        String url = String.format("%s/projects", BASE_URL);

        HashMap<String, String> params = new HashMap<>();
        params.put("ids", GSON.toJson(ids));

        String response = sendRequest(url, params);
        if (response == null) return null;

        List<Map<String, Object>> list = GSON.fromJson(response, GSON_ARRAY_TYPE);

        List<Project> projects = new ArrayList<>();
        for (Map<String, Object> item : list) {
            Project project = new Project(item);
            projects.add(project);
        }

        return projects;
    }

    public List<ProjectVersion> getProjectVersions(String slug, List<String> loaders, List<String> gameVersions) throws InterruptedException, IOException {
        String url = String.format("%s/project/%s/version", BASE_URL, slug);

        HashMap<String, String> params = new HashMap<>();
        params.put("loaders", GSON.toJson(loaders));
        params.put("game_versions", GSON.toJson(gameVersions));

        String response = sendRequest(url, params);
        if (response == null) return null;

        List<Map<String, Object>> list = GSON.fromJson(response, GSON_ARRAY_TYPE);

        List<ProjectVersion> versions = new ArrayList<>();
        for (Map<String, Object> item : list) {
            ProjectVersion version = new ProjectVersion(item);
            versions.add(version);
        }

        return versions;
    }

    public List<ProjectVersion> getVersionFiles(List<String> hashes) throws InterruptedException, IOException {
        String url = String.format("%s/version_files", BASE_URL);

        JsonObject payload = new JsonObject();
        payload.addProperty("algorithm", "sha512");
        payload.add("hashes", GSON.toJsonTree(hashes).getAsJsonArray());

        String json = GSON.toJson(payload);

        String response = sendRequest(url, null, json);
        if (response == null) return null;

        Map<String, Map<String, Object>> map = GSON.fromJson(response, GSON_MAP_MAP_TYPE);

        List<ProjectVersion> versions = new ArrayList<>();
        for (Map<String, Object> item : map.values()) {
            ProjectVersion version = new ProjectVersion(item);
            versions.add(version);
        }

        return versions;
    }

    public List<GameVersion> getGameVersionTags() throws InterruptedException, IOException {
        String url = String.format("%s/tag/game_version", BASE_URL);

        String response = sendRequest(url);
        if (response == null) return null;

        List<Map<String, Object>> list = GSON.fromJson(response, GSON_ARRAY_TYPE);

        List<GameVersion> versions = new ArrayList<>();
        for (Map<String, Object> item : list) {
            GameVersion version = new GameVersion(item);
            versions.add(version);
        }

        return versions;
    }

    private String sendRequest(String url) throws IOException, InterruptedException {
        return sendRequest(url, null, null);
    }

    private String sendRequest(String url, HashMap<String, String> queryParams) throws IOException, InterruptedException {
        return sendRequest(url, queryParams, null);
    }

    private String sendRequest(String url, HashMap<String, String> queryParams, String postData) throws IOException, InterruptedException {
        URIBuilderTiny builder = new URIBuilderTiny(url);
        if (queryParams != null && !queryParams.isEmpty()) builder.addQueryParameters(queryParams);

        return sendRequest(builder.build(), postData);
    }

    private String sendRequest(URI uri, String postData) throws IOException, InterruptedException {
        // Check if we didn't hit the request limit and wait if needed
        if (ratelimitLeft.get() <= 0) {
            Thread.sleep(((long) ratelimitReset.get() * 1000) + 1000);

            synchronized (ratelimitLeft) {
                if (ratelimitLeft.get() <= 0) ratelimitLeft.set(ratelimitLimit.get());
            }

            // Perform the request
            return sendRequest(uri, postData);
        }
        ratelimitLeft.decrementAndGet();

        // Create the request
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("User-Agent", userAgent)
                .GET();

        if (postData != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(postData))
                    .header("Content-Type", "application/json");
        }

        // And perform it
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        // Set remaining request limit from the headers
        HttpHeaders headers = response.headers();
        ratelimitLimit.set(Integer.parseInt(headers.firstValue("X-Ratelimit-Limit").orElse("300")));
        ratelimitLeft.set(Integer.parseInt(headers.firstValue("X-Ratelimit-Remaining").orElse("300")));
        ratelimitReset.set(Integer.parseInt(headers.firstValue("X-Ratelimit-Reset").orElse("60")));

        // Too many requests
        int code = response.statusCode();
        if (code == 429) {
            YLogger.error("Modrinth API ratelimit has been hit! Waiting for it to reset...");
            ratelimitLeft.set(0);
            return sendRequest(uri, postData);
        }

        return code == 200 ? response.body() : null;
    }

    public record UserAgent(String author, String projectName, String version, String contact) {
        public String build() {
            return String.format("%s/%s/%s (%s)", author, projectName, version, contact);
        }
    }
}
