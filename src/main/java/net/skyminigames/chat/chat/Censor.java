package net.skyminigames.chat.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces censored words (case-insensitive). Entries: "word" (replaced with stars of the same length)
 * or "word;replacement".
 */
public final class Censor {

    private final List<Pattern> patterns = new ArrayList<>();
    private final List<String> replacements = new ArrayList<>();

    public Censor(List<String> entries) {
        for (String entry : entries == null ? Collections.<String>emptyList() : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            int split = entry.indexOf(';');
            String word = (split >= 0 ? entry.substring(0, split) : entry).trim();
            if (word.isEmpty()) {
                continue;
            }
            patterns.add(Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            replacements.add(split >= 0 ? entry.substring(split + 1) : null);
        }
    }

    public String apply(String message) {
        String result = message;
        for (int i = 0; i < patterns.size(); i++) {
            Matcher matcher = patterns.get(i).matcher(result);
            StringBuffer out = new StringBuffer();
            while (matcher.find()) {
                String replacement = replacements.get(i) != null ? replacements.get(i) : stars(matcher.group().length());
                matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(out);
            result = out.toString();
        }
        return result;
    }

    private static String stars(int length) {
        StringBuilder stars = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stars.append('*');
        }
        return stars.toString();
    }
}
