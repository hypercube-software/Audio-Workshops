package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.remote.client.MidiOutNetworkDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MidiDispatcher {
    private final NetworkServer networkServer;

    public void onNewNetworkPacket(NetworkPacket packet) {
        MidiDeviceDefinition device = networkServer.getConfig()
                .getDeviceByNetworkId(packet.session()
                        .getNetworkId());

        networkServer.getConfig()
                .midiPortsManager()
                .getOutput(device.getOutputMidiDevice())
                .ifPresentOrElse(port -> {
                    CustomMidiEvent evt = MidiEventBuilder.forgeCustomMidiEvent(packet.payload(), packet.sendTimestamp());
                    log.info("Send " + evt.getHexValues() + " to device " + device.getDeviceName());
                    if (!(port instanceof MidiOutNetworkDevice)) {
                        if (!port.isOpen()) {
                            port.open();
                        }
                        port.send(evt);
                    } else {
                        log.error("The port " + port.getName() + " is not a MIDI port");
                    }
                }, () -> log.error("The port '{}' for device '{}' is not available. Did you switch on the device ?", device.getOutputMidiDevice(), device.getDeviceName()));

    }
}
