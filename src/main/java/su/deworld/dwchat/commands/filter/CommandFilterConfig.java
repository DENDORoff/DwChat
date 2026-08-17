package su.deworld.dwchat.commands.filter;

import org.bukkit.configuration.ConfigurationSection;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Конфигурация и логика сопоставления для одного блока фильтра команд.
 *
 * <p>Режимы:
 * <ul>
 *   <li>{@code STRING}  — точное совпадение имени команды (первый токен после {@code /})</li>
 *   <li>{@code PATTERN} — regex-сопоставление полной строки команды</li>
 * </ul>
 */
public class CommandFilterConfig {

    public enum Mode { STRING, PATTERN }

    private final boolean enabled;
    private final Mode mode;
    private final boolean blockAliases;
    private final Set<String> blockedNames;
    private final List<Pattern> patterns;
    private final List<ModerationAction> actions;

    public CommandFilterConfig(ConfigurationSection section) {
        if (section == null) {
            enabled = false; mode = Mode.STRING; blockAliases = false;
            blockedNames = Set.of(); patterns = List.of(); actions = List.of();
            return;
        }
        enabled      = true;
        mode         = Mode.valueOf(section.getString("mode", "STRING").toUpperCase(Locale.ROOT));
        blockAliases = section.getBoolean("block_aliases", true);

        List<String> raw = section.getStringList("commands");
        if (mode == Mode.STRING) {
            blockedNames = raw.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
            patterns = List.of();
        } else {
            blockedNames = Set.of();
            patterns = raw.stream()
                    .map(s -> Pattern.compile(s, Pattern.CASE_INSENSITIVE))
                    .toList();
        }

        actions = section.getStringList("actions").stream()
                .map(ModerationAction::parse)
                .toList();
    }

    public boolean isEnabled() { return enabled; }
    public List<ModerationAction> getActions() { return actions; }

    /**
     * Возвращает true если данный фильтр совпадает с командой.
     *
     * @param rawCommand строка команды без ведущего {@code /}
     */
    public boolean matches(String rawCommand) {
        if (!enabled) return false;
        String lower = rawCommand.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("/")) lower = lower.substring(1);

        if (mode == Mode.STRING) {
            String name = lower.split("\\s+")[0];
            // Стрипаем namespace:command если block_aliases включён
            if (blockAliases && name.contains(":")) {
                name = name.substring(name.indexOf(':') + 1);
            }
            return blockedNames.contains(name);
        } else {
            for (Pattern p : patterns) {
                if (p.matcher(lower).matches()) return true;
            }
            return false;
        }
    }
}