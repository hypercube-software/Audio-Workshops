package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

import com.hypercube.workshop.midiworkshop.api.MidiPortsManager;
import com.hypercube.workshop.midiworkshop.api.sysex.library.MidiDeviceLibrary;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import org.springframework.stereotype.Component;

@Component
public record NetworkServerConfig(MidiDeviceLibrary midiDeviceLibrary, MidiPortsManager midiPortsManager) {
    public MidiDeviceDefinition getDeviceByNetworkId(long networkId) {
        return midiDeviceLibrary.getDeviceByNetworkId(networkId);
    }
}
