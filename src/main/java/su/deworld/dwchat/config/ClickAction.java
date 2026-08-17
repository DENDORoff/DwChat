package su.deworld.dwchat.config;

import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a configurable click action attached to a chat component.
 */
public record ClickAction(Type type, String value) {

    public enum Type {
        RUN_COMMAND, SUGGEST_COMMAND, OPEN_URL, NONE;

        public static Type parse(String raw) {
            if (raw == null || raw.isBlank()) return NONE;
            return switch (raw.toLowerCase().replace("-", "_")) {
                case "run_command"     -> RUN_COMMAND;
                case "suggest_command" -> SUGGEST_COMMAND;
                case "open_url"        -> OPEN_URL;
                default                -> NONE;
            };
        }
    }

    public static ClickAction none() {
        return new ClickAction(Type.NONE, "");
    }

    /** Resolves placeholders and builds an Adventure ClickEvent, or null if disabled. */
    public @Nullable ClickEvent resolve(String player) {
        if (type == Type.NONE || value == null || value.isBlank()) return null;
        String resolved = value.replace("{player}", player);
        return switch (type) {
            case RUN_COMMAND     -> ClickEvent.runCommand(resolved);
            case SUGGEST_COMMAND -> ClickEvent.suggestCommand(resolved);
            case OPEN_URL        -> ClickEvent.openUrl(resolved);
            default              -> null;
        };
    }
}