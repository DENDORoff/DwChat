package su.deworld.dwchat.moderation.check;

import su.deworld.dwchat.moderation.CheckResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.moderation.action.ActionDispatcher;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Limits the number of digit characters in a single message.
 *
 * <p>When {@code strict: false}, also catches IP-address-like patterns
 * (e.g. sequences of digits separated by dots/colons).</p>
 */
public class NumbersCheck {

    private static final Pattern IP_LIKE = Pattern.compile(
            "\\d{1,3}[.:]\\d{1,3}[.:]\\d{1,3}");

    private final boolean enabled;
    private final boolean stripColor;
    private final int maxNumbers;
    private final boolean strict;
    private final List<ModerationAction> actions;

    public NumbersCheck(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enable", false)) {
            this.enabled = false;
            this.stripColor = false;
            this.maxNumbers = Integer.MAX_VALUE;
            this.strict = false;
            this.actions = List.of();
            return;
        }
        this.enabled    = true;
        this.stripColor = section.getBoolean("strip_color", true);
        this.maxNumbers = section.getInt("maxmsgnumbers", 7);
        this.strict     = section.getBoolean("strict", false);
        this.actions    = section.getStringList("actions").stream()
                .map(ModerationAction::parse).toList();
    }

    public CheckResult check(Player player, String message, JavaPlugin plugin) {
        if (!enabled) return CheckResult.PASS;

        String text = stripColor ? stripColorCodes(message) : message;

        // Strict mode: just count digits
        long digitCount = text.chars().filter(Character::isDigit).count();
        if (digitCount > maxNumbers) {
            boolean blocked = ActionDispatcher.dispatch(actions, player, plugin, message, null);
            return blocked ? CheckResult.block() : CheckResult.PASS;
        }

        // Non-strict: also detect IP-like sequences
        if (!strict && IP_LIKE.matcher(text).find()) {
            boolean blocked = ActionDispatcher.dispatch(actions, player, plugin, message, null);
            return blocked ? CheckResult.block() : CheckResult.PASS;
        }

        return CheckResult.PASS;
    }

    private static String stripColorCodes(String text) {
        // Strip §x and &x color codes
        return text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "")
                   .replaceAll("[&§]x([0-9a-fA-F]{6})", "");
    }
}