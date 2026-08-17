package su.deworld.dwchat.config;

/**
 * One entry in the auto-messages list.
 *
 * @param text   Legacy-formatted message text
 * @param hover  Hover tooltip text (empty = no hover)
 * @param click  Click action (may be NONE)
 */
public record AutoMessageEntry(String text, String hover, ClickAction click) {}