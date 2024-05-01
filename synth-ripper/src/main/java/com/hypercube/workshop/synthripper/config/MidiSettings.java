package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Setter
@Getter
public class MidiSettings {
    private float maxNoteDurationSec;
    private float maxNoteReleaseDurationSec;
    private int lowestCC;
    private int highestCC;
    private String lowestNote;
    private String highestNote;
    private int notesPerOctave;
    private List<String> presets;
    private Map<Integer, String> filenames = new HashMap<>();

    public void buildFilesNames() {
        presets.forEach(p -> {
            Matcher matcher = CachedRegExp.get("([0-9]+)\\s+(.*)", p);
            if (matcher.find()) {
                int cc = Integer.parseInt(matcher.group(1)) - 1;
                String filename = matcher.group(2);
                filenames.put(cc, filename);
            }
        });
    }

    public String getFilename(int cc) {
        return filenames.get(cc);
    }

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    private int getNoteNumber(String note) {
        Matcher m = CachedRegExp.get("([A-G#])([0-9])", note);
        if (m.find()) {
            int octave = Integer.parseInt(m.group(2)) + 1;
            String offset = m.group(1);
            int intOffset = offset.charAt(0) - 'C';
            if (offset.length() == 2) {
                intOffset++;// #
            }
            if (intOffset < 0) {
                intOffset += 9;
            }
            return octave * 12 + intOffset;
        }
        throw new MidiError("Unexpected note name:" + note);
    }

}
