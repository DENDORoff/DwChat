package su.deworld.dwchat.moderation;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.moderation.check.*;

import java.util.UUID;

/**
 * Aggregates all moderation checks and exposes a single
 * {@link #checkChat(Player, String)} entry point.
 *
 * <p>Checks are applied in this order:
 * <ol>
 *   <li>Allowed chars</li>
 *   <li>Numbers / IP check</li>
 *   <li>Caps check</li>
 *   <li>Same messages (anti-flood)</li>
 *   <li>Banned words</li>
 * </ol>
 * The first blocking check short-circuits the pipeline.
 * Non-blocking checks may mutate the message text (lowercase, censor).
 * </p>
 */
public class ModerationManager {

    private final DwChat plugin;

    // Chat checks
    private final AllowedCharsCheck chatCharsCheck;
    private final NumbersCheck numbersCheck;
    private final CaseCheck caseCheck;
    private final SameMessagesCheck sameMessagesCheck;
    private final BanWordsCheck banWordsCheck;

    // Book / sign checks (share same structure, separate config sections)
    private final AllowedCharsCheck bookCharsCheck;
    private final AllowedCharsCheck signCharsCheck;

    public ModerationManager(DwChat plugin) {
        this.plugin = plugin;
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("chat_settings");

        chatCharsCheck    = new AllowedCharsCheck(cs != null ? cs.getConfigurationSection("allowed_chat_chars") : null);
        numbersCheck      = new NumbersCheck(cs != null ? cs.getConfigurationSection("numbers_check") : null);
        caseCheck         = new CaseCheck(cs != null ? cs.getConfigurationSection("case_check") : null);
        sameMessagesCheck = new SameMessagesCheck(cs != null ? cs.getConfigurationSection("same_messages") : null);
        banWordsCheck     = new BanWordsCheck(cs != null ? cs.getConfigurationSection("ban_words_chat") : null);

        bookCharsCheck = new AllowedCharsCheck(cs != null ? cs.getConfigurationSection("allowed_book_chars") : null);
        signCharsCheck = new AllowedCharsCheck(cs != null ? cs.getConfigurationSection("allowed_sign_chars") : null);
    }

    /**
     * Runs all chat checks in order.
     *
     * @return {@code null} if the message should be blocked;
     *         otherwise the (possibly modified) message string.
     */
    public String checkChat(Player player, String message) {
        String current = message;

        // 1. Allowed chars
        CheckResult r = chatCharsCheck.check(player, current, plugin);
        if (r.blocked()) return null;
        if (r.modifiedText() != null) current = r.modifiedText();

        // 2. Numbers / IP
        r = numbersCheck.check(player, current, plugin);
        if (r.blocked()) return null;
        if (r.modifiedText() != null) current = r.modifiedText();

        // 3. Caps
        r = caseCheck.check(player, current, plugin);
        if (r.blocked()) return null;
        if (r.modifiedText() != null) current = r.modifiedText();

        // 4. Same messages
        r = sameMessagesCheck.check(player, current, plugin);
        if (r.blocked()) return null;
        if (r.modifiedText() != null) current = r.modifiedText();

        // 5. Banned words
        r = banWordsCheck.check(player, current, plugin);
        if (r.blocked()) return null;
        if (r.modifiedText() != null) current = r.modifiedText();

        return current;
    }

    /**
     * Runs book checks. Returns null to block, or (modified) text.
     */
    public String checkBook(Player player, String text) {
        CheckResult r = bookCharsCheck.check(player, text, plugin);
        return r.blocked() ? null : (r.modifiedText() != null ? r.modifiedText() : text);
    }

    /**
     * Runs sign checks. Returns null to block, or (modified) text.
     */
    public String checkSign(Player player, String text) {
        CheckResult r = signCharsCheck.check(player, text, plugin);
        return r.blocked() ? null : (r.modifiedText() != null ? r.modifiedText() : text);
    }

    /** Called on player disconnect to free flood-history memory. */
    public void clearPlayer(UUID uuid) {
        sameMessagesCheck.clearPlayer(uuid);
    }
}