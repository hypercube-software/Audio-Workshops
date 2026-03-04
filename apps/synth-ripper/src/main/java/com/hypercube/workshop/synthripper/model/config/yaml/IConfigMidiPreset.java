package com.hypercube.workshop.synthripper.model.config.yaml;

import com.hypercube.workshop.midiworkshop.api.presets.MidiPreset;
import com.hypercube.workshop.synthripper.model.config.SynthRipperConfiguration;

public interface IConfigMidiPreset {
    MidiPreset forgeMidiPreset(SynthRipperConfiguration config);

    String getTitle();
}
