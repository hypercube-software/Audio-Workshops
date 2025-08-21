package com.hypercube.workshop.midiworkshop.api.devices;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

@Slf4j
public class UDPMidiOutDevice extends MidiOutDevice {
    private final String host;
    private final int port;
    private final String device;
    private final long networkId;
    private final AtomicLong paquetId = new AtomicLong();
    private final InetAddress addr;
    private DatagramSocket clientSocket;

    public UDPMidiOutDevice(String address) {
        super(null);
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
        ByteBuffer buffer = ByteBuffer.allocate(size + 4 + 2);
        buffer.putInt((int) networkId);
        buffer.putLong(paquetId.incrementAndGet());
        buffer.putShort((short) size);
        buffer.put(data);
        byte[] payload = buffer.array();
        int payloadSize = buffer.position();
        DatagramPacket packet = new DatagramPacket(payload, payloadSize, addr, port);
        try {
            log.info("Send {} bytes to {}", size, getEndpoint());
            clientSocket.send(packet);
        } catch (IOException e) {
            throw new MidiError("Unable to send packet to %s".formatted(getEndpoint()), e);
        }
    }

    @Override
    public String getName() {
        return "UDP-MIDI " + getEndpoint();
    }

    private String getEndpoint() {
        return "%s:%d".formatted(host, port);
    }
}
