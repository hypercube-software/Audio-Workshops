package com.hypercube.workshop.synthripper.config.yaml;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.config.SynthRipperConfiguration;

public interface IConfigMidiPreset {
    MidiPreset forgeMidiPreset(SynthRipperConfiguration config);

    String getTitle();
}
