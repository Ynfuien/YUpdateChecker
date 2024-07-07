package pl.ynfuien.yupdatechecker.core.modrinth;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.moznion.uribuildertiny.URIBuilderTiny;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.core.modrinth.model.Project;
import pl.ynfuien.yupdatechecker.core.modrinth.model.GameVersion;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

// Inspiration from Modrinth4J
// https://github.com/masecla22/Modrinth4J
public class ModrinthAPI {
    private final static String BASE_URL = "https://api.modrinth.com/v2";

    private final HttpClient httpClient;
    private final String userAgent;

    private final AtomicInteger requestCount = new AtomicInteger(300);
    private final AtomicInteger timeLeft = new AtomicInteger(60);

    private final static Gson GSON = new Gson();
    private final static Type GSON_MAP_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
    private final static Type GSON_ARRAY_TYPE = new TypeToken<List<Map<String, Object>>>(){}.getType();

    public ModrinthAPI(UserAgent agent) {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        userAgent = agent.build();
    }

    public Project getProject(String slug) throws InterruptedException, ExecutionException {
        String url = String.format("%s/project/%s", BASE_URL, slug);

        String response = sendRequest(url);
        if (response == null) return null;

        Map<String, Object> map = GSON.fromJson(response, GSON_MAP_TYPE);

        if (map.containsKey("error")) {
            String error = (String) map.get("error");
            String description = (String) map.get("description");

            YLogger.error("Modrinth API returned an error:");
            YLogger.error("Error: " + error);
            YLogger.error("Description: " + description);
            return null;
        }

        return new Project(map);
    }

    public List<ProjectVersion> getProjectVersions(String slug, List<String> loaders, List<String> gameVersions) throws InterruptedException, ExecutionException {
        String url = String.format("%s/project/%s/version", BASE_URL, slug);

        HashMap<String, String> params = new HashMap<>();
        params.put("loaders", GSON.toJson(loaders));
        params.put("gameVersions", GSON.toJson(gameVersions));

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

    public ProjectVersion getVersionFile(String hash) throws InterruptedException, ExecutionException {
        String url = String.format("%s/version_file/%s", BASE_URL, hash);

        String response = sendRequest(url);
        if (response == null) return null;

        Map<String, Object> map = GSON.fromJson(response, GSON_MAP_TYPE);
        if (map == null) return null;

        if (map.containsKey("error")) {
            String error = (String) map.get("error");
            String description = (String) map.get("description");

            YLogger.error("Modrinth API returned an error:");
            YLogger.error("Error: " + error);
            YLogger.error("Description: " + description);
            return null;
        }

        return new ProjectVersion(map);
    }

    public List<GameVersion> getGameVersionTags() throws InterruptedException, ExecutionException {
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

    private String sendRequest(String url) {
        return sendRequest(url, null);
    }

    private String sendRequest(String url, HashMap<String, String> queryParams) {
        URIBuilderTiny builder = new URIBuilderTiny(url);
        if (queryParams != null && !queryParams.isEmpty()) builder.addQueryParameters(queryParams);

        return sendRequest(builder.build());
    }

    private String sendRequest(URI uri) {
        // Wait if needed
        if (requestCount.get() <= 0) {
            try {
                Thread.sleep(((long) timeLeft.get() * 1000) + 1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return sendRequest(uri);
        }

        requestCount.decrementAndGet();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET().header("User-Agent", userAgent).build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException|InterruptedException e) {
            YLogger.error("An error occurred while connecting to the Modrinth API:");
            e.printStackTrace();
            return null;
        }

        HttpHeaders headers = response.headers();
        requestCount.set(Integer.parseInt(headers.firstValue("X-Ratelimit-Remaining").orElse("300")));
        timeLeft.set(Integer.parseInt(headers.firstValue("X-Ratelimit-Reset").orElse("60")));

        if (response.statusCode() != 200) {
            YLogger.info("Headers:");
            YLogger.info(headers.toString());

            return null;
        }

        return response.body();
    }

    public record UserAgent(String author, String projectName, String version, String contact) {
        public String build() {
            return String.format("%s/%s/%s (%s)", author, projectName, version, contact);
        }
    }
}
