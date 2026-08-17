package su.deworld.dwchat.bridge.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.bridge.BridgeManager;
import su.deworld.dwchat.bridge.MediaAttachment;
import su.deworld.dwchat.config.PluginConfig;
import su.deworld.dwchat.util.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordBridge extends ListenerAdapter {

    private static final Pattern CUSTOM_EMOJI = Pattern.compile("<a?:([^:]+):\\d+>");
    private static final Pattern URL_PATTERN  = Pattern.compile(
            "https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);

    private final DwChat plugin;
    private final BridgeManager bridgeManager;
    private JDA jda;

    public DiscordBridge(DwChat plugin, BridgeManager bridgeManager) {
        this.plugin        = plugin;
        this.bridgeManager = bridgeManager;
    }

    public void start() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (cfg.discordToken.isBlank() || cfg.discordToken.equals("YOUR_DISCORD_BOT_TOKEN")) {
            plugin.logWarning("[Discord] Token not configured.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                jda = JDABuilder.createLight(cfg.discordToken,
                                EnumSet.of(GatewayIntent.GUILD_MESSAGES,
                                        GatewayIntent.MESSAGE_CONTENT,
                                        GatewayIntent.GUILD_MEMBERS,
                                        GatewayIntent.GUILD_PRESENCES))
                        .disableCache(CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS, CacheFlag.EMOJI)
                        .addEventListeners(this)
                        .setStatus(OnlineStatus.ONLINE)
                        .setActivity(buildActivity(cfg))
                        .build();
                jda.awaitReady();
                plugin.getLogger().info("[Discord] Connected as " + jda.getSelfUser().getAsTag());
            } catch (Exception e) {
                plugin.logSevere("[Discord] Failed to connect.", e);
            }
        });
    }

    public void stop() {
        if (jda != null) { jda.shutdown(); jda = null; }
    }

    // ── Incoming ──────────────────────────────────────────────

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        PluginConfig cfg = plugin.getPluginConfig();
        if (!event.getChannel().getId().equals(cfg.discordChannelId)) return;

        Message msg     = event.getMessage();
        String rawText  = msg.getContentDisplay();
        String author   = event.getAuthor().getName();
        Member member   = event.getMember();

        // Role
        String roleName = "", roleColor = "";
        if (member != null) {
            Role top = member.getRoles().stream()
                    .filter(r -> !r.getName().equals("@everyone"))
                    .max(Comparator.comparingInt(Role::getPosition))
                    .orElse(null);
            if (top != null) {
                roleName  = top.getName();
                roleColor = ColorUtil.discordColorToLegacy(top.getColor());
            }
        }

        // Message link
        String guildId     = event.isFromGuild() ? event.getGuild().getId() : "@me";
        String messageUrl  = "https://discord.com/channels/" + guildId
                + "/" + cfg.discordChannelId + "/" + msg.getId();

        String displayText = replaceCustomEmoji(rawText);

        // Commands
        for (Map.Entry<String, String> e : cfg.discordCommands.entrySet()) {
            if (rawText.equalsIgnoreCase(e.getKey())) {
                String resp = resolveCmd(e.getValue());
                msg.reply(resp).queue(null, err -> {
                    TextChannel ch = getChannel();
                    if (ch != null) ch.sendMessage(resp).queue();
                });
                return;
            }
        }

        // Attachments
        List<MediaAttachment> attachments = new ArrayList<>();
        for (Message.Attachment a : msg.getAttachments()) {
            attachments.add(MediaAttachment.of(classifyAttachment(a), messageUrl,
                    a.getContentType() != null && a.getFileName() != null
                            && classifyAttachment(a) == MediaAttachment.Type.FILE ? a.getFileName() : null));
        }
        for (var sticker : msg.getStickers()) {
            attachments.add(MediaAttachment.of(MediaAttachment.Type.STICKER, messageUrl, sticker.getName()));
        }

        // URLs in text
        Matcher m = URL_PATTERN.matcher(displayText);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String url = m.group();
            if (url.contains("tenor.com") || url.contains("giphy.com") || url.endsWith(".gif")) {
                attachments.add(MediaAttachment.of(MediaAttachment.Type.GIF, url));
            } else {
                attachments.add(MediaAttachment.of(MediaAttachment.Type.LINK, url));
            }
            m.appendReplacement(sb, "");
        }
        m.appendTail(sb);
        displayText = sb.toString().trim();

        // Reply
        var ref = msg.getReferencedMessage();
        String mcFormatted;
        if (ref != null) {
            mcFormatted = cfg.discordToMinecraftReplyFormat
                    .replace("{author}", author).replace("{role}", roleName)
                    .replace("{role_color}", roleColor).replace("{message}", displayText)
                    .replace("{reply_author}", ref.getAuthor().getName())
                    .replace("{reply_message}", replaceCustomEmoji(ref.getContentDisplay()));
        } else {
            mcFormatted = cfg.discordToMinecraftFormat
                    .replace("{author}", author).replace("{role}", roleName)
                    .replace("{role_color}", roleColor).replace("{message}", displayText);
        }

        bridgeManager.onDiscordMessageFormatted(mcFormatted, author, rawText, attachments);
    }

    // ── Outgoing ──────────────────────────────────────────────

    public void sendMinecraftChat(String player, String message, String channel) {
        TextChannel ch = getChannel();
        if (ch == null) return;
        String text = plugin.getPluginConfig().discordMinecraftFormat
                .replace("{player}", player).replace("{message}", message)
                .replace("{channel}", channel);
        ch.sendMessage(text).queue(null, e -> plugin.logWarning("[Discord] Send failed: " + e.getMessage()));
    }

    public void sendJoin(String player) {
        TextChannel ch = getChannel();
        if (ch != null) ch.sendMessage(
                plugin.getPluginConfig().joinFormatDiscord.replace("{player}", player)).queue();
    }

    public void sendLeave(String player) {
        TextChannel ch = getChannel();
        if (ch != null) ch.sendMessage(
                plugin.getPluginConfig().leaveFormatDiscord.replace("{player}", player)).queue();
    }

    public void sendRaw(String text) {
        TextChannel ch = getChannel();
        if (ch != null) ch.sendMessage(text).queue(
                null, e -> plugin.logWarning("[Discord] Forward failed: " + e.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────

    private static String replaceCustomEmoji(String text) {
        return CUSTOM_EMOJI.matcher(text).replaceAll(mr -> ":" + mr.group(1) + ":");
    }

    private static MediaAttachment.Type classifyAttachment(Message.Attachment a) {
        // Голосовые сообщения Discord имеют content-type "audio/ogg" и расширение .ogg
        // при этом они НЕ определяются isImage()/isVideo() — проверяем по content type
        String ct = a.getContentType() != null ? a.getContentType().toLowerCase() : "";
        if (ct.startsWith("audio/")) return MediaAttachment.Type.VOICE;

        if (a.isImage()) {
            if (a.getFileName().toLowerCase().endsWith(".gif")) return MediaAttachment.Type.GIF;
            return MediaAttachment.Type.PHOTO;
        }
        if (a.isVideo()) return MediaAttachment.Type.VIDEO;

        // Дополнительная проверка по расширению файла
        String fn = a.getFileName().toLowerCase();
        if (fn.endsWith(".mp3") || fn.endsWith(".ogg") || fn.endsWith(".wav")
                || fn.endsWith(".flac") || fn.endsWith(".aac") || fn.endsWith(".m4a")) {
            return MediaAttachment.Type.VOICE;
        }
        return MediaAttachment.Type.FILE;
    }

    private TextChannel getChannel() {
        if (jda == null) return null;
        TextChannel ch = jda.getTextChannelById(plugin.getPluginConfig().discordChannelId);
        if (ch == null) plugin.logWarning("[Discord] Channel not found.");
        return ch;
    }

    private Activity buildActivity(PluginConfig cfg) {
        return switch (cfg.discordActivityType) {
            case "WATCHING"   -> Activity.watching(cfg.discordActivityText);
            case "LISTENING"  -> Activity.listening(cfg.discordActivityText);
            case "STREAMING"  -> Activity.streaming(cfg.discordActivityText, cfg.discordStreamUrl);
            case "COMPETING"  -> Activity.competing(cfg.discordActivityText);
            default           -> Activity.playing(cfg.discordActivityText);
        };
    }

    private String resolveCmd(String template) {
        return template.replace("{online}",
                String.valueOf(plugin.getServer().getOnlinePlayers().size()));
    }
}