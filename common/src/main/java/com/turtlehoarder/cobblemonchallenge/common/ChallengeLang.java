package com.turtlehoarder.cobblemonchallenge.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * The mod's messages, resolved on the server.
 *
 * Deliberately not {@code Component.translatable}: this mod runs on servers whose
 * players use a vanilla client, and a translation key is resolved by the CLIENT.
 * Anyone without the mod installed would read {@code cobblemonchallenge.error.cooldown}
 * instead of a sentence. Resolving here means every player sees the same text, in
 * the language the server chose, whatever client they run.
 *
 * Every message carries its own colours as section codes, and the shared chat
 * prefix as the token {@code <prefix>}. Nothing about how a message looks is
 * decided in Java: an operator restyles the whole mod from the files, and the
 * prefix is written once, under the key {@code cobblemonchallenge.prefix}.
 *
 * A file in {@code lang/} is an OVERLAY, not a replacement: the bundled file is
 * read first and the config file is laid on top of it, key by key. So a server
 * writes only what it changes — often just the prefix — and still picks up every
 * message a later version of the mod adds.
 *
 * That is also why the defaults are NOT copied into {@code lang/} itself. A full
 * copy sitting there would shadow the bundled file forever, and an update's new
 * wording would never reach a server that had booted once. They go to
 * {@code lang/defaults/} instead, rewritten on every boot, purely to be read and
 * copied from. Nothing in there is loaded.
 *
 * Lookup order for a key:
 *   1. the configured language: bundled, then overlaid from {@code lang/}
 *   2. en_us: bundled, then overlaid from {@code lang/}
 *   3. the key itself, so a missing entry is visible instead of blank
 */
public final class ChallengeLang {

    /** Shipped with the jar and always present, which is what makes it the fallback. */
    public static final String DEFAULT_LANGUAGE = "en_us";

    /** Languages shipped in the jar, and dumped to lang/defaults/ for reference. */
    private static final String[] BUNDLED = {"en_us", "es_es"};

    /** Holds the chat prefix, so a rebrand is one line rather than one per message. */
    private static final String PREFIX_KEY = "cobblemonchallenge.prefix";

    /** What a message writes where the prefix should go. */
    private static final String PREFIX_TOKEN = "<prefix>";

    private static final Gson GSON = new Gson();

    private static final Map<String, String> STRINGS = new HashMap<>();
    private static final Map<String, String> FALLBACK = new HashMap<>();

    private ChallengeLang() {
    }

    /**
     * @param configDirectory the platform's config directory, e.g. .minecraft/config
     * @param language        the value of the 'language' config option
     */
    public static void load(Path configDirectory, String language) {
        STRINGS.clear();
        FALLBACK.clear();

        // A config file written before this option existed has no 'language'
        // key, and the platform hands back null for it. Treat that as "use the
        // default" rather than looking for a file called "null.json".
        if (language == null || language.isBlank()) {
            language = DEFAULT_LANGUAGE;
        }

        Path langDirectory = configDirectory.resolve(CobblemonChallenge.MODID).resolve("lang");
        writeReferenceDefaults(langDirectory.resolve("defaults"));

        // The fallback is read first and separately: if the chosen language is
        // missing a key, that key still has to resolve to something readable.
        readBundledInto(FALLBACK, DEFAULT_LANGUAGE);
        readInto(FALLBACK, langDirectory.resolve(DEFAULT_LANGUAGE + ".json"));

        if (DEFAULT_LANGUAGE.equals(language)) {
            STRINGS.putAll(FALLBACK);
        } else {
            readBundledInto(STRINGS, language);
            readInto(STRINGS, langDirectory.resolve(language + ".json"));
            if (STRINGS.isEmpty()) {
                CobblemonChallenge.LOGGER.warn(
                        "No language file for '{}', bundled or in {}, falling back to {}",
                        language, langDirectory, DEFAULT_LANGUAGE);
                STRINGS.putAll(FALLBACK);
            }
        }
        resolvePrefix();
        CobblemonChallenge.LOGGER.info("Loaded {} messages for language '{}'", STRINGS.size(), language);
    }

    /**
     * A message, formatted and ready to send.
     *
     * Section signs in the file are honoured, which is how a message carries its
     * own colours. Callers pass arguments and nothing else.
     */
    public static MutableComponent get(String key, Object... args) {
        return Component.literal(raw(key, args));
    }

    /** The formatted text, for the few places that need a String. */
    public static String raw(String key, Object... args) {
        String pattern = STRINGS.get(key);
        if (pattern == null) {
            pattern = FALLBACK.get(key);
        }
        if (pattern == null) {
            // Showing the key beats showing nothing: it says which entry to add.
            return key;
        }
        if (args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (RuntimeException e) {
            // A file edited by hand can easily disagree with the code about how
            // many arguments a message takes. One broken line must not take the
            // command down with it.
            CobblemonChallenge.LOGGER.warn("Bad format for '{}': {}", key, e.getMessage());
            return pattern;
        }
    }

    /**
     * Expands {@code <prefix>} in every message, in both maps.
     *
     * Done once at load rather than per message: this runs on every chat line
     * the mod sends. A language that does not set a prefix of its own borrows
     * the fallback's, so a one-key override file styles the whole mod.
     */
    private static void resolvePrefix() {
        String prefix = STRINGS.get(PREFIX_KEY);
        if (prefix == null) {
            prefix = FALLBACK.getOrDefault(PREFIX_KEY, "");
        }
        expandPrefix(STRINGS, prefix);
        expandPrefix(FALLBACK, prefix);
    }

    private static void expandPrefix(Map<String, String> target, String prefix) {
        for (Map.Entry<String, String> entry : target.entrySet()) {
            String value = entry.getValue();
            if (value.contains(PREFIX_TOKEN)) {
                entry.setValue(value.replace(PREFIX_TOKEN, prefix));
            }
        }
    }

    /**
     * Dumps the shipped languages somewhere an operator can read them.
     *
     * Overwritten on every boot on purpose: these are a printout of what the jar
     * currently contains, not configuration. Editing one changes nothing — the
     * file to edit is {@code lang/<language>.json}, one directory up, and it only
     * needs the keys being changed.
     */
    private static void writeReferenceDefaults(Path defaultsDirectory) {
        try {
            Files.createDirectories(defaultsDirectory);
        } catch (IOException e) {
            CobblemonChallenge.LOGGER.error("Could not create {}", defaultsDirectory, e);
            return;
        }
        for (String language : BUNDLED) {
            Path target = defaultsDirectory.resolve(language + ".json");
            try (InputStream bundled = open(language)) {
                if (bundled == null) {
                    continue;
                }
                Files.copy(bundled, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                CobblemonChallenge.LOGGER.error("Could not write {}", target, e);
            }
        }
    }

    private static void readInto(Map<String, String> target, Path file) {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            parseInto(target, reader);
        } catch (IOException | RuntimeException e) {
            CobblemonChallenge.LOGGER.error("Could not read {}", file, e);
        }
    }

    private static void readBundledInto(Map<String, String> target, String language) {
        try (InputStream bundled = open(language)) {
            if (bundled == null) {
                return;
            }
            parseInto(target, new InputStreamReader(bundled, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            CobblemonChallenge.LOGGER.error("Could not read bundled language {}", language, e);
        }
    }

    private static void parseInto(Map<String, String> target, Reader reader) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            target.put(entry.getKey(), entry.getValue().getAsString());
        }
    }

    private static InputStream open(String language) {
        return ChallengeLang.class.getResourceAsStream(
                "/assets/" + CobblemonChallenge.MODID + "/lang/" + language + ".json");
    }
}
