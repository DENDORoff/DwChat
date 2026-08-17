package su.deworld.dwchat.moderation;

/**
 * Result returned by a moderation check.
 *
 * @param blocked       whether the event should be cancelled
 * @param modifiedText  possibly altered message (e.g. censored word replaced); null = unchanged
 */
public record CheckResult(boolean blocked, String modifiedText) {

    public static final CheckResult PASS = new CheckResult(false, null);

    public static CheckResult block()                    { return new CheckResult(true, null); }
    public static CheckResult modify(String newText)     { return new CheckResult(false, newText); }
    public static CheckResult blockModify(String text)   { return new CheckResult(true, text); }
}