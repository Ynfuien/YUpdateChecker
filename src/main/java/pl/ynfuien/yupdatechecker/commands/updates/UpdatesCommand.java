package pl.ynfuien.yupdatechecker.commands.updates;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.ynfuien.yupdatechecker.Lang;
import pl.ynfuien.yupdatechecker.YUpdateChecker;
import pl.ynfuien.yupdatechecker.commands.Subcommand;
import pl.ynfuien.yupdatechecker.commands.main.MainCommand;
import pl.ynfuien.yupdatechecker.core.Checker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class UpdatesCommand implements CommandExecutor, TabCompleter {
    private final YUpdateChecker instance;
    private final Checker checker;
    private final Subcommand[] subcommands;

    public UpdatesCommand(YUpdateChecker instance) {
        this.instance = instance;
        this.checker = instance.getChecker();

        this.subcommands = new Subcommand[] {
            new CheckSubcommand(instance),
            new ShowSubcommand(instance)
        };
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        HashMap<String, Object> placeholders = new HashMap<>() {{put("command", label);}};

        // Help
        if (args.length == 0) {
            sendHelp(sender, placeholders);
            return true;
        }

        // Selected subcommand
        String arg1 = args[0].toLowerCase();
        for (Subcommand subcommand : subcommands) {
            if (!subcommand.name().equals(arg1)) continue;
            if (!sender.hasPermission(subcommand.permission())) break;

            String[] argsLeft = Arrays.copyOfRange(args, 1, args.length);
            subcommand.run(sender, argsLeft, placeholders);
            return true;
        }

        sendHelp(sender, placeholders);
        return true;
    }

    private void sendHelp(CommandSender sender, HashMap<String, Object> placeholders) {
        List<Subcommand> permittedCmds = Arrays.stream(subcommands).filter((subcommand) -> sender.hasPermission(subcommand.permission())).toList();
        if (permittedCmds.isEmpty()) {
            Lang.Message.COMMAND_UPDATES_HELP_NO_COMMANDS.send(sender, placeholders);
            return;
        }

        Lang.Message.COMMAND_UPDATES_HELP_HEADER.send(sender, placeholders);
        for (Subcommand subcommand : permittedCmds) {
            String usage = subcommand.usage();
            placeholders.put("subcommand", subcommand.name() + (usage == null ? "" : " " + subcommand.usage()));
            placeholders.put("description", subcommand.description());

            Lang.Message.COMMAND_UPDATES_HELP_ENTRY.send(sender, placeholders);
        }
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return MainCommand.tabCompleteSubcommands(sender, subcommands, args);
    }
}
