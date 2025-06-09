package com.hypercube.workshop.midiworkshop.common.sysex.library.importer;

import com.hypercube.workshop.midiworkshop.common.sysex.library.device.MidiDeviceMode;

public record OverrideContext(MidiDeviceMode mode, String command) {
}
