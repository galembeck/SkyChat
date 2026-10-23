package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.channel.Channel;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ch               lists channels
 * /ch &lt;canal&gt;       talks in that channel by default
 * /ch mute &lt;canal&gt;  stops / starts seeing a channel
 */
@RequiredArgsConstructor
public final class ChannelCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        ChatPlayer chatPlayer = plugin.getPlayers().get(player);

        if (args.length == 0) {
            list(player, chatPlayer);
            return true;
        }
        if (args[0].equalsIgnoreCase("mute")) {
            if (args.length != 2) {
                player.sendMessage(messages.get("channel.usage"));
                return true;
            }
            toggleIgnore(player, chatPlayer, args[1]);
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(messages.get("channel.usage"));
            return true;
        }
        focus(plugin, player, chatPlayer, args[0]);
        return true;
    }

    /** Also used by "/g" without a message. */
    public static void focus(SkyChat plugin, Player player, ChatPlayer chatPlayer, String channelName) {
        Messages messages = plugin.getMessages();
        Channel channel = plugin.getChannels().get(channelName);
        if (channel == null) {
            player.sendMessage(messages.get("channel.not-found", "channel", channelName));
            return;
        }
        if (!player.hasPermission(channel.focusPermission()) || !player.hasPermission(channel.chatPermission())) {
            player.sendMessage(messages.get("channel.no-permission", "channel", channel.getName()));
            return;
        }
        chatPlayer.setFocusChannel(channel.getName());
        chatPlayer.setTellLock(null);
        player.sendMessage(messages.get("channel.focused", "channel", channel.getName(), "color", channel.getColor()));
    }

    private void list(Player player, ChatPlayer chatPlayer) {
        Messages messages = plugin.getMessages();
        Channel current = plugin.getChatService().focusedChannel(chatPlayer);
        player.sendMessage(messages.get("channel.list-header"));
        for (Channel channel : plugin.getChannels().getChannels()) {
            if (!player.hasPermission(channel.chatPermission())) {
                continue;
            }
            String marker = channel == current ? messages.raw("channel.list-current")
                    : chatPlayer.getIgnoredChannels().contains(channel.getName()) ? messages.raw("channel.list-ignored") : "";
            player.sendMessage(messages.raw("channel.list-entry", "channel", channel.getName(), "color", channel.getColor(),
                    "nick", channel.getNickname(), "current", marker));
        }
    }

    private void toggleIgnore(Player player, ChatPlayer chatPlayer, String channelName) {
        Messages messages = plugin.getMessages();
        if (!player.hasPermission("skychat.ignore")) {
            player.sendMessage(messages.get("general.no-permission"));
            return;
        }
        Channel channel = plugin.getChannels().get(channelName);
        if (channel == null) {
            player.sendMessage(messages.get("channel.not-found", "channel", channelName));
            return;
        }
        if (chatPlayer.getIgnoredChannels().remove(channel.getName())) {
            player.sendMessage(messages.get("channel.unignoring", "channel", channel.getName(), "color", channel.getColor()));
        } else {
            chatPlayer.getIgnoredChannels().add(channel.getName());
            player.sendMessage(messages.get("channel.ignoring", "channel", channel.getName(), "color", channel.getColor()));
        }
    }
}
