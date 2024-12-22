package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.MidiSettings;

public interface IConfigMidiPreset {
    MidiPreset forgeMidiPreset(MidiSettings midiSettings);

    String getTitle();
}
