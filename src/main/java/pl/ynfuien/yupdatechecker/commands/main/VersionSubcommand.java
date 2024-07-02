package pl.ynfuien.yupdatechecker.commands.main;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.command.CommandSender;
import pl.ynfuien.yupdatechecker.Lang;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.commands.Subcommand;

import java.util.HashMap;
import java.util.List;

public class VersionSubcommand implements Subcommand {
    private final YUpdateChecker instance;

    public VersionSubcommand(YUpdateChecker instance) {
        this.instance = instance;
    }

    @Override
    public String permission() {
        return "yupdatechecker.admin";
    }

    @Override
    public String name() {
        return "version";
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public String usage() {
        return null;
    }

    @Override
    public void run(CommandSender sender, String[] args, HashMap<String, Object> placeholders) {
        PluginMeta info = instance.getPluginMeta();

        placeholders.put("name", info.getName());
        placeholders.put("version", info.getVersion());
        placeholders.put("author", info.getAuthors().get(0));
        placeholders.put("description", info.getDescription());
        placeholders.put("website", info.getWebsite());

        Lang.Message.COMMAND_MAIN_VERSION.send(sender, placeholders);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return null;
    }
}
