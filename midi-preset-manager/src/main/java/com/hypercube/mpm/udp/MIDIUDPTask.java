package com.hypercube.mpm.udp;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.UDPMidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MIDIUDPTask implements Runnable {
    private final ProjectConfiguration config;
    private final Map<Long, ChannelState> channels;
    private final DatagramPacket datagramPacket;

    @Override
    public void run() {
        log.info("Receive {} bytes", datagramPacket.getLength());
        ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
        long networkId = buffer.getInt() & 0xFFFFFFFFL;
        long packetNumber = buffer.getLong();
        int size = buffer.getShort() & 0xFFFF;
        byte[] data = new byte[size];
        buffer.get(data);
        ChannelState state = channels.get(networkId);
        if (state == null) {
            state = new ChannelState(networkId);
            channels.put(networkId, state);
        }
        state.addNewPacket(packetNumber, data);
        state.consumePackets(payload -> {
            MidiDeviceDefinition device = config.getMidiDeviceLibrary()
                    .getDeviceByNetworkId(networkId);
            config.getMidiDeviceManager()
                    .getOutput(device.getOutputMidiDevice())
                    .ifPresentOrElse(port -> {
                        if (!(port instanceof UDPMidiOutDevice)) {
                            CustomMidiEvent evt = SysExBuilder.forgeCustomMidiEvent(payload);
                            log.info("Send " + evt.getHexValues() + " to device " + device.getDeviceName());
                            if (!port.isOpen()) {
                                port.open();
                            }
                            port.send(evt);
                        } else {
                            log.error("The port " + port.getName() + " is not a MIDI port");
                        }
                    }, () -> {
                        log.error("The port '{}' for device '{}' is not available. Did you switch on the device ?", device.getOutputMidiDevice(), device.getDeviceName());
                    });
        });
    }
}
