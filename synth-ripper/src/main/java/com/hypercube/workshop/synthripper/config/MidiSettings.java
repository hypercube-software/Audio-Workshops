package com.hypercube.workshop.synthripper.config;

import com.hypercube.workshop.midiworkshop.common.MidiNote;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.sysex.macro.CommandMacro;
import com.hypercube.workshop.synthripper.config.yaml.IConfigMidiPreset;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.IntStream;

@Setter
@Getter
public class MidiSettings {
    public static final int DEFAULT_MIDI_CHANNEL = 1;
    public static final int USE_DEFAULT_MIDI_CHANNEL = -1;
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
     * Which kinf of bankName select must be used
     */
    private MidiBankFormat presetFormat;

    /**
     * Defautl MIDI channel to use in the range [1-16] not [0-15]
     */
    private int channel = DEFAULT_MIDI_CHANNEL;
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
     * How many cc to record (max is 127)
     */
    private int ccPerNote;
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
    private List<CommandMacro> commands;

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    public List<MidiPreset> getSelectedPresets(SynthRipperConfiguration synthRipperConfiguration) {
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
                            .forgeMidiPreset(synthRipperConfiguration.getConfigFile(), this))
                    .toList();
        }
        return selectedPresets;
    }

    private int getNoteNumber(String note) {
        return MidiNote.fromName(note)
                .value();
    }

    public int getZeroBasedChannel() {
        return channel - 1;
    }
}
