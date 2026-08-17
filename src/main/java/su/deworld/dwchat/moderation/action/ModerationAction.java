package su.deworld.dwchat.moderation.action;

/**
 * Одно действие из конфигурации, применяемое при срабатывании проверки.
 *
 * <p>Форматы строк действий:
 * <ul>
 *   <li>{@code [MESSAGE] текст}          — отправить сообщение нарушителю</li>
 *   <li>{@code [NOTIFY] текст perm=X}    — уведомить всех с правом X</li>
 *   <li>{@code [HIDE]}                   — скрыть ответ сервера на команду для ВСЕХ</li>
 *   <li>{@code [LITE_HIDE]}              — скрыть ответ сервера на команду для всех БЕЗ dwchat.admin</li>
 *   <li>{@code [BLOCK]}                  — заблокировать выполнение команды для ВСЕХ</li>
 *   <li>{@code [LITE_BLOCK]}             — заблокировать выполнение команды для всех БЕЗ dwchat.admin</li>
 *   <li>{@code [BLOCK_TAB_COMPLETE]}     — скрыть из таб-комплита для ВСЕХ</li>
 *   <li>{@code [LITE_BLOCK_TAB_COMPLETE]}— скрыть из таб-комплита для всех БЕЗ dwchat.admin</li>
 * </ul>
 * Правило «LITE»: действие применяется только если у игрока нет права {@code dwchat.admin}.
 */
public record ModerationAction(Type type, String text, String notifyPermission) {

    public enum Type {
        MESSAGE,
        NOTIFY,
        // Команды — абсолютные (для всех)
        HIDE,
        BLOCK,
        BLOCK_TAB_COMPLETE,
        // Команды — мягкие (для всех без dwchat.admin)
        LITE_HIDE,
        LITE_BLOCK,
        LITE_BLOCK_TAB_COMPLETE
    }

    public static ModerationAction parse(String raw) {
        raw = raw.trim();

        if (raw.startsWith("[MESSAGE]")) {
            return new ModerationAction(Type.MESSAGE, raw.substring("[MESSAGE]".length()).trim(), null);
        }
        if (raw.startsWith("[NOTIFY]")) {
            String rest = raw.substring("[NOTIFY]".length()).trim();
            String perm = null;
            String text = rest;
            int idx = rest.lastIndexOf(" perm=");
            if (idx >= 0) {
                perm = rest.substring(idx + " perm=".length()).trim()
                        .replace("{", "").replace("}", "");
                text = rest.substring(0, idx).trim();
            }
            return new ModerationAction(Type.NOTIFY, text, perm);
        }

        return switch (raw) {
            case "[HIDE]"                   -> new ModerationAction(Type.HIDE,                   "", null);
            case "[LITE_HIDE]"              -> new ModerationAction(Type.LITE_HIDE,              "", null);
            case "[BLOCK]"                  -> new ModerationAction(Type.BLOCK,                  "", null);
            case "[LITE_BLOCK]"             -> new ModerationAction(Type.LITE_BLOCK,             "", null);
            case "[BLOCK_TAB_COMPLETE]"     -> new ModerationAction(Type.BLOCK_TAB_COMPLETE,     "", null);
            case "[LITE_BLOCK_TAB_COMPLETE]"-> new ModerationAction(Type.LITE_BLOCK_TAB_COMPLETE,"", null);
            default -> new ModerationAction(Type.MESSAGE, raw, null);
        };
    }
}