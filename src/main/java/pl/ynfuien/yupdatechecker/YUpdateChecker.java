package pl.ynfuien.yupdatechecker;

import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.main.ModrinthAPI;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import pl.ynfuien.ydevlib.config.ConfigHandler;
import pl.ynfuien.ydevlib.config.ConfigObject;
import pl.ynfuien.ydevlib.messages.YLogger;
import pl.ynfuien.yupdatechecker.commands.main.MainCommand;
import pl.ynfuien.yupdatechecker.commands.updates.UpdatesCommand;
import pl.ynfuien.yupdatechecker.config.ConfigName;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.Checker;

import java.util.HashMap;

public final class YUpdateChecker extends JavaPlugin {
    private static YUpdateChecker instance;
    private ModrinthAPI modrinthAPI = ModrinthAPI.rateLimited(UserAgent.builder()
            .authorUsername("Ynfuien")
            .projectName("YUpdateChecker")
            .projectVersion(getPluginMeta().getVersion())
            .contact("ynfuien@gmail.com")
            .build(), "");
    private Checker checker = new Checker(this);
    private final ConfigHandler configHandler = new ConfigHandler(this);
    private ConfigObject config;

    @Override
    public void onEnable() {
        instance = this;
        YLogger.setup("<dark_aqua>[<aqua>Y<gradient:white:#1BD96A>UpdateChecker</gradient><dark_aqua>] <white>", getComponentLogger());
//        YLogger.setDebugging(true);


        // Configs
        loadConfigs();
        loadLang();

        config = configHandler.getConfigObject(ConfigName.CONFIG);
        PluginConfig.load(config.getConfig());

        // Commands
        setupCommands();

        YLogger.info("Plugin successfully <green>enabled<white>!");
    }

    @Override
    public void onDisable() {
        YLogger.info("Plugin successfully <red>disabled<white>!");
    }


    private void setupCommands() {
        HashMap<String, CommandExecutor> commands = new HashMap<>();
        commands.put("yupdatechecker", new MainCommand(this));
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
