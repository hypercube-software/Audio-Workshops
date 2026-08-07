package com.hypercube.workshop.synthripper.model.config;

import com.hypercube.workshop.midiworkshop.api.MidiNote;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.presets.DrumKitNote;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetBuilder;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPresetIdentity;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceBank;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDevicePreset;
import com.hypercube.workshop.synthripper.preset.PresetGenerator;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Setter
@Getter
public class MidiSettings {
    public static final int DEFAULT_MIDI_CHANNEL = 1;
    public static final int USE_DEFAULT_MIDI_CHANNEL = -1;
    /**
     * Output formats for presets, should match {@link PresetGenerator#getAlias()}
     */
    private List<String> outputFormats;
    /**
     * Max Duration of NoteOn message before sending Note Off
     */
    private float maxNoteDurationSec;
    /**
     * Max duration after Note Off before stopping recording
     */
    private float maxNoteReleaseDurationSec;
    /**
     * Default MIDI channel to use in the range [1-16] not [0-15]
     */
    private int channel = DEFAULT_MIDI_CHANNEL;
    /**
     * Lower bound (included) - preset name from the device library
     */
    private String lowestPreset;
    /**
     * Upper bound (included) - preset name from the device library
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
     * Presets to record
     */
    private List<MidiPreset> selectedPresets;

    public int getLowestNoteInt() {
        return getNoteNumber(lowestNote);
    }

    public int getHighestNoteInt() {
        return getNoteNumber(highestNote);
    }

    public List<MidiPreset> getSelectedPresets(SynthRipperConfiguration config) {
        if (selectedPresets == null) {
            var device = config.getDevice();
            var flatPresets = new ArrayList<FlatPreset>();

            for (MidiDeviceMode mode : device.getDeviceModes().values()) {
                for (MidiDeviceBank bank : mode.getBanks().values()) {
                    for (MidiDevicePreset devicePreset : bank.getPresets()) {
                        flatPresets.add(new FlatPreset(mode, bank, devicePreset));
                    }
                }
            }

            int startIdx = findPresetIndex(flatPresets, lowestPreset, "Lowest preset not found: " + lowestPreset);
            int endIdx = findPresetIndex(flatPresets, highestPreset, "Highest preset not found: " + highestPreset);

            selectedPresets = new ArrayList<>();
            for (int i = startIdx; i <= endIdx; i++) {
                FlatPreset fp = flatPresets.get(i);
                int program = extractProgram(fp.devicePreset);
                MidiPreset midiPreset = MidiPresetBuilder.parse(device, fp.mode, fp.bank, program);
                midiPreset.setId(new MidiPresetIdentity(fp.mode.getName(), fp.bank.getName(), fp.devicePreset.name(), fp.devicePreset.category()));
                fp.devicePreset.drumMap()
                        .forEach(entry -> midiPreset.getDrumKitNotes()
                                .add(parseDrumKitNote(entry)));
                selectedPresets.add(midiPreset);
            }
        }
        return selectedPresets;
    }

    private int findPresetIndex(List<FlatPreset> flatPresets, String presetName, String errorMsg) {
        for (int i = 0; i < flatPresets.size(); i++) {
            if (flatPresets.get(i).devicePreset.name().equals(presetName)) {
                return i;
            }
        }
        throw new MidiError(errorMsg);
    }

    /**
     * Extract the program number from a {@link MidiDevicePreset} command.
     * The command is a hex string like "00AB" where the last byte is the program number.
     */
    private int extractProgram(MidiDevicePreset devicePreset) {
        String cmd = devicePreset.command();
        if (cmd == null || cmd.length() < 2) {
            return 0;
        }
        return Integer.parseInt(cmd.substring(cmd.length() - 2), 16);
    }

    /**
     * Copy a drumMap entry of the device library (formatted like "1B | Insects")
     * into a {@link DrumKitNote}. The note is given as a hexadecimal MIDI note and
     * it is of the network drum kit.
     */
    private DrumKitNote parseDrumKitNote(String entry) {
        String[] parts = entry.split("\\|", 2);
        if (parts.length != 2) {
            throw new MidiError("Invalid drumMap entry: " + entry);
        }
        int note = Integer.parseInt(parts[0].trim(), 16);
        String title = parts[1].trim();
        return new DrumKitNote(title, note);
    }

    public int getZeroBasedChannel() {
        return channel - 1;
    }

    private int getNoteNumber(String note) {
        return MidiNote.fromName(note)
                .value();
    }

    private record FlatPreset(MidiDeviceMode mode, MidiDeviceBank bank, MidiDevicePreset devicePreset) {
    }
}
