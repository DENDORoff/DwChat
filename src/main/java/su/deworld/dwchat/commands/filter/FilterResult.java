package su.deworld.dwchat.commands.filter;

import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.List;

/**
 * Результат проверки команды через {@link CommandFilterManager}.
 *
 * <p>Каждое из полей уже учитывает право {@code dwchat.admin} игрока:
 * если у игрока есть это право, LITE_* действия были исключены заранее в менеджере.
 *
 * @param block           заблокировать выполнение команды
 * @param hide            скрыть ответ сервера (feedback) в игре
 * @param blockTabComplete скрыть из таб-комплита
 * @param actions         действия MESSAGE / NOTIFY для диспетчера
 */
public record FilterResult(boolean block, boolean hide, boolean blockTabComplete,
                           List<ModerationAction> actions) {

    public boolean isClean() {
        return !block && !hide && !blockTabComplete && actions.isEmpty();
    }
}