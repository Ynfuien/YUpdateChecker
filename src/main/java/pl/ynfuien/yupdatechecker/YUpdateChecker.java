package pl.ynfuien.yupdatechecker;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.ynfuien.ydevlib.config.ConfigHandler;
import pl.ynfuien.ydevlib.config.ConfigObject;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.commands.main.AdminCommand;
import pl.ynfuien.yupdatechecker.commands.updates.UpdatesCommand;
import pl.ynfuien.yupdatechecker.config.ConfigName;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.Checker;
import pl.ynfuien.yupdatechecker.core.modrinth.ModrinthAPI;

import java.util.HashMap;

public final class YUpdateChecker extends JavaPlugin {
    private static YUpdateChecker instance;

    private ModrinthAPI modrinthAPI = new ModrinthAPI(
            new ModrinthAPI.UserAgent(
                    "Ynfuien",
                    getPluginMeta().getName(),
                    getPluginMeta().getVersion(),
                    "ynfuien@gmail.com")
    );
    private final Checker checker = new Checker(this);

    private final ConfigHandler configHandler = new ConfigHandler(this);
    private ConfigObject config;

    @Override
    public void onEnable() {
        instance = this;
        YLogger.setup("<dark_aqua>[<aqua>Y<gradient:white:#1BD96A>UpdateChecker</gradient><dark_aqua>] <white>", getComponentLogger());

        // Configs
        loadConfigs();
        loadLang();

        config = configHandler.getConfigObject(ConfigName.CONFIG);
        PluginConfig.load(config.getConfig());

        // Commands
        setupCommands();

        // BStats
        new Metrics(this, 22505);

        // Startup update check
        if (PluginConfig.onStartup) {
            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> {
                HashMap<String, Object> placeholders = new HashMap<>() {{put("command", "updates");}};
                ConsoleCommandSender console = Bukkit.getConsoleSender();

                Lang.Message.COMMAND_UPDATES_CHECK_START.send(console, placeholders);

                checker.check().thenAcceptAsync((checkResult) -> {
                    if (checkResult == null) {
                        Lang.Message.COMMAND_UPDATES_CHECK_FAIL.send(console, placeholders);
                        return;
                    }

                    Lang.Message.COMMAND_UPDATES_CHECK_FINISH.send(console, placeholders);
                });
            });
        }

        YLogger.info("Plugin successfully <green>enabled<white>!");
    }

    @Override
    public void onDisable() {
        checker.stopCheck();

        YLogger.info("Plugin successfully <red>disabled<white>!");
    }


    private void setupCommands() {
        HashMap<String, CommandExecutor> commands = new HashMap<>();
        commands.put("yupdatechecker", new AdminCommand(this));
        commands.put("updates", new UpdatesCommand(this));

        for (String name : commands.keySet()) {
            CommandExecutor cmd = commands.get(name);

            getCommand(name).setExecutor(cmd);
            getCommand(name).setTabCompleter((TabCompleter) cmd);
        }
    }

    private void loadLang() {
        // Get lang config
        FileConfiguration config = configHandler.getConfig(ConfigName.LANG);

        // Reload lang
        Lang.loadLang(config);
    }

    private void loadConfigs() {
        configHandler.load(ConfigName.CONFIG);
        configHandler.load(ConfigName.LANG, true, true);
    }

    public boolean reloadPlugin() {
        // Reload all configs
        boolean fullSuccess = configHandler.reloadAll();

        PluginConfig.load(config.getConfig());

        // Reload lang
        instance.loadLang();

        return fullSuccess;
    }

    public static YUpdateChecker getInstance() {
        return instance;
    }

    public ModrinthAPI getModrinthAPI() {
        return modrinthAPI;
    }

    public Checker getChecker() {
        return checker;
    }
}
