package com.hypercube.workshop.midiworkshop.common.presets;

import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceDefinition;

@FunctionalInterface
public interface MidiPresetConsumer {
    void onNewMidiPreset(MidiDeviceDefinition device, MidiPreset midiPreset);
}
