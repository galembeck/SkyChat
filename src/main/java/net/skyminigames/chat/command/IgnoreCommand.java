package net.skyminigames.chat.command;

import lombok.RequiredArgsConstructor;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /ignore             lists ignored players
 * /ignore &lt;jogador&gt;   ignores / stops ignoring a player (channels and private messages)
 */
@RequiredArgsConstructor
public final class IgnoreCommand implements CommandExecutor {

    private final SkyChat plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("general.player-only"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("skychat.ignore")) {
            player.sendMessage(messages.get("general.no-permission"));
            return true;
        }
        ChatPlayer chatPlayer = plugin.getPlayers().get(player);

        if (args.length == 0) {
            list(player, chatPlayer);
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(messages.get("private.not-online", "player", args[0]));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(messages.get("ignore.self"));
            return true;
        }
        if (chatPlayer.getIgnoredPlayers().remove(target.getUniqueId())) {
            player.sendMessage(messages.get("ignore.removed", "player", target.getName()));
            return true;
        }
        if (target.hasPermission("skychat.block.ignore")) {
            player.sendMessage(messages.get("ignore.cannot"));
            return true;
        }
        chatPlayer.getIgnoredPlayers().add(target.getUniqueId());
        player.sendMessage(messages.get("ignore.added", "player", target.getName()));
        return true;
    }

    private void list(Player player, ChatPlayer chatPlayer) {
        Messages messages = plugin.getMessages();
        if (chatPlayer.getIgnoredPlayers().isEmpty()) {
            player.sendMessage(messages.get("ignore.empty"));
            return;
        }
        List<String> names = new ArrayList<>();
        for (UUID id : chatPlayer.getIgnoredPlayers()) {
            OfflinePlayer ignored = Bukkit.getOfflinePlayer(id);
            names.add(ignored.getName() != null ? ignored.getName() : id.toString());
        }
        player.sendMessage(messages.get("ignore.list", "players", String.join(", ", names)));
    }
}
