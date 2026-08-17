package su.deworld.dwchat.automessage;

import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;
import su.deworld.dwchat.DwChat;
import su.deworld.dwchat.chat.ComponentFactory;
import su.deworld.dwchat.config.AutoMessageEntry;
import su.deworld.dwchat.config.PluginConfig;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoMessageManager {

    private final DwChat plugin;
    private BukkitTask task;
    private int index = 0;

    public AutoMessageManager(DwChat plugin) {
        this.plugin = plugin;
    }

    public void start() {
        PluginConfig cfg = plugin.getPluginConfig();
        if (!cfg.autoMessagesEnabled || cfg.autoMessages.isEmpty()) return;

        long ticks = cfg.autoMessagesInterval * 20L;

        // BroadcastTask — статический вложенный класс без лямбд и захватов.
        // Shadow ASM с JDK 22 не может обрабатывать ни method reference (this::x),
        // ни анонимные классы захватывающие внешний this через синтетическое поле.
        // Статический вложенный класс с явной ссылкой через конструктор — единственный
        // надёжный способ избежать проблем с invokedynamic в Shadow JAR.
        this.task = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, new BroadcastTask(this), ticks, ticks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        index = 0;
    }

    void broadcast() {
        PluginConfig cfg = plugin.getPluginConfig();
        List<AutoMessageEntry> messages = cfg.autoMessages;
        if (messages.isEmpty()) return;

        final AutoMessageEntry entry;
        if (cfg.autoMessagesRandom) {
            int size = messages.size();
            entry = messages.get(ThreadLocalRandom.current().nextInt(size));
        } else {
            if (index >= messages.size()) {
                index = 0;
            }
            entry = messages.get(index);
            index = index + 1;
            if (index >= messages.size()) {
                index = 0;
            }
        }

        Component component = ComponentFactory.autoMessage(
                entry.text(), entry.hover(), entry.click());
        plugin.getServer().broadcast(component);
    }

    // ── Статический вложенный класс — нет синтетических полей, нет invokedynamic ──

    static final class BroadcastTask implements Runnable {

        private final AutoMessageManager manager;

        BroadcastTask(AutoMessageManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            manager.broadcast();
        }
    }
}