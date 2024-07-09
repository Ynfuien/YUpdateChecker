package pl.ynfuien.yupdatechecker.commands.updates;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.ynfuien.ydevlib.utils.CommonPlaceholders;
import pl.ynfuien.yupdatechecker.Lang;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.commands.Subcommand;
import pl.ynfuien.yupdatechecker.config.PluginConfig;
import pl.ynfuien.yupdatechecker.core.CheckResult;
import pl.ynfuien.yupdatechecker.core.Checker;

import java.time.Duration;
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
            Lang.Message.COMMAND_UPDATES_CHECK_FAIL_IS_RUNNING.send(sender, placeholders);
            return;
        }

        // Recent check warning
        CheckResult lastCheck = checker.getLastCheck();
        if (lastCheck != null && !(args.length > 0 && args[0].equalsIgnoreCase("-y"))) {
            Duration dur = Duration.ofMillis(System.currentTimeMillis() - lastCheck.times().end());
            if (dur.toMinutes() < PluginConfig.confirmDuration) {
                CommonPlaceholders.setDuration(placeholders, dur, null);
                Lang.Message.COMMAND_UPDATES_CHECK_FAIL_RECENT.send(sender, placeholders);
                return;
            }
        }

        // Check start
        Lang.Message.COMMAND_UPDATES_CHECK_START.send(sender, placeholders);
        checker.check().thenAcceptAsync((checkResult) -> {
            if (checkResult == null) {
                Lang.Message.COMMAND_UPDATES_CHECK_FAIL.send(sender, placeholders);
                return;
            }

            Lang.Message.COMMAND_UPDATES_CHECK_FINISH.send(sender, placeholders);
        });


        // Action bar stuff
        if (!PluginConfig.actionBarEnable) return;
        if (!(sender instanceof Player p)) return;

        Checker.CurrentCheck currentCheck = checker.getCurrentCheck();
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, (task) -> {
            if (!p.isOnline()) {
                task.cancel();
                return;
            }

            Lang.Message message = null;

            placeholders.put("progress", currentCheck.getProgress());
            placeholders.put("goal", currentCheck.getGoal());
            placeholders.put("requests-sent", currentCheck.getRequestsSent());


            Checker.CheckState state = currentCheck.getState();
            switch (state) {
                case HASHING -> message = Lang.Message.CHECK_PROGRESS_ACTION_BAR_HASHING;
                case CHECKING_HASHES -> message = Lang.Message.CHECK_PROGRESS_ACTION_BAR_CHECKING_HASHES;
                case GETTING_PROJECTS -> message = Lang.Message.CHECK_PROGRESS_ACTION_BAR_GETTING_PROJECTS;
                case GETTING_VERSIONS -> message = Lang.Message.CHECK_PROGRESS_ACTION_BAR_GETTING_VERSIONS;
            }

            if (message != null) p.sendActionBar(message.getComponent(sender, placeholders));

            if (!checker.isCheckRunning()) task.cancel();
        }, 2, PluginConfig.actionBarInterval);
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        return List.of();
    }
}
