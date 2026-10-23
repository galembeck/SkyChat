package net.skyminigames.chat.chat;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.skyminigames.chat.SkyChat;
import net.skyminigames.chat.api.SkyChatMessageEvent;
import net.skyminigames.chat.channel.Channel;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.mute.MuteManager;
import net.skyminigames.chat.player.ChatPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything a message goes through. Always called on the main thread.
 */
public final class ChatService {

    private final SkyChat plugin;

    public ChatService(SkyChat plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ entry points

    /** A plain chat line: goes to the "/tell" conversation if locked, otherwise to the focused channel. */
    public void handlePlainChat(Player player, String message) {
        ChatPlayer chatPlayer = plugin.getPlayers().get(player);
        if (chatPlayer.getTellLock() != null) {
            Player target = Bukkit.getPlayer(chatPlayer.getTellLock());
            if (target != null) {
                sendPrivate(player, target, message);
                return;
            }
            chatPlayer.setTellLock(null);
        }
        Channel channel = focusedChannel(chatPlayer);
        if (channel != null) {
            sendToChannel(player, channel, message);
        }
    }

    public Channel focusedChannel(ChatPlayer chatPlayer) {
        Channel focused = plugin.getChannels().get(chatPlayer.getFocusChannel());
        if (focused != null) {
            return focused;
        }
        Channel fallback = plugin.getChannels().get(plugin.getChatConfig().getDefaultChannel());
        if (fallback != null) {
            return fallback;
        }
        return plugin.getChannels().getChannels().stream().findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ channels

    public void sendToChannel(Player sender, Channel channel, String rawMessage) {
        Messages messages = plugin.getMessages();
        ChatPlayer chatPlayer = plugin.getPlayers().get(sender);
        clearAfk(sender, chatPlayer);

        if (!sender.hasPermission(channel.chatPermission())) {
            sender.sendMessage(messages.get("channel.no-permission", "channel", channel.getName()));
            return;
        }
        if (chatPlayer.getIgnoredChannels().contains(channel.getName())) {
            sender.sendMessage(messages.get("channel.you-ignore", "channel", channel.getName()));
            return;
        }
        if (isBlockedByMute(sender)) {
            return;
        }
        if (!checkDelay(sender, chatPlayer, channel)) {
            return;
        }

        String message = process(sender, rawMessage);
        if (ChatColor.stripColor(message).trim().isEmpty()) {
            return;
        }
        if (!charge(sender, channel)) {
            return;
        }

        Set<Player> recipients = recipients(sender, channel);
        SkyChatMessageEvent event = new SkyChatMessageEvent(sender, channel, message, recipients);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        chatPlayer.getLastMessage().put(channel.getName(), System.currentTimeMillis());
        String line = TagFormatter.format(channel.getFormat(), tags(sender, channel), event.getMessage());
        boolean spacing = plugin.getChatConfig().isSpacing() && sender.hasPermission("skychat.spacing");
        for (Player recipient : event.getRecipients()) {
            if (spacing) {
                recipient.sendMessage("");
                recipient.sendMessage(line);
                recipient.sendMessage("");
            } else {
                recipient.sendMessage(line);
            }
        }

        if (channel.isLocal() && plugin.getChatConfig().isShowNoOneHearsYou() && onlySender(event.getRecipients(), sender)) {
            sender.sendMessage(messages.get("channel.no-one-hears"));
        }
        spy(sender, chatPlayer, event.getRecipients(), messages.raw("spy.channel", "msg", ChatColor.stripColor(line)));
        if (plugin.getChatConfig().isLogToConsole()) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    /** Who reads a channel message: permission, focus, distance / world, ignored channels and players. */
    private Set<Player> recipients(Player sender, Channel channel) {
        Set<Player> recipients = new LinkedHashSet<>();
        Location origin = sender.getLocation();
        double maxDistanceSquared = channel.getDistance() * channel.getDistance();
        boolean senderUnignorable = sender.hasPermission("skychat.block.ignore");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(channel.chatPermission())) {
                continue;
            }
            ChatPlayer reader = plugin.getPlayers().get(player);
            if (reader.getIgnoredChannels().contains(channel.getName())) {
                continue;
            }
            if (channel.isNeedFocus() && player != sender && focusedChannel(reader) != channel) {
                continue;
            }
            if (!senderUnignorable && reader.getIgnoredPlayers().contains(sender.getUniqueId())) {
                continue;
            }
            boolean sameWorld = player.getWorld().equals(sender.getWorld());
            if (channel.isLocal()) {
                if (!sameWorld || player.getLocation().distanceSquared(origin) > maxDistanceSquared) {
                    continue;
                }
            } else if (!channel.isCrossworlds() && !sameWorld) {
                continue;
            }
            recipients.add(player);
        }
        return recipients;
    }

    private boolean checkDelay(Player sender, ChatPlayer chatPlayer, Channel channel) {
        if (channel.getDelayPerMessage() <= 0 || sender.hasPermission(channel.noDelayPermission())) {
            return true;
        }
        Long last = chatPlayer.getLastMessage().get(channel.getName());
        if (last == null) {
            return true;
        }
        long waitMillis = channel.getDelayPerMessage() * 1000L - (System.currentTimeMillis() - last);
        if (waitMillis <= 0) {
            return true;
        }
        sender.sendMessage(plugin.getMessages().get("channel.delay", "seconds", (waitMillis + 999) / 1000));
        return false;
    }

    /** Charges costPerMessage through Vault's economy, when set and the player has no ".free" permission. */
    private boolean charge(Player sender, Channel channel) {
        Economy economy = plugin.getEconomy();
        double cost = channel.getCostPerMessage();
        if (cost <= 0 || economy == null || sender.hasPermission(channel.freePermission())) {
            return true;
        }
        if (!economy.has(sender, cost)) {
            sender.sendMessage(plugin.getMessages().get("channel.no-money", "cost", economy.format(cost)));
            return false;
        }
        economy.withdrawPlayer(sender, cost);
        if (channel.isShowCostMessage()) {
            sender.sendMessage(plugin.getMessages().get("channel.cost", "cost", economy.format(cost)));
        }
        return true;
    }

    private Map<String, String> tags(Player sender, Channel channel) {
        Map<String, String> tags = new HashMap<>();
        tags.put("name", channel.getName());
        tags.put("nick", channel.getNickname());
        tags.put("color", channel.getColor().toString());
        tags.put("sender", sender.getDisplayName());
        tags.put("plainsender", sender.getName());
        tags.put("world", sender.getWorld().getName());

        Chat chat = plugin.getChat();
        if (chat != null) {
            String group = chat.getPrimaryGroup(sender);
            tags.put("prefix", chat.getPlayerPrefix(sender));
            tags.put("suffix", chat.getPlayerSuffix(sender));
            tags.put("groupprefix", group == null ? "" : chat.getGroupPrefix(sender.getWorld(), group));
            tags.put("groupsuffix", group == null ? "" : chat.getGroupSuffix(sender.getWorld(), group));
        }

        Calendar now = Calendar.getInstance();
        tags.put("time_hour", twoDigits(now.get(Calendar.HOUR_OF_DAY)));
        tags.put("time_min", twoDigits(now.get(Calendar.MINUTE)));
        tags.put("time_sec", twoDigits(now.get(Calendar.SECOND)));
        tags.put("date_day", twoDigits(now.get(Calendar.DAY_OF_MONTH)));
        tags.put("date_month", twoDigits(now.get(Calendar.MONTH) + 1));
        tags.put("date_year", String.valueOf(now.get(Calendar.YEAR)));
        return tags;
    }

    // ------------------------------------------------------------------ private messages

    public void sendPrivate(Player sender, Player target, String rawMessage) {
        Messages messages = plugin.getMessages();
        ChatPlayer from = plugin.getPlayers().get(sender);
        ChatPlayer to = plugin.getPlayers().get(target);
        clearAfk(sender, from);

        if (sender.equals(target)) {
            sender.sendMessage(messages.get("private.self"));
            return;
        }
        if (plugin.getChatConfig().isBlockPrivateWhenMuted() && isBlockedByMute(sender)) {
            return;
        }
        if (to.getIgnoredPlayers().contains(sender.getUniqueId()) && !sender.hasPermission("skychat.block.ignore")) {
            sender.sendMessage(messages.get("private.ignored"));
            return;
        }

        String message = process(sender, rawMessage);
        if (ChatColor.stripColor(message).trim().isEmpty()) {
            return;
        }

        sender.sendMessage(messages.raw("private.send", "receiver", target.getName(), "sender", sender.getName()).replace("{msg}", message));
        target.sendMessage(messages.raw("private.receive", "receiver", target.getName(), "sender", sender.getName()).replace("{msg}", message));
        from.setReplyTarget(target.getUniqueId());
        to.setReplyTarget(sender.getUniqueId());

        if (to.isAfk()) {
            sender.sendMessage(messages.get("private.afk", "player", target.getName(), "motive", motive(to)));
        }

        String spyLine = messages.raw("spy.private", "sender", sender.getName(), "receiver", target.getName())
                .replace("{msg}", ChatColor.stripColor(message));
        Set<Player> involved = new LinkedHashSet<>();
        involved.add(sender);
        involved.add(target);
        spy(sender, from, involved, spyLine);
        if (plugin.getChatConfig().isLogToConsole()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.stripColor(spyLine));
        }
    }

