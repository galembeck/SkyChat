package net.skyminigames.chat.channel;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelTest {

    @Test
    void readsLegendchatStyleFile() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("name: Staff\nnickname: SC\nformat: '&b[Staff] {msg}'\ncolor: AQUA\nshortcutAllowed: true\n"
                + "needFocus: false\ndistance: 0.0\ncrossworlds: true\ndelayPerMessage: 2\ncostPerMessage: 1.5\nshowCostMessage: false\n");
        Channel channel = Channel.fromConfig(yaml);

        assertEquals("staff", channel.getName());
        assertEquals("sc", channel.getNickname());
        assertEquals(ChatColor.AQUA, channel.getColor());
        assertFalse(channel.isLocal());
        assertEquals(2, channel.getDelayPerMessage());
        assertEquals(1.5, channel.getCostPerMessage());
        assertEquals("skychat.channel.staff.chat", channel.chatPermission());
    }

    @Test
    void colourNamesFromBukkitLegendchatOrCodes() {
        assertEquals(ChatColor.DARK_BLUE, Channel.parseColor("DARKBLUE"));
        assertEquals(ChatColor.DARK_BLUE, Channel.parseColor("dark_blue"));
        assertEquals(ChatColor.DARK_GREEN, Channel.parseColor("DARGREEN"));
        assertEquals(ChatColor.LIGHT_PURPLE, Channel.parseColor("LIGHTPURPLE"));
        assertEquals(ChatColor.YELLOW, Channel.parseColor("&e"));
        assertEquals(ChatColor.WHITE, Channel.parseColor("not-a-colour"));
        assertEquals(ChatColor.WHITE, Channel.parseColor("BOLD"));
    }

    @Test
    void localChannelHasDistance() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", "local");
        yaml.set("distance", 60.0);
        assertTrue(Channel.fromConfig(yaml).isLocal());
        assertEquals("local", Channel.fromConfig(yaml).getNickname());
    }
}
