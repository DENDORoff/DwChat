package su.deworld.dwchat.bridge.telegram;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.bridge.BridgeManager;
import su.deworld.dwchat.bridge.MediaAttachment;
import su.deworld.dwchat.config.PluginConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramBridge implements LongPollingSingleThreadUpdateConsumer {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>\"]+", Pattern.CASE_INSENSITIVE);

    private final DwChat plugin;
    private final BridgeManager bridgeManager;
    private TelegramClient telegramClient;
    private TelegramBotsLongPollingApplication botsApp;

    public TelegramBridge(DwChat plugin, BridgeManager bridgeManager) {
        this.plugin        = plugin;
        this.bridgeManager = bridgeManager;
    }

    public void start() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (cfg.telegramToken.isBlank() || cfg.telegramToken.equals("YOUR_TELEGRAM_BOT_TOKEN")) {
            plugin.logWarning("[Telegram] Token not configured.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                telegramClient = new OkHttpTelegramClient(cfg.telegramToken);
                botsApp = new TelegramBotsLongPollingApplication();
                botsApp.registerBot(cfg.telegramToken, this);
                plugin.getLogger().info("[Telegram] Bot started.");
            } catch (Exception e) {
                plugin.logSevere("[Telegram] Failed to start.", e);
            }
        });
    }

    public void stop() {
        if (botsApp != null) {
            try { botsApp.close(); } catch (Exception ignored) {}
            botsApp = null;
        }
        telegramClient = null;
    }

    // ── Incoming ──────────────────────────────────────────────

    @Override
    public void consume(Update update) {
        if (!update.hasMessage()) return;
        Message msg = update.getMessage();
        PluginConfig cfg = plugin.getPluginConfig();

        if (msg.getChatId() != cfg.telegramChatId) return;
        if (cfg.telegramTopicThreadId != 0) {
            Integer tid = msg.getMessageThreadId();
            if (tid == null || tid != cfg.telegramTopicThreadId) return;
        }
        if (msg.getFrom() != null && Boolean.TRUE.equals(msg.getFrom().getIsBot())) return;

        String firstName = msg.getFrom() != null ? msg.getFrom().getFirstName() : "Unknown";
        String username  = msg.getFrom() != null ? msg.getFrom().getUserName() : null;
        String messageUrl = buildMessageUrl(cfg, msg.getMessageId());

        String rawText = msg.hasText() ? msg.getText()
                       : msg.hasCaption() ? msg.getCaption() : "";

        // Commands
        if (!rawText.isBlank()) {
            for (Map.Entry<String, String> e : cfg.telegramCommands.entrySet()) {
                if (rawText.equalsIgnoreCase(e.getKey())) {
                    sendReply(msg.getChatId(), msg.getMessageId(), resolveCmd(e.getValue()));
                    return;
                }
            }
        }

        // Attachments
        List<MediaAttachment> attachments = new ArrayList<>();
        if (msg.hasPhoto())     attachments.add(MediaAttachment.of(MediaAttachment.Type.PHOTO,   messageUrl));
        if (msg.hasVideo())     attachments.add(MediaAttachment.of(MediaAttachment.Type.VIDEO,   messageUrl));
        if (msg.hasAnimation()) attachments.add(MediaAttachment.of(MediaAttachment.Type.GIF,     messageUrl));
        if (msg.hasVoice())     attachments.add(MediaAttachment.of(MediaAttachment.Type.VOICE,   messageUrl, fmt(msg.getVoice().getDuration())));
        if (msg.hasVideoNote()) attachments.add(MediaAttachment.of(MediaAttachment.Type.VIDEO,   messageUrl, fmt(msg.getVideoNote().getDuration())));
        if (msg.hasAudio())     attachments.add(MediaAttachment.of(MediaAttachment.Type.VOICE,   messageUrl, msg.getAudio().getTitle()));
        if (msg.hasDocument())  attachments.add(MediaAttachment.of(MediaAttachment.Type.FILE,    messageUrl, msg.getDocument().getFileName()));
        if (msg.hasSticker()) {
            String stickerMeta = msg.getSticker().getEmoji();
            if (stickerMeta == null || stickerMeta.isBlank()) stickerMeta = msg.getSticker().getSetName();
            attachments.add(MediaAttachment.of(MediaAttachment.Type.STICKER, null, stickerMeta));
        }

        // URLs from text
        String displayText = rawText;
        if (!rawText.isBlank()) {
            Matcher m = URL_PATTERN.matcher(rawText);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String url = m.group();
                if (url.contains("tenor.com") || url.contains("giphy.com") || url.endsWith(".gif"))
                    attachments.add(MediaAttachment.of(MediaAttachment.Type.GIF, url));
                else
                    attachments.add(MediaAttachment.of(MediaAttachment.Type.LINK, url));
                m.appendReplacement(sb, "");
            }
            m.appendTail(sb);
            displayText = sb.toString().trim();
        }

        // Reply
        Message rep = msg.getReplyToMessage();
        String replyAuthor = null, replyMessage = null;
        if (rep != null) {
            if (rep.getFrom() != null) replyAuthor = rep.getFrom().getFirstName();
            if (rep.hasText())         replyMessage = rep.getText();
            else if (rep.hasCaption()) replyMessage = rep.getCaption();
        }

        bridgeManager.onTelegramMessage(firstName, username, displayText,
                replyAuthor, replyMessage, attachments);
    }

    // ── Outgoing ──────────────────────────────────────────────

    public void sendMinecraftChat(String player, String message, String channel) {
        String text = plugin.getPluginConfig().telegramMinecraftFormat
                .replace("{player}", player).replace("{message}", escMd(message))
                .replace("{channel}", channel);
        sendToChat(text);
    }

    public void sendJoin(String player) {
        sendToChat(plugin.getPluginConfig().joinFormatTelegram.replace("{player}", player));
    }

    public void sendLeave(String player) {
        sendToChat(plugin.getPluginConfig().leaveFormatTelegram.replace("{player}", player));
    }

    public void sendRaw(String text) { sendToChat(text); }

    // ── Helpers ───────────────────────────────────────────────

    private void sendToChat(String text) {
        if (telegramClient == null) return;
        PluginConfig cfg = plugin.getPluginConfig();
        SendMessage.SendMessageBuilder b = SendMessage.builder()
                .chatId(cfg.telegramChatId).text(text).parseMode("Markdown");
        if (cfg.telegramTopicThreadId != 0) b.messageThreadId(cfg.telegramTopicThreadId);
        try { telegramClient.execute(b.build()); }
        catch (TelegramApiException e) { plugin.logWarning("[Telegram] Send failed: " + e.getMessage()); }
    }

    private void sendReply(long chatId, int replyTo, String text) {
        if (telegramClient == null) return;
        PluginConfig cfg = plugin.getPluginConfig();
        SendMessage.SendMessageBuilder b = SendMessage.builder()
                .chatId(chatId).replyToMessageId(replyTo).text(text);
        if (cfg.telegramTopicThreadId != 0) b.messageThreadId(cfg.telegramTopicThreadId);
        try { telegramClient.execute(b.build()); }
        catch (TelegramApiException e) { plugin.logWarning("[Telegram] Reply failed: " + e.getMessage()); }
    }

    private static String buildMessageUrl(PluginConfig cfg, int msgId) {
        long chatId = cfg.telegramChatId;
        if (chatId < 0) {
            String s = String.valueOf(Math.abs(chatId));
            if (s.startsWith("100")) s = s.substring(3);
            return "https://t.me/c/" + s + "/" + msgId;
        }
        return "";
    }

    private static String fmt(int sec) {
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    private static String escMd(String text) {
        return text.replace("`", "'").replace("_", "\\_");
    }

    private String resolveCmd(String template) {
        return template.replace("{online}",
                String.valueOf(plugin.getServer().getOnlinePlayers().size()));
    }
}