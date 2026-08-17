package su.deworld.dwchat;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import su.deworld.dwchat.automessage.AutoMessageManager;
import su.deworld.dwchat.bridge.BridgeManager;
import su.deworld.dwchat.commands.DwChatCommand;
import su.deworld.dwchat.commands.filter.CommandFilterManager;
import su.deworld.dwchat.config.PluginConfig;
import su.deworld.dwchat.listeners.*;
import su.deworld.dwchat.moderation.ModerationManager;

import java.util.Objects;
import java.util.logging.Level;

public final class DwChat extends JavaPlugin {

    private static DwChat instance;

    private PluginConfig pluginConfig;
    private BridgeManager bridgeManager;
    private AutoMessageManager autoMessageManager;
    private ModerationManager moderationManager;
    private CommandFilterManager commandFilterManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        pluginConfig          = new PluginConfig(this);
        moderationManager     = new ModerationManager(this);
        commandFilterManager  = new CommandFilterManager(this);
        bridgeManager         = new BridgeManager(this);
        autoMessageManager    = new AutoMessageManager(this);

        bridgeManager.start();
        autoMessageManager.start();
        registerListeners();
        registerCommands();

        getLogger().info("DwChat enabled.");
    }

    @Override
    public void onDisable() {
        if (bridgeManager      != null) bridgeManager.stop();
        if (autoMessageManager != null) autoMessageManager.stop();
        getLogger().info("DwChat disabled.");
    }

    public void reload() {
        if (bridgeManager      != null) bridgeManager.stop();
        if (autoMessageManager != null) autoMessageManager.stop();
        HandlerList.unregisterAll(this);

        reloadConfig();
        pluginConfig         = new PluginConfig(this);
        moderationManager    = new ModerationManager(this);
        commandFilterManager = new CommandFilterManager(this);

        bridgeManager      = new BridgeManager(this);
        autoMessageManager = new AutoMessageManager(this);
        bridgeManager.start();
        autoMessageManager.start();
        registerListeners();

        getLogger().info("DwChat reloaded.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new CommandFilterListener(this), this);
        pm.registerEvents(new BookSignModerationListener(this), this);
    }

    private void registerCommands() {
        var cmd = Objects.requireNonNull(getCommand("dwchat"));
        var handler = new DwChatCommand(this);
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
    }

    public static DwChat getInstance()                      { return instance; }
    public PluginConfig getPluginConfig()                   { return pluginConfig; }
    public BridgeManager getBridgeManager()                 { return bridgeManager; }
    public AutoMessageManager getAutoMessageManager()       { return autoMessageManager; }
    public ModerationManager getModerationManager()         { return moderationManager; }
    public CommandFilterManager getCommandFilterManager()   { return commandFilterManager; }

    public void logWarning(String msg)             { getLogger().warning(msg); }
    public void logSevere(String msg, Throwable t) { getLogger().log(Level.SEVERE, msg, t); }
}