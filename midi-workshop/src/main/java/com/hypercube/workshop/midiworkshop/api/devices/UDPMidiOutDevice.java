package com.hypercube.workshop.midiworkshop.api.devices;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/**
 * A little experimentation to send MIDI over UDP
 * <p>The CRC32 of the device name is what I call the "networkid", it identifies the target for the packet
 */
@Slf4j
public class UDPMidiOutDevice extends MidiOutDevice {
    public static final int MAX_UDP_PACKET_SIZE = 65507;
    // paquetId counters are kept during the entire JVM runtime, they are stored by networkid
    // TODO: add the concept of session to avoid this
    private static final Map<Long, AtomicLong> paquetId = new HashMap<>();
    private final String host;
    private final int port;
    private final String device;
    private final long networkId;
    private final InetAddress addr;
    private final String name;
    private DatagramSocket clientSocket;

    public UDPMidiOutDevice(String address) {
        super(null);
        this.name = address;
        String[] parts = address.split(":");
        this.host = parts[0];
        this.port = Integer.parseInt(parts[1]);
        this.device = parts[2];
        this.networkId = getDeviceNetworkId(device);
        try {
            this.addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new MidiError("Unable to parse host " + host, e);
        }
        paquetId.computeIfAbsent(this.networkId, key -> new AtomicLong(0));
    }

    public static boolean isUDPAddress(String name) {
        return name.split(":").length == 3;
    }

    public static long getDeviceNetworkId(String deviceName) {
        CRC32 crc32 = new CRC32();
        try {
            byte[] bytes = deviceName.getBytes("UTF-8");

            crc32.update(bytes, 0, bytes.length);
        } catch (UnsupportedEncodingException e) {
            log.error("Unexpected error", e);
            return -1;
        }

        return crc32.getValue();
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public boolean isOpen() {
        return clientSocket != null;
    }

    @Override
    public void open() {
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            clientSocket = null;
            throw new MidiError("Unable to connect to %s".formatted(getEndpoint()), e);
        }
    }

    @Override
    public void send(MidiMessage msg, long timestamp) {
        byte[] data = msg.getMessage();
        int size = msg.getLength();
        ByteBuffer buffer = ByteBuffer.allocate(size + 4 + +8 + 2);
        buffer.putInt((int) networkId);
        long packetId = paquetId.get(networkId)
                .getAndIncrement();
        buffer.putLong(packetId);
        buffer.putShort((short) size);
        buffer.put(data);
        byte[] payload = buffer.array();
        int payloadSize = buffer.position();
        if (payloadSize > MAX_UDP_PACKET_SIZE) {
            throw new MidiError("Packet size %d exceed max UDP size %d".formatted(payloadSize, MAX_UDP_PACKET_SIZE));
        }
        DatagramPacket packet = new DatagramPacket(payload, payloadSize, addr, port);
        try {
            log.info("Send {} bytes (0x {}) in networkId {} packet {} to {}", size, getHexValuesSpaced(data), "%16X".formatted(networkId), packetId, name);
            clientSocket.send(packet);
        } catch (IOException e) {
            throw new MidiError("Unable to send packet to %s".formatted(getEndpoint()), e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    private String getHexValuesSpaced(byte[] payload) {
        StringBuilder sb = new StringBuilder((payload.length + 1) * 2);
        sb.append("0x");
        for (byte b : payload)
            sb.append(String.format(" %02X", b));
        return sb.toString();
    }

    private String getEndpoint() {
        return "%s:%d".formatted(host, port);
    }
}