    public Player replyTarget(Player player) {
        UUID target = plugin.getPlayers().get(player).getReplyTarget();
        return target == null ? null : Bukkit.getPlayer(target);
    }

    // ------------------------------------------------------------------ shared helpers

    /** Colour codes the player may use, then the censor. */
    public String process(Player sender, String rawMessage) {
        String message = rawMessage;
        if (plugin.getChatConfig().isCensorEnabled()) {
            message = plugin.getCensor().apply(message);
        }
        return ColorFilter.apply(message, sender::hasPermission);
    }

    /** Sends the right "you are muted" message and returns true when the player may not talk. */
    public boolean isBlockedByMute(Player sender) {
        Messages messages = plugin.getMessages();
        MuteManager mutes = plugin.getMutes();
        if (mutes.isMuted(sender.getUniqueId())) {
            long minutes = mutes.minutesLeft(sender.getUniqueId());
            sender.sendMessage(minutes < 0 ? messages.get("mute.you-are-muted") : messages.get("mute.you-are-muted-time", "minutes", minutes));
            return true;
        }
        if (mutes.isAllMuted() && !sender.hasPermission("skychat.admin.muteall")) {
            sender.sendMessage(messages.get("mute.all-muted"));
            return true;
        }
        return false;
    }

    public void clearAfk(Player player, ChatPlayer chatPlayer) {
        if (chatPlayer.isAfk()) {
            chatPlayer.setAfkMotive(null);
            player.sendMessage(plugin.getMessages().get("afk.off"));
        }
    }

    public String motive(ChatPlayer chatPlayer) {
        String motive = chatPlayer.getAfkMotive();
        return motive == null || motive.isEmpty() ? "" : plugin.getMessages().raw("afk.motive", "motive", motive);
    }

    /** Spies see what they didn't already receive; senders with /skychat hide stay invisible. */
    private void spy(Player sender, ChatPlayer senderData, Set<Player> alreadyReceived, String line) {
        if (senderData.isHidden() && sender.hasPermission("skychat.admin.hide")) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == sender || alreadyReceived.contains(player)) {
                continue;
            }
            ChatPlayer spy = plugin.getPlayers().get(player);
            if (spy.isSpy() && player.hasPermission("skychat.admin.spy")) {
                player.sendMessage(line);
            }
        }
    }

    private static boolean onlySender(Set<Player> recipients, Player sender) {
        for (Player player : recipients) {
            if (player != sender) {
                return false;
            }
        }
        return true;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
