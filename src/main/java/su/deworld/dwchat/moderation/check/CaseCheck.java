package su.deworld.dwchat.moderation.check;

import su.deworld.dwchat.moderation.CheckResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.moderation.action.ActionDispatcher;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.List;

/**
 * Anti-caps check.
 *
 * <p>When {@code strict: false}, converts the message to lowercase instead of blocking.</p>
 */
public class CaseCheck {

    private final boolean enabled;
    private final int maxUppercasePercent;
    private final boolean strict;
    private final List<ModerationAction> actions;

    public CaseCheck(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enable", false)) {
            this.enabled = false;
            this.maxUppercasePercent = 100;
            this.strict = false;
            this.actions = List.of();
            return;
        }
        this.enabled             = true;
        this.maxUppercasePercent = section.getInt("max_uppercase_percent", 60);
        this.strict              = section.getBoolean("strict", false);
        this.actions             = section.getStringList("actions").stream()
                .map(ModerationAction::parse).toList();
    }

    public CheckResult check(Player player, String message, JavaPlugin plugin) {
        if (!enabled) return CheckResult.PASS;

        long letters = message.chars().filter(Character::isLetter).count();
        if (letters == 0) return CheckResult.PASS;

        long upper = message.chars().filter(Character::isUpperCase).count();
        int pct = (int) (upper * 100 / letters);

        if (pct > maxUppercasePercent) {
            if (strict) {
                boolean blocked = ActionDispatcher.dispatch(actions, player, plugin, message, null);
                return blocked ? CheckResult.block() : CheckResult.PASS;
            } else {
                // Soft: dispatch informational actions, then lowercase the message
                ActionDispatcher.dispatch(actions, player, plugin, message, null);
                return CheckResult.modify(message.toLowerCase());
            }
        }
        return CheckResult.PASS;
    }
}