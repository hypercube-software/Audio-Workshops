package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.audioworkshop.common.utils.CachedRegExp;
import com.hypercube.workshop.midiworkshop.common.MidiNote;
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
    private int velocityPerNote;
    private int presetOffset;
    private List<String> presets;
    private Map<Integer, String> filenames = new HashMap<>();

    public void buildFilesNames() {
        presets.forEach(p -> {
            Matcher matcher = CachedRegExp.get("([0-9]+)\\s+(.*)", p);
            if (matcher.find()) {
                int cc = Integer.parseInt(matcher.group(1)) - presetOffset;
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
        return MidiNote.fromName(note)
                .value();
    }

    public int getLowestCC() {
        return lowestCC - presetOffset;
    }

    public int getHighestCC() {
        return highestCC - presetOffset;
    }
}
