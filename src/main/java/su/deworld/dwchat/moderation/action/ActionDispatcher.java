package su.deworld.dwchat.moderation.action;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Выполняет информационные действия (MESSAGE, NOTIFY) из списка.
 *
 * <p>Поддерживаемые плейсхолдеры: {@code %player%}, {@code %msg%},
 * {@code %symbol%}, {@code %word%}, {@code %cmd%}.</p>
 *
 * <p>Блокирующие действия (BLOCK, HIDE, *_TAB_COMPLETE) обрабатываются
 * в слушателях напрямую и в этот метод не передаются.</p>
 */
public final class ActionDispatcher {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private ActionDispatcher() {}

    /**
     * Выполняет все переданные информационные действия.
     *
     * @param actions список действий (только MESSAGE / NOTIFY)
     * @param player  нарушитель
     * @param plugin  ссылка на плагин (для итерации по игрокам сервера)
     * @param msg     полный текст сообщения / команды (плейсхолдер %msg%)
     * @param extra   доп. значение: символ, слово или команда (%symbol% / %word% / %cmd%)
     * @return true если в списке был хотя бы один BLOCK/LITE_BLOCK (используется чатом)
     */
    public static boolean dispatch(List<ModerationAction> actions, Player player,
                                   JavaPlugin plugin, String msg, String extra) {
        boolean blocked = false;
        for (ModerationAction action : actions) {
            switch (action.type()) {
                case MESSAGE -> {
                    String text = resolve(action.text(), player, msg, extra);
                    player.sendMessage(LEGACY.deserialize(text));
                }
                case NOTIFY -> {
                    String text = resolve(action.text(), player, msg, extra);
                    var component = LEGACY.deserialize(text);
                    String perm = action.notifyPermission();
                    for (Player online : plugin.getServer().getOnlinePlayers()) {
                        if (perm == null || online.hasPermission(perm)) {
                            online.sendMessage(component);
                        }
                    }
                    plugin.getServer().getConsoleSender().sendMessage(component);
                }
                // Блокирующие типы — используются чатовым пайплайном
                case BLOCK, LITE_BLOCK -> blocked = true;
                // Остальные (HIDE, BLOCK_TAB_COMPLETE и их LITE-варианты)
                // обрабатываются в CommandFilterManager/Listener, не здесь
                default -> {}
            }
        }
        return blocked;
    }

    private static String resolve(String template, Player player, String msg, String extra) {
        return template
                .replace("%player%", player.getName())
                .replace("%msg%",    msg)
                .replace("%symbol%", extra != null ? extra : "")
                .replace("%word%",   extra != null ? extra : "")
                .replace("%cmd%",    extra != null ? extra : "");
    }
}