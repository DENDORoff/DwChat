package su.deworld.dwchat.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import su.deworld.dwchat.DwChat;

public class BookSignModerationListener implements Listener {

    private final DwChat plugin;

    public BookSignModerationListener(DwChat plugin) {
        this.plugin = plugin;
    }

    // ── Books ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        var meta = event.getNewBookMeta();
        boolean blocked = false;

        // Check each page
        var pages = meta.pages();
        for (int i = 0; i < pages.size(); i++) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(pages.get(i));
            String result = plugin.getModerationManager().checkBook(player, plain);
            if (result == null) {
                blocked = true;
                break;
            }
        }

        if (blocked) event.setCancelled(true);
    }

    // ── Signs ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        var lines = event.lines();

        for (int i = 0; i < lines.size(); i++) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(lines.get(i));
            if (plain.isBlank()) continue;

            String result = plugin.getModerationManager().checkSign(player, plain);
            if (result == null) {
                event.setCancelled(true);
                return;
            }
        }
    }
}