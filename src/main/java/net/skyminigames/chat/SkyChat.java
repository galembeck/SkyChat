package net.skyminigames.chat;

import lombok.Getter;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.skyminigames.chat.channel.ChannelManager;
import net.skyminigames.chat.chat.Censor;
import net.skyminigames.chat.chat.ChatService;
import net.skyminigames.chat.command.AdminCommand;
import net.skyminigames.chat.command.AfkCommand;
import net.skyminigames.chat.command.ChannelCommand;
import net.skyminigames.chat.command.IgnoreCommand;
import net.skyminigames.chat.command.ReplyCommand;
import net.skyminigames.chat.command.TellCommand;
import net.skyminigames.chat.config.ChatConfig;
import net.skyminigames.chat.listener.ChatListener;
import net.skyminigames.chat.message.Messages;
import net.skyminigames.chat.mute.MuteManager;
import net.skyminigames.chat.player.PlayerManager;
import net.skyminigames.chat.util.StartupBanner;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class SkyChat extends JavaPlugin {

    private ChatConfig chatConfig;
    private Messages messages;
    private ChannelManager channels;
    private Censor censor;
    private PlayerManager players;
    private MuteManager mutes;
    private ChatService chatService;
    /** Prefixes / suffixes from PermissionsEx through Vault; {@code null} if unavailable. */
    private Chat chat;
    /** For costPerMessage; {@code null} if no economy plugin. */
    private Economy economy;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        saveDefaultConfig();
        messages = new Messages(this);
        channels = new ChannelManager(this);
        loadSettings();

        chat = service(Chat.class);
        economy = service(Economy.class);
        if (chat == null) {
            getLogger().warning("Vault has no chat provider: {prefix} and {suffix} will be empty.");
        }

        players = new PlayerManager(this);
        players.load();
        mutes = new MuteManager(this);
        mutes.load();
        chatService = new ChatService(this);

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("tell").setExecutor(new TellCommand(this));
        getCommand("reply").setExecutor(new ReplyCommand(this));
        getCommand("ignore").setExecutor(new IgnoreCommand(this));
        getCommand("afk").setExecutor(new AfkCommand(this));
        getCommand("skychat").setExecutor(new AdminCommand(this));

        // After /reload, players already online
        for (Player player : getServer().getOnlinePlayers()) {
            players.get(player);
        }

        StartupBanner.printEnable(this, System.currentTimeMillis() - start);
    }

    @Override
    public void onDisable() {
        if (players != null) {
            players.saveAll();
        }
        StartupBanner.printDisable(this);
    }

    /** /skychat reload: config.yml, messages.yml and the channel files. */
    public void reload() {
        reloadConfig();
        loadSettings();
    }

    private void loadSettings() {
        chatConfig = new ChatConfig(getConfig());
        messages.reload();
        channels.reload();
        censor = new Censor(chatConfig.getCensoredWords());
        if (channels.get(chatConfig.getDefaultChannel()) == null) {
            getLogger().warning("default-channel '" + chatConfig.getDefaultChannel() + "' doesn't exist; using the first channel.");
        }
    }

    private <T> T service(Class<T> type) {
        RegisteredServiceProvider<T> provider = getServer().getServicesManager().getRegistration(type);
        return provider == null ? null : provider.getProvider();
    }
}
