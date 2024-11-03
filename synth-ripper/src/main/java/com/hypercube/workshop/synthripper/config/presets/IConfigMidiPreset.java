package com.hypercube.workshop.synthripper.config.presets;

import com.hypercube.workshop.midiworkshop.common.presets.MidiBankFormat;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPreset;
import com.hypercube.workshop.midiworkshop.common.presets.MidiPresetNumbering;

public interface IConfigMidiPreset {
    MidiPreset forgeMidiPreset(MidiBankFormat midiBankFormat, MidiPresetNumbering presetNumbering);

    String getTitle();
}
