package pl.ynfuien.yupdatechecker;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import pl.ynfuien.ydevlib.messages.LangBase;
import pl.ynfuien.ydevlib.messages.Messenger;
import pl.ynfuien.ydevlib.messages.colors.ColorFormatter;

import java.util.HashMap;

public class Lang extends LangBase {
    public enum Message implements LangBase.Message {
        PREFIX,
        COMMAND_MAIN_USAGE,
        COMMAND_MAIN_RELOAD_FAIL,
        COMMAND_MAIN_RELOAD_SUCCESS,
        COMMAND_MAIN_VERSION,

        // Help
        COMMAND_UPDATES_HELP_NO_COMMANDS,
        COMMAND_UPDATES_HELP_HEADER,
        COMMAND_UPDATES_HELP_ENTRY,
        // Check
        COMMAND_UPDATES_CHECK_DESCRIPTION,
        COMMAND_UPDATES_SHOW_DESCRIPTION,
        COMMAND_UPDATES_CHECK_FAIL,
        COMMAND_UPDATES_CHECK_FAIL_IS_RUNNING,
        COMMAND_UPDATES_CHECK_FAIL_RECENT,
        COMMAND_UPDATES_CHECK_START,
        COMMAND_UPDATES_CHECK_FINISH,
        // Show
        COMMAND_UPDATES_SHOW_FAIL_NO_CHECK,
        COMMAND_UPDATES_SHOW_FAIL_CHECK_RUNNING,
        COMMAND_UPDATES_SHOW_OLD_RUNNING_NEW,
        COMMAND_UPDATES_SHOW,
        COMMAND_UPDATES_SHOW_USAGE,
        COMMAND_UPDATES_SHOW_PLUGINS_HEADER,
        COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_OUTDATED,
        COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE,
        COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE_RELEASE,
        COMMAND_UPDATES_SHOW_PLUGINS_ENTRY_UP_TO_DATE_VERSION,
        COMMAND_UPDATES_SHOW_PLUGINS_URL,
        COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_NEXT,
        COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_BOTH,
        COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_PREVIOUS,
        COMMAND_UPDATES_SHOW_PLUGINS_FOOTER_PAGE_SINGLE,
        COMMAND_UPDATES_SHOW_DATAPACKS_HEADER,
        COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_OUTDATED,
        COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE,
        COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE_RELEASE,
        COMMAND_UPDATES_SHOW_DATAPACKS_ENTRY_UP_TO_DATE_VERSION,
        COMMAND_UPDATES_SHOW_DATAPACKS_URL,
        COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_NEXT,
        COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_BOTH,
        COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_PREVIOUS,
        COMMAND_UPDATES_SHOW_DATAPACKS_FOOTER_PAGE_SINGLE,
        CHECK_PROGRESS_ACTION_BAR
        ;

        /**
         * Gets name/path of this message.
         */
        @Override
        public String getName() {
            return name().toLowerCase().replace('_', '-');
        }

        /**
         * Gets original unformatted message.
         */
        public String get() {
            return Lang.get(getName());
        }

        /**
         * Gets message with parsed:
         * - {prefix} placeholder
         * - additional provided placeholders
         */
        public String get(HashMap<String, Object> placeholders) {
            return Lang.get(getName(), placeholders);
        }

        /**
         * Gets message with parsed:
         * - PlaceholderAPI
         * - {prefix} placeholder
         * - additional provided placeholders
         */
        public String get(CommandSender sender, HashMap<String, Object> placeholders) {
            return ColorFormatter.parsePAPI(sender, Lang.get(getName(), placeholders));
        }

        /**
         * Gets message as component with parsed:
         * - MiniMessage
         * - PlaceholderAPI
         * - {prefix} placeholder
         * - additional provided placeholders
         */
        public Component getComponent(CommandSender sender, HashMap<String, Object> placeholders) {
            return Messenger.parseMessage(sender, Lang.get(getName()), placeholders);
        }

        /**
         * Sends this message to provided sender.<br/>
         * Parses:<br/>
         * - MiniMessage<br/>
         * - PlaceholderAPI<br/>
         * - {prefix} placeholder
         */
        public void send(CommandSender sender) {
            this.send(sender, new HashMap<>());
        }

        /**
         * Sends this message to provided sender.<br/>
         * Parses:<br/>
         * - MiniMessage<br/>
         * - PlaceholderAPI<br/>
         * - {prefix} placeholder<br/>
         * - additional provided placeholders
         */
        public void send(CommandSender sender, HashMap<String, Object> placeholders) {
            Lang.sendMessage(sender, this, placeholders);
        }
    }
}
