package com.hypercube.workshop.midiworkshop.api;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public record MidiNote(int value, int octave, String name) {
    private static final List<String> sharps = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    private static final List<String> flats = List.of("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B");

    public static MidiNote fromName(String name) {
        Pattern pattern = Pattern.compile("([CDEFGAB][#b]?)([-0123456789])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            String baseName = matcher.group(1);
            int shift = Math.max(sharps.indexOf(baseName), flats.indexOf(baseName));
            int octave = Integer.parseInt(matcher.group(2));
            int value = (octave + 2) * 12 + shift;
            return new MidiNote(value, octave, name);
        } else {
            throw new IllegalArgumentException("Illegal note name:" + name);
        }
    }

    public static MidiNote fromValue(int value) {
        int octave = (value / 12) - 2;
        int shift = value % 12;
        String name = sharps.get(shift) + octave;
        return new MidiNote(value, octave, name);
    }
}
