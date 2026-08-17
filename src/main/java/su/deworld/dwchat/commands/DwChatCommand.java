package su.deworld.dwchat.commands;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.deworld.dwchat.DwChat;

import java.util.List;

public class DwChatCommand implements CommandExecutor, TabCompleter {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final DwChat plugin;

    public DwChatCommand(DwChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("dwchat.admin")) {
            sender.sendMessage(LEGACY.deserialize(plugin.getPluginConfig().msgNoPermission));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reload();
            sender.sendMessage(LEGACY.deserialize(plugin.getPluginConfig().msgReloadSuccess));
            return true;
        }
        sender.sendMessage(LEGACY.deserialize(plugin.getPluginConfig().msgUsage));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}