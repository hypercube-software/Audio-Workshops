package com.hypercube.workshop.midiworkshop.common.seq;

import java.util.List;

/**
 * Do the calculations given how the MIDI file specification represent a key signature
 *
 * @param alterations
 * @param major
 * @see <a href="https://en.wikipedia.org/wiki/Key_signature"></a>
 */
public record KeySignature(int alterations, boolean major) {
    private static final List<String> majorSharpScales = List.of("C", "G", "D", "A", "E", "B", "F#", "C#");
    private static final List<String> majorFlatScales = List.of("C", "F", "Bb", "Eb", "Ab", "Db", "Gb", "Cb");

    private static final List<String> minorSharpScales = List.of("A", "E", "B", "F#", "C#", "G#", "D#", "A#");
    private static final List<String> minorFlatScales = List.of("A", "D", "G", "C", "F", "Bb", "Eb", "Ab");

    @Override
    public String toString() {
        if (major && alterations <= 0) {
            return majorFlatScales.get(Math.abs(alterations)) + " Major";
        } else if (major && alterations > 0) {
            return majorSharpScales.get(Math.abs(alterations)) + " Major";
        } else if (!major && alterations <= 0) {
            return minorFlatScales.get(Math.abs(alterations)) + " Minor";
        } else if (!major && alterations > 0) {
            return minorSharpScales.get(Math.abs(alterations)) + " Minor";
        }
        return "";
    }
}
