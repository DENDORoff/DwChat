package su.deworld.dwchat.moderation.check;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.moderation.action.ActionDispatcher;
import su.deworld.dwchat.moderation.action.ModerationAction;
import su.deworld.dwchat.moderation.CheckResult;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Проверяет наличие запрещённых слов/подстрок в сообщении чата.
 *
 * <ul>
 *   <li>{@code strict: true}  — заблокировать сообщение целиком</li>
 *   <li>{@code strict: false} — заменить запрещённое слово на {@code censor_symbol}</li>
 * </ul>
 */
public class BanWordsCheck {

    private final boolean enabled;
    private final boolean strict;
    private final boolean stripColor;
    private final String censorSymbol;
    private final List<String> words;
    private final List<ModerationAction> actions;

    public BanWordsCheck(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enable", false)) {
            this.enabled      = false;
            this.strict       = false;
            this.stripColor   = true;
            this.censorSymbol = "***";
            this.words        = List.of();
            this.actions      = List.of();
            return;
        }
        this.enabled      = true;
        this.strict       = section.getBoolean("strict", false);
        this.stripColor   = section.getBoolean("strip_color", true);
        this.censorSymbol = section.getString("censor_symbol", "***");
        this.words        = section.getStringList("words").stream()
                .map(String::toLowerCase).toList();
        this.actions      = section.getStringList("actions").stream()
                .map(ModerationAction::parse).toList();
    }

    public CheckResult check(Player player, String message, JavaPlugin plugin) {
        if (!enabled || words.isEmpty()) return CheckResult.PASS;

        String compare = stripColor
                ? stripColorCodes(message.toLowerCase())
                : message.toLowerCase();

        for (String word : words) {
            if (compare.contains(word)) {
                ActionDispatcher.dispatch(actions, player, plugin, message, word);
                if (strict) {
                    return CheckResult.block();
                } else {
                    String censored = message.replaceAll(
                            "(?i)" + Pattern.quote(word), censorSymbol);
                    return CheckResult.modify(censored);
                }
            }
        }
        return CheckResult.PASS;
    }

    private static String stripColorCodes(String text) {
        return text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "")
                .replaceAll("[&§]x([0-9a-fA-F]{6})", "");
    }
}