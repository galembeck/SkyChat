package net.skyminigames.chat.chat;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatTextTest {

    private static String colors(String message, String... permissions) {
        Set<String> perms = new HashSet<>(Arrays.asList(permissions));
        return ColorFilter.apply(message, perms::contains);
    }

    @Test
    void colorsNeedPermission() {
        assertEquals("&aoi &lforte", colors("&aoi &lforte"));
        assertEquals("§aoi &lforte", colors("&aoi &lforte", "skychat.color.allcolors"));
        assertEquals("§aoi §lforte", colors("&aoi &lforte", "skychat.color.allcolors", "skychat.color.allformats"));
    }

    @Test
    void singleColourPermissions() {
        assertEquals("§coi &aok", colors("&coi &aok", "skychat.color.red"));
        assertEquals("§loi &kx", colors("&loi &kx", "skychat.color.bold"));
        assertEquals("§1azul", colors("&1azul", "skychat.color.darkblue"));
    }

    @Test
    void rawColourSymbolIsNeutralised() {
        assertEquals("&ctexto", colors("§ctexto"));
    }

    @Test
    void censorReplacesCaseInsensitive() {
        Censor censor = new Censor(Arrays.asList("feio", "bobo;legal", " ", ""));
        assertEquals("que **** e legal", censor.apply("que FEIO e bobo"));
        assertEquals("sem nada", new Censor(Collections.<String>emptyList()).apply("sem nada"));
        assertEquals("sem nada", new Censor(null).apply("sem nada"));
    }

    @Test
    void formatFillsTagsAndColoursButNotTheMessage() {
        Map<String, String> tags = new HashMap<>();
        tags.put("prefix", "&6[Master] ");
        tags.put("plainsender", "ImLighTzzZ_");
        String line = TagFormatter.format("&e[L] {prefix}{plainsender}&e: {msg}", tags, "{prefix} &chack");
        assertEquals("§e[L] §6[Master] ImLighTzzZ_§e: {prefix} &chack", line);
    }

    @Test
    void unknownTagsStayAndMissingValuesBecomeEmpty() {
        Map<String, String> tags = new HashMap<>();
        tags.put("suffix", null);
        assertEquals("a{nope}b oi", TagFormatter.format("a{nope}{suffix}b {msg}", tags, "oi"));
    }
}
