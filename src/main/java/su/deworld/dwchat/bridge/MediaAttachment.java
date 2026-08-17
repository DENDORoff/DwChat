package su.deworld.dwchat.bridge;

/**
 * Describes a single media attachment coming from Discord or Telegram.
 *
 * @param type  Attachment type label shown in Minecraft chat
 * @param url   Direct URL to open on click. Null = no link.
 * @param meta  Optional extra info: filename, duration, emoji, etc.
 */
public record MediaAttachment(Type type, String url, String meta) {

    public enum Type {
        PHOTO("фото"),
        VIDEO("видео"),
        GIF("гиф"),
        VOICE("голосовое"),
        FILE("файл"),
        STICKER("стикер"),
        LINK("ссылка");

        private final String label;
        Type(String label) { this.label = label; }
        public String label() { return label; }
    }

    public static MediaAttachment of(Type type, String url) {
        return new MediaAttachment(type, url, null);
    }

    public static MediaAttachment of(Type type, String url, String meta) {
        return new MediaAttachment(type, url, meta);
    }
}