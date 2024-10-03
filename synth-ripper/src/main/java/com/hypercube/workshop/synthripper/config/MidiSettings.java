package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.IntStream;

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
     * How preset numbers must be parsed
     */
    private MidiPresetFormat presetFormat;
    /**
     * Lower bound (included)
     */
    private String lowestPreset;
    /**
     * Upper bound (included)
     */
    private String highestPreset;
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
     * Presets to record
     */
    private List<MidiPreset> selectedPresets;

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    public List<MidiPreset> getSelectedPresets() {
        if (selectedPresets == null) {
            String lowestPresetPrefix = lowestPreset + " ";
            String highestPresetPrefix = highestPreset + " ";
            int startIdx = IntStream.range(0, presets.size())
                    .filter(idx -> presets.get(idx)
                            .startsWith(lowestPresetPrefix))
                    .findFirst()
                    .orElseThrow(() -> new MidiError("Lowest preset not found:" + lowestPreset));
            int endIdx = IntStream.range(0, presets.size())
                    .filter(idx -> presets.get(idx)
                            .startsWith(highestPresetPrefix))
                    .findFirst()
                    .orElseThrow(() -> new MidiError("Highest preset not found:" + highestPreset));
            selectedPresets = IntStream.rangeClosed(startIdx, endIdx)
                    .boxed()
                    .map(idx -> {
                        String definition = presets.get(idx);
                        MidiPreset preset = MidiPreset.fromString(presetFormat, definition);
                        if (preset.title() == null) {
                            throw new MidiError("Preset without name: " + definition);
                        }
                        return preset;
                    })
                    .toList();
        }
        return selectedPresets;
    }

    private int getNoteNumber(String note) {
        return MidiNote.fromName(note)
                .value();
    }
}
