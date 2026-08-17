package su.deworld.dwchat.bridge;

import net.kyori.adventure.text.Component;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.bridge.discord.DiscordBridge;
import su.deworld.dwchat.bridge.telegram.TelegramBridge;
import su.deworld.dwchat.chat.ChatChannel;
import su.deworld.dwchat.chat.ComponentFactory;
import su.deworld.dwchat.config.PluginConfig;

import java.util.List;

public class BridgeManager {

    private final DwChat plugin;
    private DiscordBridge discordBridge;
    private TelegramBridge telegramBridge;

    public BridgeManager(DwChat plugin) {
        this.plugin = plugin;
    }

    public void start() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (cfg.discordEnabled) {
            discordBridge = new DiscordBridge(plugin, this);
            discordBridge.start();
        }
        if (cfg.telegramEnabled) {
            telegramBridge = new TelegramBridge(plugin, this);
            telegramBridge.start();
        }
    }

    public void stop() {
        if (discordBridge  != null) { discordBridge.stop();  discordBridge  = null; }
        if (telegramBridge != null) { telegramBridge.stop(); telegramBridge = null; }
    }

    // ── Called by Minecraft listeners ────────────────────────

    public void onMinecraftChat(String player, String message, ChatChannel channel) {
        PluginConfig cfg = plugin.getPluginConfig();

        boolean sendToDiscord  = (channel == ChatChannel.GLOBAL && cfg.discordSendGlobal)
                              || (channel == ChatChannel.LOCAL  && cfg.discordSendLocal);
        boolean sendToTelegram = (channel == ChatChannel.GLOBAL && cfg.telegramSendGlobal)
                              || (channel == ChatChannel.LOCAL  && cfg.telegramSendLocal);

        String channelLabel = channel == ChatChannel.GLOBAL ? "global" : "local";

        if (sendToDiscord  && discordBridge  != null) discordBridge.sendMinecraftChat(player, message, channelLabel);
        if (sendToTelegram && telegramBridge != null) telegramBridge.sendMinecraftChat(player, message, channelLabel);
    }

    public void onPlayerJoin(String player) {
        if (discordBridge  != null) discordBridge.sendJoin(player);
        if (telegramBridge != null) telegramBridge.sendJoin(player);
    }

    public void onPlayerLeave(String player) {
        if (discordBridge  != null) discordBridge.sendLeave(player);
        if (telegramBridge != null) telegramBridge.sendLeave(player);
    }

    // ── Called by bridge implementations ─────────────────────

    /**
     * Discord → Minecraft + optional Telegram forward.
     */
    public void onDiscordMessageFormatted(String formattedMinecraft, String author,
                                          String plainMessage, List<MediaAttachment> attachments) {
        PluginConfig cfg = plugin.getPluginConfig();
        Component component = ComponentFactory.bridgeMessage(formattedMinecraft, attachments);

        broadcastComponent(component, BroadcastSource.DISCORD);

        if (cfg.forwardDiscordToTelegram && telegramBridge != null) {
            String forwarded = cfg.telegramDiscordForwardFormat
                    .replace("{author}", author)
                    .replace("{message}", plainMessage);
            telegramBridge.sendRaw(forwarded);
        }
    }

    /**
     * Telegram → Minecraft + optional Discord forward.
     */
    public void onTelegramMessage(String author, String username, String message,
                                   String replyAuthor, String replyMessage,
                                   List<MediaAttachment> attachments) {
        PluginConfig cfg = plugin.getPluginConfig();

        String mcFormat;
        if (replyAuthor != null) {
            mcFormat = cfg.telegramToMinecraftReplyFormat
                    .replace("{author}", author)
                    .replace("{username}", username != null ? username : author)
                    .replace("{message}", message)
                    .replace("{reply_author}", replyAuthor)
                    .replace("{reply_message}", replyMessage != null ? replyMessage : "");
        } else {
            mcFormat = cfg.telegramToMinecraftFormat
                    .replace("{author}", author)
                    .replace("{username}", username != null ? username : author)
                    .replace("{message}", message);
        }

        Component component = ComponentFactory.bridgeMessage(mcFormat, attachments);
        broadcastComponent(component, BroadcastSource.TELEGRAM);

        if (cfg.forwardTelegramToDiscord && discordBridge != null) {
            String forwarded = cfg.discordTelegramForwardFormat
                    .replace("{author}", author)
                    .replace("{message}", message);
            discordBridge.sendRaw(forwarded);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private enum BroadcastSource { DISCORD, TELEGRAM }

    private void broadcastComponent(Component component, BroadcastSource source) {
        PluginConfig cfg = plugin.getPluginConfig();
        su.deworld.dwchat.config.SoundConfig sound =
                source == BroadcastSource.DISCORD ? cfg.discordSound : cfg.telegramSound;

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (var player : plugin.getServer().getOnlinePlayers()) {
                player.sendMessage(component);
                sound.playTo(player);
            }
        });
    }

    public DiscordBridge getDiscordBridge()   { return discordBridge; }
    public TelegramBridge getTelegramBridge() { return telegramBridge; }
}