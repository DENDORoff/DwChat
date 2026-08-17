package su.deworld.dwchat.moderation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.moderation.action.ActionDispatcher;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Blocks messages that contain characters not in the allowed set.
 */
public class AllowedCharsCheck {

    private final boolean enabled;
    private final Set<Character> allowed;
    private final List<ModerationAction> actions;

    public AllowedCharsCheck(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enable", false)) {
            this.enabled = false;
            this.allowed = Set.of();
            this.actions = List.of();
            return;
        }
        this.enabled = true;
        String pattern = section.getString("pattern", "");
        this.allowed = pattern.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toSet());
        this.actions = section.getStringList("actions").stream()
                .map(ModerationAction::parse)
                .toList();
    }

    /**
     * @return CheckResult.PASS if ok, or block result with the first offending symbol dispatched.
     */
    public CheckResult check(Player player, String message, JavaPlugin plugin) {
        if (!enabled) return CheckResult.PASS;
        for (char c : message.toCharArray()) {
            if (!allowed.contains(c)) {
                boolean blocked = ActionDispatcher.dispatch(actions, player, plugin,
                        message, String.valueOf(c));
                return blocked ? CheckResult.block() : CheckResult.PASS;
            }
        }
        return CheckResult.PASS;
    }
}