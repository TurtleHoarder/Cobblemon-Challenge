package com.turtlehoarder.cobblemonchallenge.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Moves the config file into the mod's own directory, once.
 *
 * The mod used to write straight into the config root. Now that it owns a
 * directory for its language files, the config belongs next to them. Without
 * this, an existing server would silently start from defaults after the update:
 * the mod would find no file at the new path and write a fresh one, and every
 * setting the operator had chosen would be quietly lost.
 */
public final class ConfigMigration {

    private ConfigMigration() {
    }

    /**
     * @param configDirectory the platform's config directory
     * @param oldName         file name the mod used to write, relative to it
     * @param newName         where it should live now, relative to it
     */
    public static void moveIntoModDirectory(Path configDirectory, String oldName, String newName) {
        Path old = configDirectory.resolve(oldName);
        Path target = configDirectory.resolve(newName);

        if (!Files.exists(old)) {
            return;
        }
        // A file already at the new path wins. Someone who has run the new
        // version and then dropped the old file back in should not have their
        // current settings overwritten by a stale copy.
        if (Files.exists(target)) {
            CobblemonChallenge.LOGGER.warn(
                    "Both {} and {} exist. Using the newer location and leaving the old file alone.",
                    old, target);
            return;
        }
        try {
            Files.createDirectories(target.getParent());
            Files.move(old, target, StandardCopyOption.ATOMIC_MOVE);
            CobblemonChallenge.LOGGER.info("Moved config from {} to {}", old, target);
        } catch (IOException e) {
            // Not fatal: the mod will write a fresh file at the new path. The
            // settings are lost, so this has to be loud rather than swallowed.
            CobblemonChallenge.LOGGER.error(
                    "Could not move {} to {}. The old settings were NOT carried over.",
                    old, target, e);
        }
    }
}
