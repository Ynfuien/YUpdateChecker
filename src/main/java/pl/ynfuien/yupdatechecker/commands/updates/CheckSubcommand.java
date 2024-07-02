package pl.ynfuien.yupdatechecker.commands.updates;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.ynfuien.yupdatechecker.Lang;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.commands.Subcommand;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.Checker;

import java.util.HashMap;
import java.util.List;

public class CheckSubcommand implements Subcommand {
    private final YUpdateChecker instance;
    private final Checker checker;

    public CheckSubcommand(YUpdateChecker instance) {
        this.instance = instance;
        this.checker = instance.getChecker();
    }

    @Override
    public String permission() {
        return "yupdatechecker.updates."+name();
    }

    @Override
    public String name() {
        return "check";
    }

    @Override
    public String description() {
        return Lang.Message.COMMAND_UPDATES_CHECK_DESCRIPTION.get();
    }

    @Override
    public String usage() {
        return null;
    }

    @Override
    public void run(CommandSender sender, String[] args, HashMap<String, Object> placeholders) {
        if (checker.isCheckRunning()) {
            Lang.Message.COMMAND_UPDATES_CHECK_IS_RUNNING.send(sender, placeholders);
            return;
        }

        Lang.Message.COMMAND_UPDATES_CHECK_START.send(sender, placeholders);

        checker.check().thenAcceptAsync((checkResult) -> {
            if (checkResult == null) {
                Lang.Message.COMMAND_UPDATES_CHECK_FAIL.send(sender, placeholders);
                return;
            }

            Lang.Message.COMMAND_UPDATES_CHECK_FINISH.send(sender, placeholders);
        });


        if (!PluginConfig.actionBarEnable) return;
        if (!(sender instanceof Player p)) return;

        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, (task) -> {
            if (!p.isOnline()) {
                task.cancel();
                return;
            }

            placeholders.put("state", checker.getCurrentCheckState());
            placeholders.put("goal", checker.getCurrentCheckGoal());

            Component message = Lang.Message.CHECK_PROGRESS_ACTION_BAR.getComponent(sender, placeholders);
            p.sendActionBar(message);

            if (!checker.isCheckRunning()) task.cancel();
        }, 5, PluginConfig.actionBarInterval);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return List.of();
    }
}
