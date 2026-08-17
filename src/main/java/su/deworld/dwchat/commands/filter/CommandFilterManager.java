package su.deworld.dwchat.commands.filter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Загружает все блоки {@link CommandFilterConfig} из секции {@code commands:}
 * и предоставляет единую точку проверки для слушателей.
 */
public class CommandFilterManager {

    private static final String ADMIN_PERM = "dwchat.admin";

    private final DwChat plugin;
    private final List<CommandFilterConfig> filters = new ArrayList<>();

    public CommandFilterManager(DwChat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        filters.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("commands");
        if (root == null) return;
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec != null) filters.add(new CommandFilterConfig(sec));
        }
    }

    /**
     * Проверяет команду против всех фильтров с учётом прав игрока.
     *
     * <p>Логика LITE vs абсолютные:
     * <ul>
     *   <li>{@code LITE_*} — действует только если у игрока нет {@code dwchat.admin}</li>
     *   <li>абсолютные ({@code BLOCK}, {@code HIDE}, {@code BLOCK_TAB_COMPLETE}) — действует всегда</li>
     * </ul>
     *
     * @param command строка команды без ведущего {@code /}
     * @param player  игрок, выполняющий команду
     */
    public FilterResult check(String command, Player player) {
        boolean isAdmin = player.hasPermission(ADMIN_PERM);

        boolean block            = false;
        boolean hide             = false;
        boolean blockTabComplete = false;
        List<ModerationAction> dispatchable = new ArrayList<>();

        for (CommandFilterConfig filter : filters) {
            if (!filter.matches(command)) continue;

            for (ModerationAction action : filter.getActions()) {
                switch (action.type()) {
                    // Абсолютные — действуют всегда
                    case BLOCK              -> block            = true;
                    case HIDE               -> hide             = true;
                    case BLOCK_TAB_COMPLETE -> blockTabComplete = true;

                    // LITE — пропускаем если есть dwchat.admin
                    case LITE_BLOCK              -> { if (!isAdmin) block            = true; }
                    case LITE_HIDE               -> { if (!isAdmin) hide             = true; }
                    case LITE_BLOCK_TAB_COMPLETE -> { if (!isAdmin) blockTabComplete = true; }

                    // Информационные — диспетчеризируем всегда
                    // (администраторы тоже должны видеть [NOTIFY] уведомления)
                    case MESSAGE, NOTIFY -> dispatchable.add(action);
                }
            }
        }

        return new FilterResult(block, hide, blockTabComplete, dispatchable);
    }

    /**
     * Проверяет нужно ли скрыть команду из таб-комплита для данного игрока.
     */
    public boolean isTabCompleteBlocked(String commandName, Player player) {
        boolean isAdmin = player.hasPermission(ADMIN_PERM);
        for (CommandFilterConfig filter : filters) {
            if (!filter.matches(commandName)) continue;
            for (ModerationAction action : filter.getActions()) {
                if (action.type() == ModerationAction.Type.BLOCK_TAB_COMPLETE) return true;
                if (action.type() == ModerationAction.Type.LITE_BLOCK_TAB_COMPLETE && !isAdmin) return true;
            }
        }
        return false;
    }

    public List<CommandFilterConfig> getFilters() { return filters; }
}