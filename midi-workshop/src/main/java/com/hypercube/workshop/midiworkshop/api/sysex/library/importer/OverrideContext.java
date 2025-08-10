package com.hypercube.workshop.midiworkshop.api.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceMode;

public record OverrideContext(MidiDeviceMode mode, String command) {
}
