package su.deworld.dwchat.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.chat.ChatChannel;
import su.deworld.dwchat.chat.ComponentFactory;
import su.deworld.dwchat.config.PluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    // Matches &<code> and §<code> color/format codes
    private static final Pattern COLOR_PATTERN =
            Pattern.compile("[&§]([0-9a-fk-orA-FK-OR])");
    // Matches &x&R&R&G&G&B&B hex color
    private static final Pattern HEX_PATTERN =
            Pattern.compile("[&§]x([&§][0-9a-fA-F]){6}");

    // Permission nodes
    private static final String PERM_COLOR_PREFIX  = "dwchat.color.";
    private static final String PERM_FONT_BOLD      = "dwchat.font.bold";
    private static final String PERM_FONT_ITALIC    = "dwchat.font.italic";
    private static final String PERM_FONT_UNDERLINE = "dwchat.font.underline";
    private static final String PERM_FONT_STRIKE    = "dwchat.font.strikethrough";
    private static final String PERM_FONT_OBFUSCATE = "dwchat.font.obfuscated";
    private static final String PERM_COLOR_HEX      = "dwchat.color.hex";
    private static final String PERM_COLOR_ALL      = "dwchat.color.*";

    private final DwChat plugin;

    public ChatListener(DwChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player sender = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        PluginConfig cfg  = plugin.getPluginConfig();

        // ── Determine channel ─────────────────────────────────
        ChatChannel channel;
        String displayMessage;

        if (rawMessage.startsWith(cfg.globalPrefix)) {
            displayMessage = rawMessage.substring(cfg.globalPrefix.length()).trim();
            if (displayMessage.isBlank()) return;
            channel = ChatChannel.GLOBAL;
        } else {
            displayMessage = rawMessage;
            channel = ChatChannel.LOCAL;
        }

        // ── Moderation ────────────────────────────────────────
        String moderated = plugin.getModerationManager().checkChat(sender, displayMessage);
        if (moderated == null) return; // blocked by moderation

        // ── Color/format permission filtering ─────────────────
        displayMessage = filterColorCodes(sender, moderated);

        // ── Build component ───────────────────────────────────
        Component component = ComponentFactory.chatMessage(sender, displayMessage, channel, cfg);

        // ── Determine recipients ──────────────────────────────
        List<Player> recipients = new ArrayList<>();
        if (channel == ChatChannel.GLOBAL) {
            // Include sender only if there are other players too;
            // but sender always sees own message regardless
            recipients.addAll(plugin.getServer().getOnlinePlayers());
        } else {
            int radius = cfg.localRadius;
            Location senderLoc = sender.getLocation();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (!p.getWorld().equals(sender.getWorld())) continue;
                if (radius < 0 || p.getLocation().distanceSquared(senderLoc) <= (double) radius * radius) {
                    recipients.add(p);
                }
            }
        }

        // ── "Nobody heard you" notification ──────────────────
        // For LOCAL: no other player nearby (only sender in list, or empty)
        // For GLOBAL: no players online other than sender
        boolean onlyMe = recipients.isEmpty()
                || (recipients.size() == 1 && recipients.contains(sender));
        if (onlyMe && !cfg.nooneHeardMessage.isBlank()) {
            sender.sendMessage(LEGACY.deserialize(cfg.nooneHeardMessage));
        }

        // ── Deliver ───────────────────────────────────────────
        final Component finalComponent  = component;
        final ChatChannel finalChannel  = channel;
        final String finalMessage       = displayMessage;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            var soundCfg = finalChannel == ChatChannel.GLOBAL ? cfg.globalSound : cfg.localSound;
            for (Player p : recipients) {
                p.sendMessage(finalComponent);
                soundCfg.playTo(p);
            }
        });

        // ── Bridge ────────────────────────────────────────────
        plugin.getBridgeManager().onMinecraftChat(sender.getName(), finalMessage, finalChannel);
    }

    // ── Color/format permission check ────────────────────────

    /**
     * Strips color / format codes the player does not have permission to use.
     *
     * <p>Permission map:
     * <ul>
     *   <li>{@code dwchat.color.*}      — all colors and hex</li>
     *   <li>{@code dwchat.color.hex}    — hex colors (&x&R&R&G&G&B&B)</li>
     *   <li>{@code dwchat.color.<0-9a-f>} — individual color codes</li>
     *   <li>{@code dwchat.font.bold}        — &l</li>
     *   <li>{@code dwchat.font.italic}      — &o</li>
     *   <li>{@code dwchat.font.underline}   — &n</li>
     *   <li>{@code dwchat.font.strikethrough}— &m</li>
     *   <li>{@code dwchat.font.obfuscated}  — &k</li>
     * </ul>
     * </p>
     */
    private String filterColorCodes(Player player, String message) {
        boolean allColors = player.hasPermission(PERM_COLOR_ALL);

        // Strip hex colors if no permission
        if (!allColors && !player.hasPermission(PERM_COLOR_HEX)) {
            message = HEX_PATTERN.matcher(message).replaceAll("");
        }

        // Process §/& codes
        Matcher m = COLOR_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            char code = m.group(1).toLowerCase().charAt(0);
            if (allColors || hasCodePermission(player, code)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(sb, ""); // strip the code
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private boolean hasCodePermission(Player player, char code) {
        return switch (code) {
            case '0','1','2','3','4','5','6','7',
                 '8','9','a','b','c','d','e','f' ->
                    player.hasPermission(PERM_COLOR_PREFIX + code);
            case 'l' -> player.hasPermission(PERM_FONT_BOLD);
            case 'o' -> player.hasPermission(PERM_FONT_ITALIC);
            case 'n' -> player.hasPermission(PERM_FONT_UNDERLINE);
            case 'm' -> player.hasPermission(PERM_FONT_STRIKE);
            case 'k' -> player.hasPermission(PERM_FONT_OBFUSCATE);
            case 'r' -> true; // reset always allowed
            default  -> false;
        };
    }
}