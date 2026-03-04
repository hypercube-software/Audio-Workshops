package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.api.sysex.library.device.ControllerValueType;

public record MidiControllerId(ControllerValueType type, int id) {

}
