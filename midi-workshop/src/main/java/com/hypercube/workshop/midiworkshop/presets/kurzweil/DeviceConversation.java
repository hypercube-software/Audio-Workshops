package com.hypercube.workshop.midiworkshop.presets.kurzweil;

import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;

@FunctionalInterface
public interface DeviceConversation {
    void execute(MidiDeviceDefinition device, MidiInPort midiInPort, MidiOutPort midiOutPort);
}
