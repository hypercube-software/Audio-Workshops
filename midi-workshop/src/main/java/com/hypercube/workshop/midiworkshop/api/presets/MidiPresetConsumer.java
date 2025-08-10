package com.hypercube.workshop.midiworkshop.api.presets;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;

@FunctionalInterface
public interface MidiPresetConsumer {
    void onNewMidiPreset(MidiDeviceDefinition device, MidiPreset midiPreset);
}
