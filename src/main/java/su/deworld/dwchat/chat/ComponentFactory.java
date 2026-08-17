package su.deworld.dwchat.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import su.deworld.dwchat.bridge.MediaAttachment;
import su.deworld.dwchat.config.ClickAction;
import su.deworld.dwchat.config.PluginConfig;

import java.util.List;

/**
 * Builds Adventure {@link Component} objects for all chat contexts:
 * <ul>
 *   <li>Local / Global chat messages with configurable hover + click on the player name</li>
 *   <li>Bridge messages (Discord / Telegram) with media attachment tags</li>
 *   <li>Auto-messages with hover + click</li>
 * </ul>
 */
public final class ComponentFactory {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private ComponentFactory() {}

    // ── Chat messages ─────────────────────────────────────────

    /**
     * Builds a local or global chat component.
     *
     * <p>The format string is split on the literal {@code {player}} token.
     * The player-name segment gets hover / click applied; the rest is plain legacy text.</p>
     */
    public static Component chatMessage(Player sender, String message, ChatChannel channel,
                                         PluginConfig cfg) {
        String format = channel == ChatChannel.GLOBAL ? cfg.globalFormat : cfg.localFormat;
        String hover  = channel == ChatChannel.GLOBAL ? cfg.globalHover  : cfg.localHover;
        ClickAction click = channel == ChatChannel.GLOBAL ? cfg.globalClick : cfg.localClick;

        String resolved = resolveChatPlaceholders(format, sender, message);

        // Split on {player} position so we can attach hover/click only to the name part
        int playerIdx = format.indexOf("{player}");
        if (playerIdx < 0) {
            // No {player} token — return plain formatted string
            return LEGACY.deserialize(resolved);
        }

        // Part before {player}
        String before = resolveChatPlaceholders(format.substring(0, playerIdx), sender, message);
        // Player name token
        String nameText = sender.getName();
        // Part after {player}
        String after = resolveChatPlaceholders(
                format.substring(playerIdx + "{player}".length()), sender, message);

        TextComponent.Builder nameComponent = Component.text()
                .content(nameText)
                // Carry over color/decoration from legacy context around the name
                .append(Component.empty());

        // Apply hover
        if (hover != null && !hover.isBlank()) {
            String resolvedHover = resolveChatPlaceholders(hover, sender, message);
            nameComponent.hoverEvent(HoverEvent.showText(LEGACY.deserialize(resolvedHover)));
        }

        // Apply click
        ClickEvent clickEvent = click.resolve(sender.getName());
        if (clickEvent != null) {
            nameComponent.clickEvent(clickEvent);
        }

        return Component.text()
                .append(LEGACY.deserialize(before))
                .append(nameComponent.build())
                .append(LEGACY.deserialize(after))
                .build();
    }

    // ── Bridge messages ───────────────────────────────────────

    /**
     * Deserialises a legacy-formatted bridge message and appends clickable attachment tags.
     */
    public static Component bridgeMessage(String legacyText, List<MediaAttachment> attachments) {
        TextComponent.Builder builder = Component.text();
        builder.append(LEGACY.deserialize(legacyText));

        for (MediaAttachment att : attachments) {
            builder.append(Component.space());
            builder.append(attachmentTag(att));
        }
        return builder.build();
    }

    // ── Auto-messages ─────────────────────────────────────────

    /**
     * Builds an auto-message component with optional hover and click on the entire text.
     */
    public static Component autoMessage(String legacyText, String hover, ClickAction click) {
        TextComponent.Builder builder = Component.text()
                .append(LEGACY.deserialize(legacyText));

        if (hover != null && !hover.isBlank()) {
            builder.hoverEvent(HoverEvent.showText(LEGACY.deserialize(hover)));
        }

        ClickEvent clickEvent = click.resolve("");
        if (clickEvent != null) {
            builder.clickEvent(clickEvent);
        }
        return builder.build();
    }

    // ── Helpers ───────────────────────────────────────────────

    private static String resolveChatPlaceholders(String template, Player sender, String message) {
        return template
                .replace("{player}",       sender.getName())
                .replace("{display_name}", LEGACY.serialize(sender.displayName()))
                .replace("{world}",        sender.getWorld().getName())
                .replace("{ping}",         String.valueOf(sender.getPing()))
                .replace("{message}",      message);
    }

    /**
     * Renders one attachment as {@code [label]} or {@code [label: meta]},
     * underlined and optionally clickable.
     */
    private static Component attachmentTag(MediaAttachment att) {
        String label = att.meta() != null && !att.meta().isBlank()
                ? "[" + att.type().label() + ": " + att.meta() + "]"
                : "[" + att.type().label() + "]";

        TextComponent.Builder tag = Component.text()
                .content(label)
                .decorate(TextDecoration.UNDERLINED);

        if (att.url() != null && !att.url().isBlank()) {
            tag.clickEvent(ClickEvent.openUrl(att.url()));
            tag.hoverEvent(HoverEvent.showText(Component.text("Открыть: " + att.url())));
        }
        return tag.build();
    }
}