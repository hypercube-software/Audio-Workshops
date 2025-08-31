package com.hypercube.mpm.udp;

import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.devices.udp.UDPPacketType;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class MIDIUDPTask implements Runnable {
    private final ProjectConfiguration config;
    private final MidiUDPProxy proxy;
    private final DatagramPacket datagramPacket;

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.wrap(datagramPacket.getData());
        UDPPacketType type = UDPPacketType.values()[buffer.get()];
        if (Objects.requireNonNull(type) == UDPPacketType.MIDI_EVENT) {
            onMidiEvent(buffer);
        }
    }

    private void onMidiEvent(ByteBuffer buffer) {
        long networkId = buffer.getInt() & 0xFFFFFFFFL;
        long packetNumber = buffer.getLong();
        long sendTimeStamp = buffer.getLong();
        int size = buffer.getShort() & 0xFFFF;
        byte[] data = new byte[size];
        buffer.get(data);
        MidiDeviceDefinition device = config.getMidiDeviceLibrary()
                .getDeviceByNetworkId(networkId);
        log.info("Receive {} bytes , packet {}, networkId {} => {}", data.length, packetNumber, networkId, device.getDeviceName());
        if (!proxy.getSessions()
                .containsKey(networkId)) {
            log.warn("Session closed, ignore the msg");
            return;
        }
        // MIDI is a unique stream of serialized data, so only one thread can write towards the device at a time
        synchronized (device) {
            UDPSessionState session = proxy.getSessions()
                    .get(networkId);
            session.addNewPacket(packetNumber, sendTimeStamp, data);
            session.pushOrderedPackets();
        }
    }
}
