package su.deworld.dwchat.moderation.check;

import su.deworld.dwchat.moderation.CheckResult;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.moderation.action.ActionDispatcher;
import su.deworld.dwchat.moderation.action.ModerationAction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-flood: tracks each player's recent message history and blocks
 * messages that are too similar to previously sent ones.
 *
 * <p>Similarity is measured as a simple character-overlap ratio derived from
 * the Levenshtein distance, which is fast enough for short chat messages.</p>
 */
public class SameMessagesCheck {

    private final boolean enabled;
    private final int samePercents;
    private final int maxSameMessage;
    private final int minMessageLength;
    private final int historySize;
    private final boolean stripColor;
    private final List<ModerationAction> actions;

    /** Per-player circular message history. */
    private final Map<UUID, Deque<String>> histories = new ConcurrentHashMap<>();

    public SameMessagesCheck(ConfigurationSection section) {
        if (section == null || !section.getBoolean("enable", false)) {
            this.enabled          = false;
            this.samePercents     = 70;
            this.maxSameMessage   = 2;
            this.minMessageLength = 3;
            this.historySize      = 10;
            this.stripColor       = true;
            this.actions          = List.of();
            return;
        }
        this.enabled          = true;
        this.samePercents     = section.getInt("same_percents", 70);
        this.maxSameMessage   = section.getInt("max_same_message", 2);
        this.minMessageLength = section.getInt("min_message_length", 3);
        this.historySize      = section.getInt("history_size", 10);
        this.stripColor       = section.getBoolean("strip_color", true);
        this.actions          = section.getStringList("actions").stream()
                .map(ModerationAction::parse).toList();
    }

    public CheckResult check(Player player, String message, JavaPlugin plugin) {
        if (!enabled) return CheckResult.PASS;

        String normalized = stripColor ? stripColorCodes(message) : message;
        if (normalized.length() < minMessageLength) return CheckResult.PASS;

        Deque<String> history = histories.computeIfAbsent(
                player.getUniqueId(), k -> new ArrayDeque<>(historySize));

        long similarCount = history.stream()
                .filter(prev -> similarity(normalized, prev) >= samePercents)
                .count();

        // Add to history (before checking so current message counts too)
        if (history.size() >= historySize) history.pollFirst();
        history.addLast(normalized);

        if (similarCount >= maxSameMessage) {
            boolean blocked = ActionDispatcher.dispatch(actions, player, plugin, message, null);
            return blocked ? CheckResult.block() : CheckResult.PASS;
        }
        return CheckResult.PASS;
    }

    /** Clears a player's history on disconnect. */
    public void clearPlayer(UUID uuid) {
        histories.remove(uuid);
    }

    // ── Levenshtein-based similarity ──────────────────────────

    /**
     * Returns similarity in percent [0–100].
     */
    private static int similarity(String a, String b) {
        if (a.equals(b)) return 100;
        int maxLen = Math.max(a.length(), b.length());
        if (maxLen == 0) return 100;
        int dist = levenshtein(a, b);
        return (int) ((1.0 - (double) dist / maxLen) * 100);
    }

    private static int levenshtein(String a, String b) {
        int[] dp = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) dp[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int prev = dp[0];
            dp[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int temp = dp[j];
                dp[j] = a.charAt(i - 1) == b.charAt(j - 1) ? prev
                        : 1 + Math.min(prev, Math.min(dp[j], dp[j - 1]));
                prev = temp;
            }
        }
        return dp[b.length()];
    }

    private static String stripColorCodes(String text) {
        return text.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "")
                   .replaceAll("[&§]x([0-9a-fA-F]{6})", "");
    }
}