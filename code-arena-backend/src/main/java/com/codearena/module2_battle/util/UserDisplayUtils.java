package com.codearena.module2_battle.util;

import com.codearena.user.entity.User;
import com.codearena.user.repository.UserRepository;

/**
 * Centralized utility for resolving user display names and avatar URLs.
 * Mirrors the frontend profile display logic:
 *   nickname → firstName → email prefix → auth0Id cleanup
 *
 * The frontend profile page uses:
 *   data.db?.nickname || data.db?.firstName || user.nickname || 'Unknown Hacker'
 * And for avatar:
 *   activeIconId ? dicebear(activeIconId) : (user.picture || dicebear('default'))
 */
public final class UserDisplayUtils {

    private UserDisplayUtils() {}

    /**
     * Resolve a human-readable display name from a User entity.
     * Falls back gracefully — never returns null or "OPERATOR".
     */
    public static String resolveDisplayName(User user) {
        if (user == null) return "Unknown Hacker";

        if (user.getNickname() != null
                && !user.getNickname().isBlank()
                && !looksLikeAuth0Identifier(user.getNickname())) {
            return user.getNickname();
        }
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            if (user.getLastName() != null && !user.getLastName().isBlank()) {
                return user.getFirstName() + " " + user.getLastName();
            }
            return user.getFirstName();
        }
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().split("@")[0];
        }
        // Last resort: derive a readable fallback from auth0Id
        if (user.getAuth0Id() != null && !user.getAuth0Id().isBlank()) {
            return cleanAuth0Id(user.getAuth0Id());
        }
        return "Unknown Hacker";
    }

    public static boolean looksLikeAuth0Identifier(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim().toLowerCase();
        return v.startsWith("auth0|")
                || v.startsWith("google-oauth2|")
                || v.startsWith("github|")
                || v.startsWith("facebook|")
                || v.startsWith("user_");
    }

    /**
     * Resolve display name from an auth0 user ID by looking up the DB.
     */
    public static String resolveDisplayName(String auth0Id, UserRepository userRepository) {
        if (auth0Id == null) return "Unknown Hacker";
        return userRepository.findByAuth0Id(auth0Id)
                .map(UserDisplayUtils::resolveDisplayName)
                .orElse("Unknown Hacker");
    }

    /**
     * Resolve avatar URL for a User entity.
     * Logic mirrors the frontend my-profile.component.html:
     *   activeIconId → dicebear(seed=iconId)
     *   avatarUrl (from Auth0 picture) → use directly
     *   fallback → dicebear(seed=nickname or "default")
     */
    public static String resolveAvatarUrl(User user) {
        if (user == null) return dicebearUrl("default");

        // Custom equipped icon takes priority (same as frontend)
        if (user.getActiveIconId() != null 
                && !user.getActiveIconId().isBlank()
                && !"default_icon".equals(user.getActiveIconId())) {
            return dicebearUrl(user.getActiveIconId().replace("icon_", ""));
        }
        // Next: Auth0 picture URL (Google avatar, etc.)
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            return user.getAvatarUrl();
        }
        // Fallback: dicebear with nickname as seed
        String seed = user.getNickname() != null ? user.getNickname() : "default";
        return dicebearUrl(seed);
    }

    /**
     * Resolve avatar URL from an auth0 user ID by looking up the DB.
     */
    public static String resolveAvatarUrl(String auth0Id, UserRepository userRepository) {
        if (auth0Id == null) return dicebearUrl("default");
        return userRepository.findByAuth0Id(auth0Id)
                .map(UserDisplayUtils::resolveAvatarUrl)
                .orElse(dicebearUrl("default"));
    }

    /**
     * Clean an Auth0 ID into something readable.
     * e.g. "auth0|abc123def456" → "abc123def456"
     * e.g. "google-oauth2|1234567890" → "1234567890"
     */
    private static String cleanAuth0Id(String auth0Id) {
        String cleanId = auth0Id.contains("|") ? auth0Id.split("\\|", 2)[1] : auth0Id;
        // If it's very long (typical of auth0 IDs), take last 8 chars
        if (cleanId.length() > 12) {
            return "user_" + cleanId.substring(cleanId.length() - 8);
        }
        return cleanId;
    }

    private static String dicebearUrl(String seed) {
        return "https://api.dicebear.com/7.x/bottts/svg?seed=" + seed;
    }
}
