package pl.ynfuien.yupdatechecker.commands.updates;

import masecla.modrinth4j.model.project.Project;
import masecla.modrinth4j.model.version.ProjectVersion;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import pl.ynfuien.ydevlib.utils.CommonPlaceholders;
import pl.ynfuien.yupdatechecker.Lang;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.commands.Subcommand;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.CheckResult;
import pl.ynfuien.yupdatechecker.core.Checker;
import pl.ynfuien.yupdatechecker.core.ProjectCheckResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShowSubcommand implements Subcommand {
    private final YUpdateChecker instance;
    private final Checker checker;

    public ShowSubcommand(YUpdateChecker instance) {
        this.instance = instance;
        this.checker = instance.getChecker();
    }

    @Override
    public String permission() {
        return "yupdatechecker.updates."+name();
    }

    @Override
    public String name() {
        return "show";
    }

    @Override
    public String description() {
        return Lang.Message.COMMAND_UPDATES_SHOW_DESCRIPTION.get();
    }

    @Override
    public String usage() {
        return "[plugins | datapacks] [page]";
    }

    @Override
    public void run(CommandSender sender, String[] args, HashMap<String, Object> placeholders) {
        CheckResult result = checker.getLastCheck();
        // No check available
        if (result == null) {
            if (checker.isCheckRunning()) {
                Lang.Message.COMMAND_UPDATES_SHOW_FAIL_CHECK_RUNNING.send(sender, placeholders);
                return;
            }

            Lang.Message.COMMAND_UPDATES_SHOW_FAIL_NO_CHECK.send(sender, placeholders);
            return;
        }

        // Check available, and new one is running
        if (checker.isCheckRunning()) {
            Lang.Message.COMMAND_UPDATES_SHOW_OLD_RUNNING_NEW.send(sender, placeholders);
        }

        // General counts
        if (args.length == 0) {
            // Times
            CheckResult.Times times = result.times();
            CommonPlaceholders.setDateTime(placeholders, times.start(), "time-start");
            CommonPlaceholders.setDateTime(placeholders, times.end(), "time-end");
            CommonPlaceholders.setDuration(placeholders, times.duration(), "time-duration");

            // Plugins
            placeholders.put("plugins-all", result.allPluginsCount());
            placeholders.put("plugins-modrinth", result.plugins().size());
            placeholders.put("plugins-up-to-date", result.upToDatePluginsCount());
            placeholders.put("plugins-outdated", result.outdatedPluginsCount());

            // Datapacks
            placeholders.put("datapacks-all", result.allDataPacksCount());
            placeholders.put("datapacks-modrinth", result.dataPacks().size());
            placeholders.put("datapacks-up-to-date", result.upToDateDataPacksCount());
            placeholders.put("datapacks-outdated", result.outdatedDataPacksCount());

            Lang.Message.COMMAND_UPDATES_SHOW.send(sender, placeholders);
            return;
        }


        // Plugins / datapacks
        String arg1 = args[0].toLowerCase();
        boolean plugins = arg1.equals("plugins");
        if (!plugins && !arg1.equals("datapacks")) {
            Lang.Message.COMMAND_UPDATES_SHOW_USAGE.send(sender, placeholders);
            return;
        }

        Messages messages = new Messages(plugins);

        List<ProjectCheckResult> results = plugins ? result.plugins() : result.dataPacks();
        int resultsCount = results.size();
        placeholders.put("count", resultsCount);

        if (resultsCount == 0) {
            messages.EMPTY.send(sender, placeholders);
            return;
        }

        placeholders.put("minecraft-version", Bukkit.getMinecraftVersion());
        String channels = String.join(", ", PluginConfig.considerChannels.stream().map(versionType -> versionType.name().toLowerCase()).toList());
        placeholders.put("release-channels", channels);

        int pageNumber = 1;
        if (args.length > 1) {
            try {
                pageNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {}
        }

        if (pageNumber < 1) pageNumber = 1;

        int pageSize = PluginConfig.pageSize;
        int pageCount = (int) Math.ceil(resultsCount / (double) pageSize);
        if (pageNumber > pageCount) pageNumber = pageCount;

        placeholders.put("current-page", pageNumber);
        placeholders.put("page-count", pageCount);
        boolean isPreviousPage = pageNumber > 1;
        boolean isNextPage = pageNumber < pageCount;
        if (isPreviousPage) placeholders.put("previous-page", pageNumber - 1);
        if (isNextPage) placeholders.put("next-page", pageNumber + 1);

        messages.HEADER.send(sender, placeholders);


        int margin = (pageNumber - 1) * pageSize;
        for (int i = 0; i < pageSize; i++) {
            int index = margin + i;
            if (index > resultsCount - 1) break;

            ProjectCheckResult projectResult = results.get(index);

            placeholders.put("number", index + 1);
            placeholders.put("versions-behind", projectResult.versionsBehind());

            Project project = projectResult.project();
            placeholders.put("project-id", project.getId());
            placeholders.put("project-slug", project.getSlug());
            placeholders.put("project-title", project.getTitle());
            placeholders.put("project-color", project.getColor());
            placeholders.put("project-color-hex", String.format("%06x", project.getColor()));
            placeholders.put("project-description", project.getDescription());
            placeholders.put("project-downloads", project.getDownloads());
            placeholders.put("project-followers", project.getFollowers());

            ProjectVersion currentVer = projectResult.currentVersion();
            placeholders.put("current-version-id", currentVer.getId());
            placeholders.put("current-version-name", currentVer.getName());
            placeholders.put("current-version-number", currentVer.getVersionNumber());
            placeholders.put("current-version-changelog", currentVer.getChangelog());
            placeholders.put("current-version-date-published", currentVer.getDatePublished());

            ProjectVersion latestVer = projectResult.latestVersion();
            placeholders.put("new-version-id", latestVer.getId());
            placeholders.put("new-version-name", latestVer.getName());
            placeholders.put("new-version-number", latestVer.getVersionNumber());
            placeholders.put("new-version-changelog", latestVer.getChangelog());
            placeholders.put("new-version-date-published", latestVer.getDatePublished());

            String url = messages.URL.get(placeholders);
            placeholders.put("url", url);

            if (projectResult.upToDate()) {
                if (projectResult.isNewerReleaseForDifferentChannel()) {
                    messages.ENTRY_UP_TO_DATE_RELEASE.send(sender, placeholders);
                    continue;
                }

                if (projectResult.isNewerReleaseForNewerMcVersion()) {
                    messages.ENTRY_UP_TO_DATE_VERSION.send(sender, placeholders);
                    continue;
                }

                messages.ENTRY_UP_TO_DATE.send(sender, placeholders);
                continue;
            }

            messages.ENTRY_OUTDATED.send(sender, placeholders);
        }

        if (pageCount == 1) {
            messages.FOOTER_PAGE_SINGLE.send(sender, placeholders);
            return;
        }

        if (isPreviousPage) {
            if (isNextPage) {
                messages.FOOTER_PAGE_BOTH.send(sender, placeholders);
                return;
            }

            messages.FOOTER_PAGE_PREVIOUS.send(sender, placeholders);
            return;
        }

        messages.FOOTER_PAGE_NEXT.send(sender, placeholders);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length > 2) return completions;

        String arg1 = args[0].toLowerCase();
        if (args.length == 1) {
            for (String completion : new String[] {"plugins", "datapacks"}) {
                if (completion.startsWith(arg1)) completions.add(completion);
            }

            return completions;
        }

        CheckResult result = checker.getLastCheck();
        if (result == null) return completions;

        boolean plugins = arg1.equals("plugins");
        if (!plugins && !arg1.equals("datapacks")) return completions;

        List<ProjectCheckResult> results = plugins ? result.plugins() : result.dataPacks();
        int pageCount = (int) Math.ceil(results.size() / (double) PluginConfig.pageSize);
        if (pageCount < 2) return completions;

        String arg2 = args[1];
        for (int i = 0; i < pageCount; i++) {
            String value = String.valueOf(i + 1);

            if (value.startsWith(arg2)) completions.add(value);
        }

        return completions;
    }

    protected class Messages {
        protected final Lang.Message EMPTY;
        protected final Lang.Message HEADER;
        protected final Lang.Message ENTRY_OUTDATED;
        protected final Lang.Message ENTRY_UP_TO_DATE;
        protected final Lang.Message ENTRY_UP_TO_DATE_RELEASE;
        protected final Lang.Message ENTRY_UP_TO_DATE_VERSION;
        protected final Lang.Message URL;
        protected final Lang.Message FOOTER_PAGE_NEXT;
        protected final Lang.Message FOOTER_PAGE_BOTH;
        protected final Lang.Message FOOTER_PAGE_PREVIOUS;
        protected final Lang.Message FOOTER_PAGE_SINGLE;

        protected Messages(boolean plugins) {
            EMPTY = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_EMPTY : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_EMPTY;
            HEADER = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_HEADER : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_HEADER;
            ENTRY_OUTDATED = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_OUTDATED : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_OUTDATED;
            ENTRY_UP_TO_DATE = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE;
            ENTRY_UP_TO_DATE_RELEASE = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE_RELEASE : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE_RELEASE;
            ENTRY_UP_TO_DATE_VERSION = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE_VERSION : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE_VERSION;
            URL = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_URL : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_URL;
            FOOTER_PAGE_NEXT = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_NEXT : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_NEXT;
            FOOTER_PAGE_BOTH = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_BOTH : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_BOTH;
            FOOTER_PAGE_PREVIOUS = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_PREVIOUS : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_PREVIOUS;
            FOOTER_PAGE_SINGLE = plugins ? Lang.Message.COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_SINGLE : Lang.Message.COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_SINGLE;
        }
    }
}
