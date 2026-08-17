package su.deworld.dwchat.config;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Per-channel/source sound notification configuration.
 */
public record SoundConfig(boolean enabled, Sound sound, float volume, float pitch) {

    public static SoundConfig load(ConfigurationSection section, Sound defaultSound,
                                   float defaultVol, float defaultPitch) {
        if (section == null) return disabled();
        boolean enabled = section.getBoolean("enabled", false);
        if (!enabled) return disabled();

        Sound sound = defaultSound;
        String typeName = section.getString("type", "");
        if (!typeName.isBlank()) {
            try {
                sound = Sound.valueOf(typeName.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        float volume = (float) section.getDouble("volume", defaultVol);
        float pitch  = (float) section.getDouble("pitch",  defaultPitch);
        return new SoundConfig(true, sound, volume, pitch);
    }

    public static SoundConfig disabled() {
        return new SoundConfig(false, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    /** Plays this sound for a single player if enabled. */
    public void playTo(Player player) {
        if (!enabled) return;
        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}