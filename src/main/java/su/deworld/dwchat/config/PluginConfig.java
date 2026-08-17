package su.deworld.dwchat.config;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import su.deworld.dwchat.DwChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginConfig {

    // ── Global chat ───────────────────────────────────────────
    public final String globalPrefix;
    public final String globalFormat;
    public final String globalHover;
    public final ClickAction globalClick;
    public final SoundConfig globalSound;

    // ── Local chat ────────────────────────────────────────────
    public final int localRadius;
    public final String localFormat;
    public final String localHover;
    public final ClickAction localClick;
    public final SoundConfig localSound;

    // ── Discord ───────────────────────────────────────────────
    public final boolean discordEnabled;
    public final String discordToken;
    public final String discordChannelId;
    public final String discordActivityType;
    public final String discordActivityText;
    public final String discordStreamUrl;
    public final boolean discordSendLocal;
    public final boolean discordSendGlobal;
    public final String discordMinecraftFormat;
    public final String discordToMinecraftFormat;
    public final String discordToMinecraftReplyFormat;
    public final SoundConfig discordSound;
    public final String discordTelegramForwardFormat;
    public final Map<String, String> discordCommands;

    // ── Telegram ──────────────────────────────────────────────
    public final boolean telegramEnabled;
    public final String telegramToken;
    public final long telegramChatId;
    public final int telegramTopicThreadId;
    public final boolean telegramSendLocal;
    public final boolean telegramSendGlobal;
    public final String telegramMinecraftFormat;
    public final String telegramToMinecraftFormat;
    public final String telegramToMinecraftReplyFormat;
    public final SoundConfig telegramSound;
    public final String telegramDiscordForwardFormat;
    public final Map<String, String> telegramCommands;

    // ── Forwarding ────────────────────────────────────────────
    public final boolean forwardDiscordToTelegram;
    public final boolean forwardTelegramToDiscord;

    // ── Events ────────────────────────────────────────────────
    public final boolean sendJoinLeave;
    public final String joinFormatDiscord;
    public final String joinFormatTelegram;
    public final String leaveFormatDiscord;
    public final String leaveFormatTelegram;

    // ── Auto-messages ─────────────────────────────────────────
    public final boolean autoMessagesEnabled;
    public final int autoMessagesInterval;
    public final boolean autoMessagesRandom;
    public final List<AutoMessageEntry> autoMessages;

    // ── Plugin messages ───────────────────────────────────────
    public final String msgNoPermission;
    public final String msgReloadSuccess;
    public final String msgUsage;
    public final String nooneHeardMessage;

    public PluginConfig(DwChat plugin) {
        FileConfiguration c = plugin.getConfig();

        // Global chat
        globalPrefix = c.getString("chat.global.prefix", "!");
        globalFormat = c.getString("chat.global.format", "&8[&fG&8] &7{player}&8: &f{message}");
        globalHover  = c.getString("chat.global.hover", "");
        globalClick  = loadClick(c.getConfigurationSection("chat.global.click"));
        globalSound  = SoundConfig.load(c.getConfigurationSection("chat.global.sound"),
                Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.0f);

        // Local chat
        localRadius  = c.getInt("chat.local.radius", 100);
        localFormat  = c.getString("chat.local.format", "&7{player}&8: &f{message}");
        localHover   = c.getString("chat.local.hover", "");
        localClick   = loadClick(c.getConfigurationSection("chat.local.click"));
        localSound   = SoundConfig.load(c.getConfigurationSection("chat.local.sound"),
                Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 0.8f);

        // Discord
        discordEnabled           = c.getBoolean("discord.enabled", false);
        discordToken             = c.getString("discord.token", "");
        discordChannelId         = c.getString("discord.channel-id", "");
        discordActivityType      = c.getString("discord.activity.type", "PLAYING").toUpperCase();
        discordActivityText      = c.getString("discord.activity.text", "Minecraft");
        discordStreamUrl         = c.getString("discord.activity.stream-url", "");
        discordSendLocal         = c.getBoolean("discord.send-local", false);
        discordSendGlobal        = c.getBoolean("discord.send-global", true);
        discordMinecraftFormat   = c.getString("discord.minecraft-to-discord", "{player} » {message}");
        discordToMinecraftFormat = c.getString("discord.discord-to-minecraft",
                "&7[&bDiscord&7] &r{role_color}{role} &f{author}&7: &r{message}");
        discordToMinecraftReplyFormat = c.getString("discord.discord-to-minecraft-reply",
                "&7[&bDiscord&7] &r{role_color}{role} &f{author} &7↩ &f{reply_author}&7: &r{message}");
        discordSound             = SoundConfig.load(c.getConfigurationSection("discord.sound"),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
        discordTelegramForwardFormat = c.getString("discord.telegram-forward-format",
                "📨 **[Telegram]** {author}: {message}");
        discordCommands          = loadStringMap(c.getConfigurationSection("discord.commands"));

        // Telegram
        telegramEnabled              = c.getBoolean("telegram.enabled", false);
        telegramToken                = c.getString("telegram.token", "");
        telegramChatId               = c.getLong("telegram.chat-id", 0L);
        telegramTopicThreadId        = c.getInt("telegram.topic-thread-id", 0);
        telegramSendLocal            = c.getBoolean("telegram.send-local", false);
        telegramSendGlobal           = c.getBoolean("telegram.send-global", true);
        telegramMinecraftFormat      = c.getString("telegram.minecraft-to-telegram", "*{player}* » {message}");
        telegramToMinecraftFormat    = c.getString("telegram.telegram-to-minecraft",
                "&7[&9Telegram&7] &f{author}&7: &r{message}");
        telegramToMinecraftReplyFormat = c.getString("telegram.telegram-to-minecraft-reply",
                "&7[&9Telegram&7] &f{author} &7↩ &f{reply_author}&7: &r{message}");
        telegramSound                = SoundConfig.load(c.getConfigurationSection("telegram.sound"),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        telegramDiscordForwardFormat = c.getString("telegram.discord-forward-format",
                "📨 *[Discord]* {author}: {message}");
        telegramCommands             = loadStringMap(c.getConfigurationSection("telegram.commands"));

        // Forwarding
        forwardDiscordToTelegram = c.getBoolean("forwarding.discord-to-telegram", true);
        forwardTelegramToDiscord = c.getBoolean("forwarding.telegram-to-discord", true);

        // Events
        sendJoinLeave       = c.getBoolean("events.send-join-leave", true);
        joinFormatDiscord   = c.getString("events.join-format-discord", "✅ **{player}** joined.");
        joinFormatTelegram  = c.getString("events.join-format-telegram", "✅ *{player}* зашёл.");
        leaveFormatDiscord  = c.getString("events.leave-format-discord", "❌ **{player}** left.");
        leaveFormatTelegram = c.getString("events.leave-format-telegram", "❌ *{player}* вышел.");

        // Auto-messages
        autoMessagesEnabled  = c.getBoolean("auto-messages.enabled", false);
        autoMessagesInterval = c.getInt("auto-messages.interval", 300);
        autoMessagesRandom   = c.getBoolean("auto-messages.random", false);
        autoMessages         = loadAutoMessages(c.getMapList("auto-messages.messages"));

        // Plugin messages
        msgNoPermission  = c.getString("messages.no-permission", "&cНет прав.");
        msgReloadSuccess = c.getString("messages.reload-success", "&aПерезагружено.");
        msgUsage         = c.getString("messages.usage", "&e/dwchat reload");
        nooneHeardMessage = c.getString("messages.noone-heard", "&7Вас никто не услышал.");
    }

    // ── Loaders ───────────────────────────────────────────────

    public static ClickAction loadClick(ConfigurationSection section) {
        if (section == null) return ClickAction.none();
        String type  = section.getString("type", "");
        String value = section.getString("value", "");
        return new ClickAction(ClickAction.Type.parse(type), value);
    }

    private static Map<String, String> loadStringMap(ConfigurationSection section) {
        if (section == null) return Collections.emptyMap();
        Map<String, String> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key, section.getString(key, ""));
        }
        return Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    private static List<AutoMessageEntry> loadAutoMessages(List<Map<?, ?>> list) {
        List<AutoMessageEntry> result = new ArrayList<>();
        for (Map<?, ?> raw : list) {
            Map<String, Object> map = (Map<String, Object>) raw;
            String text  = String.valueOf(map.getOrDefault("text", ""));
            String hover = String.valueOf(map.getOrDefault("hover", ""));
            ClickAction click = ClickAction.none();

            Object clickObj = map.get("click");
            if (clickObj instanceof Map<?, ?> clickMap) {
                // getOrDefault не компилируется на wildcard Map<?,?> — используем get + Elvis
                Object ctypeRaw  = clickMap.get("type");
                Object cvalueRaw = clickMap.get("value");
                String ctype  = ctypeRaw  != null ? String.valueOf(ctypeRaw)  : "";
                String cvalue = cvalueRaw != null ? String.valueOf(cvalueRaw) : "";
                click = new ClickAction(ClickAction.Type.parse(ctype), cvalue);
            }
            result.add(new AutoMessageEntry(text, hover, click));
        }
        return Collections.unmodifiableList(result);
    }
}