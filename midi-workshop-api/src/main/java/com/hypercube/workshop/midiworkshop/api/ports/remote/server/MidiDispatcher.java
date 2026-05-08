package com.hypercube.workshop.midiworkshop.api.ports.remote.server;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.ports.remote.client.NetworkMidiOutPort;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.util.MidiEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MidiDispatcher {
    private final NetworkServer networkServer;

    public void onNewNetworkPacket(NetworkPacket packet) {
        try {
            MidiDeviceDefinition device = networkServer.getConfig()
                    .getDeviceByNetworkId(packet.session()
                            .getNetworkId());

            networkServer.getConfig()
                    .midiPortsManager()
                    .getOutput(device.getOutputMidiDevice())
                    .ifPresentOrElse(port -> {
                        CustomMidiEvent evt = MidiEventBuilder.forgeCustomMidiEvent(packet.payload(), MidiOutPort.NO_TIME_STAMP);
                        log.info("Send {} to device {}", evt.getHexValues(), device.getDeviceName());
                        if (!(port instanceof NetworkMidiOutPort)) {
                            if (!port.isOpen()) {
                                port.open();
                            }
                            port.send(evt);
                        } else {
                            log.error("The port {} is not a MIDI port", port.getName());
                        }
                    }, () -> log.error("The port '{}' for device '{}' is not available. Did you switch on the device ?", device.getOutputMidiDevice(), device.getDeviceName()));
        } catch (MidiError e) {
            log.error("Unexpected error in MidiDispatcher", e);
        }
    }
}
