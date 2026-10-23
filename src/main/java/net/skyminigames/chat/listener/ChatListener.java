package net.skyminigames.chat.listener;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.channel.Channel;
import net.skyminigames.chat.command.ChannelCommand;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;

@RequiredArgsConstructor
public final class ChatListener implements Listener {

    private final SkyChat plugin;

    /**
     * SkyChat delivers every message itself. Running last and ignoring cancelled events means other
     * plugins (SkyLogin for players not logged in, Essentials mute...) still get to block a message.
     * The work moves to the main thread: permissions, locations and economy are not thread-safe.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (event.isAsynchronous()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getChatService().handlePlainChat(player, message);
                }
            });
        } else {
            plugin.getChatService().handlePlainChat(player, message);
        }
    }

    /** Channel shortcuts: "/g oi" sends to the global channel, "/g" alone makes it the default channel. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String text = event.getMessage().substring(1);
        String[] parts = text.split(" ", 2);
        Channel channel = plugin.getChannels().byShortcut(parts[0].toLowerCase(Locale.ROOT));
        if (channel == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = parts.length > 1 ? parts[1].trim() : "";
        if (message.isEmpty()) {
            ChannelCommand.focus(plugin, player, plugin.getPlayers().get(player), channel.getName());
        } else {
            plugin.getChatService().sendToChannel(player, channel, message);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayers().get(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getPlayers().unload(event.getPlayer());
    }

    /** Walking to another block ends AFK. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        ChatPlayer chatPlayer = plugin.getPlayers().getIfOnline(event.getPlayer().getUniqueId());
        if (chatPlayer != null && chatPlayer.isAfk()) {
            plugin.getChatService().clearAfk(event.getPlayer(), chatPlayer);
        }
    }
}
