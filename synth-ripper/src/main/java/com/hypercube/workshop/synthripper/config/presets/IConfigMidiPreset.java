package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;

import java.io.File;

public interface IConfigMidiPreset {
    MidiPreset forgeMidiPreset(File configFile, MidiSettings midiSettings);

    String getTitle();
}
