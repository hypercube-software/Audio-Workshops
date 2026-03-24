package com.hypercube.workshop.midiworkshop.presets.kurzweil;

import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;

@FunctionalInterface
public interface DeviceConversation {
    void execute(MidiDeviceDefinition device, MidiInDevice midiInDevice, MidiOutDevice midiOutDevice);
}
