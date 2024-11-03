package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;
import com.hypercube.workshop.synthripper.config.presets.IConfigMidiPreset;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Setter
@Getter
public class MidiSettings {
    /**
     * Output format for presets, shound match {@link PresetGenerator#getAlias()}
     */
    private String outputFormat;
    /**
     * Max Duration of NoteOn message before sending Note Off
     */
    private float maxNoteDurationSec;
    /**
     * Max duration after Note Off before stopping recording
     */
    private float maxNoteReleaseDurationSec;
    /**
     * Which kinf of bank select must be used
     */
    private MidiBankFormat presetFormat;
    /**
     * Preset numbers start from 0 or 1 ?
     */
    private MidiPresetNumbering presetNumbering;
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
     * Presets definitions
     */
    private List<IConfigMidiPreset> presets;
    /**
     * Presets to record
     */
    private List<MidiPreset> selectedPresets;
    /**
     * Command templates
     */
    private Map<String, String> commands;

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    public List<MidiPreset> getSelectedPresets() {
        if (selectedPresets == null) {
            int startIdx = IntStream.range(0, presets.size())
                    .filter(idx -> presets.get(idx)
                            .getTitle()
                            .equals(lowestPreset))
                    .findFirst()
                    .orElseThrow(() -> new MidiError("Lowest preset not found:" + lowestPreset));
            int endIdx = IntStream.range(0, presets.size())
                    .filter(idx -> presets.get(idx)
                            .getTitle()
                            .equals(highestPreset))
                    .findFirst()
                    .orElseThrow(() -> new MidiError("Highest preset not found:" + highestPreset));
            selectedPresets = IntStream.rangeClosed(startIdx, endIdx)
                    .boxed()
                    .map(idx -> presets.get(idx)
                            .forgeMidiPreset(presetFormat, presetNumbering))
                    .toList();
        }
        return selectedPresets;
    }

    private int getNoteNumber(String note) {
        return MidiNote.fromName(note)
                .value();
    }
}
