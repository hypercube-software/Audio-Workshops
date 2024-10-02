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
    /**
     * Max Duration of NoteOn message before sending Note Off
     */
    private float maxNoteDurationSec;
    /**
     * Max duration after Note Off before stopping recording
     */
    private float maxNoteReleaseDurationSec;
    /**
     * real MIDI cc = displayed cc - presetOffset (it is usually 1)
     */
    private int presetOffset;
    /**
     * Lower bound (included) for displayed CC
     */
    private int lowestCC;
    /**
     * Upper bound (included) for displayed CC
     */
    private int highestCC;
    /**
     * lower bound (included)
     */
    private String lowestNote;
    /**
     * Upper bound (included)
     */
    private String highestNote;
    /**
     * How many notes to capture per octave (inside lower and upper bounds)
     */
    private int notesPerOctave;
    /**
     * How many velocities to record (max is 127)
     */
    private int velocityPerNote;
    /**
     * Presets names for each CC value
     */
    private List<String> presets;
    /**
     * Filename for a given CC value based on presets lists
     */
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

    public int getDisplayedCC(int cc) {
        return cc + presetOffset;
    }
}
