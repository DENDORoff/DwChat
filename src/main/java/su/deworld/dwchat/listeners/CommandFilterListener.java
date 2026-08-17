package su.deworld.dwchat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.commands.filter.FilterResult;
import su.deworld.dwchat.moderation.action.ActionDispatcher;

import java.util.Iterator;

/**
 * Слушатель фильтрации команд.
 *
 * <p>Механизмы:
 * <ul>
 *   <li><b>BLOCK / LITE_BLOCK</b> — отменяет выполнение команды.</li>
 *   <li><b>HIDE / LITE_HIDE</b> — отменяет команду И скрывает её от игрока
 *       (в отличие от BLOCK не отправляет игроку сообщение из [MESSAGE]-действий).
 *       На Paper 1.21.1 публичного API для подавления только feedback нет,
 *       поэтому HIDE реализован как отмена события без уведомления игрока.</li>
 *   <li><b>BLOCK_TAB_COMPLETE / LITE_BLOCK_TAB_COMPLETE</b> — убирает из таб-комплита.</li>
 * </ul>
 */
public class CommandFilterListener implements Listener {

    private final DwChat plugin;

    public CommandFilterListener(DwChat plugin) {
        this.plugin = plugin;
    }

    // ── Выполнение команды ────────────────────────────────────

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().substring(1); // убираем "/"

        FilterResult result = plugin.getCommandFilterManager().check(command, player);
        if (result.isClean()) return;

        if (result.hide()) {
            // HIDE: тихо отменить — без уведомлений игроку, без лога
            // MESSAGE/NOTIFY действия намеренно пропускаются
            event.setCancelled(true);
            return;
        }

        if (result.block()) {
            // BLOCK: отменить команду + отправить игроку [MESSAGE] уведомления
            event.setCancelled(true);
            ActionDispatcher.dispatch(
                    result.actions(), player, plugin, command, command.split("\\s+")[0]);
            return;
        }

        // Только уведомления без блокировки
        ActionDispatcher.dispatch(
                result.actions(), player, plugin, command, command.split("\\s+")[0]);
    }

    // ── Таб-комплит ───────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player)) return;
        if (event.getCompletions().isEmpty()) return;

        Iterator<String> it = event.getCompletions().iterator();
        while (it.hasNext()) {
            String suggestion = it.next();
            String name = suggestion.startsWith("/") ? suggestion.substring(1) : suggestion;
            if (name.contains(" ")) name = name.split("\\s+")[0];

            if (plugin.getCommandFilterManager().isTabCompleteBlocked(name, player)) {
                it.remove();
            }
        }
    }
}