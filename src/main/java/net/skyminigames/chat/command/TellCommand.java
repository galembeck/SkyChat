package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /tell &lt;jogador&gt; &lt;mensagem&gt;  sends a private message
 * /tell &lt;jogador&gt;              starts / stops a private conversation (normal chat goes to that player)
 */
@RequiredArgsConstructor
public final class TellCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skychat.tell")) {
            player.sendMessage(messages.get("general.no-permission"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(messages.get("private.usage"));
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(messages.get("private.not-online", "player", args[0]));
            return true;
        }

        if (args.length == 1) {
            toggleLock(player, target);
            return true;
        }
        plugin.getChatService().sendPrivate(player, target, join(args, 1));
        return true;
    }

    private void toggleLock(Player player, Player target) {
        Messages messages = plugin.getMessages();
        ChatPlayer chatPlayer = plugin.getPlayers().get(player);
        if (target.getUniqueId().equals(chatPlayer.getTellLock())) {
            chatPlayer.setTellLock(null);
            player.sendMessage(messages.get("private.lock-off", "player", target.getName()));
        } else if (target.equals(player)) {
            player.sendMessage(messages.get("private.self"));
        } else {
            chatPlayer.setTellLock(target.getUniqueId());
            player.sendMessage(messages.get("private.lock-on", "player", target.getName()));
        }
    }

    static String join(String[] args, int from) {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }
}
