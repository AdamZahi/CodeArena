package com.codearena.module8_terminalquest.tts;

import java.util.List;

/**
 * Automatic speaker rotation for story chapters.
 * Chapter speakers cycle through the roster using (orderIndex - 1) % roster size.
 * Boss missions always use a dedicated override speaker.
 */
public final class SpeakerRotation {

    public record Speaker(String name, String voice, String role) {}

    private static final List<Speaker> ROSTER = List.of(
            new Speaker("Sarah Chen",   "Aoede", "DevOps Lead — NexaTech"),
            new Speaker("Lina Torres",  "Kore",  "Cloud Architect — NexaTech"),
            new Speaker("Alex Rivera",  "Puck",  "SRE Engineer — NexaTech"),
            new Speaker("Nadia Park",   "Aoede", "Security Analyst — NexaTech"),
            new Speaker("Karim Osman",  "Puck",  "Platform Engineer — NexaTech")
    );

    private static final Speaker BOSS_SPEAKER =
            new Speaker("Le Directeur", "Puck", "CTO — NexaTech");

    private SpeakerRotation() {}

    /** Returns the chapter speaker based on orderIndex (1-based, wraps every 5). */
    public static Speaker getSpeakerForChapter(int orderIndex) {
        int idx = (orderIndex - 1) % ROSTER.size();
        return ROSTER.get(idx);
    }

    /** Returns the fixed boss-fight speaker used for all boss missions. */
    public static Speaker getBossSpeaker() {
        return BOSS_SPEAKER;
    }
}
