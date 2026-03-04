package com.hypercube.workshop.synthripper.model.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hypercube.workshop.midiworkshop.api.errors.MidiConfigError;
import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;

@Setter
@Getter
public class SynthRipperConfiguration {
    private static final List<String> notes = List.of("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B");
    @JsonIgnore
    private MidiDeviceLibrary midiDeviceLibrary;
    private String projectName;
    private String device;
    private DevicesSettings ports;
    private MidiSettings midi;
    private AudioSettings audio;
    private File configFile; // loaded config file corresponding to this class

    public static String noteNameFromPitch(int input) {
        int octave = (input - 12) / 12;
        int offset = input % 12;
        return notes.get(offset) + octave;
    }


    public String getOutputDir() {
        return "output/" + projectName;
    }

    public List<MidiPreset> getSelectedPresets() {
        return midi.getSelectedPresets(this);
    }

    public MidiDeviceDefinition getDevice() {
        return midiDeviceLibrary != null ? midiDeviceLibrary.getDevice(device)
                .orElseThrow(() -> new MidiConfigError("Device '%s' not defined in the library".formatted(device))) : null;
    }
}
