package pl.ynfuien.yupdatechecker.config;

import org.bukkit.configuration.ConfigurationSection;
import pl.ynfuien.ydevlib.messages.YLogger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PluginConfig {
    public static boolean onStartup = false;
    public static int threads = 2;
    public static Set<String> considerChannels = new HashSet<>();

    public static boolean actionBarEnable = true;
    public static int actionBarInterval = 40;
    public static int pageSize = 10;

    public static int confirmDuration = 10;

    public static void load(ConfigurationSection config) {
        ConfigurationSection updateCheck = config.getConfigurationSection("update-check");

        onStartup = updateCheck.getBoolean("on-startup");

        threads = updateCheck.getInt("threads");
        if (threads < 1) {
            YLogger.warn("Threads count can't be lower than 1! One thread will be used.");
            threads = 1;
        }

        if (updateCheck.isList("consider-channels")) {
            considerChannels.clear();

            List<String> channels = updateCheck.getStringList("consider-channels");
            for (String channel : channels) {
                channel = channel.toLowerCase();

                if (!channel.equals("release") && !channel.equals("beta") && !channel.equals("alpha")) {
                    YLogger.error(String.format("Release channel '%s' is incorrect! It won't be used.", channel));
                    continue;
                }

                considerChannels.add(channel);
            }

            if (considerChannels.isEmpty()) {
                considerChannels = Set.of("release", "beta");
                YLogger.warn("No release channels to consider are specified! Will use release and beta channels.");
            }
        }

        ConfigurationSection command = config.getConfigurationSection("command");
        actionBarEnable = command.getBoolean("action-bar.enable");
        actionBarInterval = command.getInt("action-bar.interval");

        pageSize = command.getInt("page-size");

        confirmDuration = command.getInt("confirm-duration");
    }
}
