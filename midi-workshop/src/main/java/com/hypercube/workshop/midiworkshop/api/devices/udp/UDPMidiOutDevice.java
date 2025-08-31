package com.hypercube.workshop.midiworkshop.api.devices.udp;

import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * A little experimentation to send MIDI over UDP
 * <p>The CRC32 of the device name is what I call the "networkid", it identifies the target for the packet
 */
@Slf4j
public class UDPMidiOutDevice extends MidiOutDevice {
    public static final int MAX_UDP_PACKET_SIZE = 65507;
    private final String host;
    private final int port;
    private final String device;
    private final long networkId;
    private final InetAddress addr;
    private final String name;
    private UDPSession udpSession;
    private DatagramSocket clientUDPSocket;
    private Socket clientTCPSocket;
    private OutputStream tcpOutputStream;

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
        sendViaTCP(serializeSessionCloseMessage());
        udpSession = null;
        clientTCPSocket.close();
        clientTCPSocket = null;
    }

    @Override
    public boolean isOpen() {
        return clientUDPSocket != null && clientTCPSocket != null;
    }

    @Override
    public void open() {
        try {
            openCount.incrementAndGet();
            clientUDPSocket = new DatagramSocket();
            clientTCPSocket = new Socket(addr, port);
            clientTCPSocket.setKeepAlive(true);
            clientTCPSocket.setTcpNoDelay(true);
            tcpOutputStream = clientTCPSocket.getOutputStream();
            udpSession = new UDPSession();
            sendViaTCP(serializeSessionOpenMessage());
        } catch (IOException e) {
            clientUDPSocket = null;
            throw new MidiError("Unable to connect to %s (ip: %s)".formatted(getEndpoint(), addr.toString()), e);
        }
    }

    @Override
    public void send(MidiMessage msg, long timestamp) {
        send(serializeMidiMessage(msg));
    }

    @Override
    public String getName() {
        return name;
    }

    private void send(DatagramPacket packet) {
        try {
            //sendViaUDP(packet);
            sendViaTCP(packet);
        } catch (IOException e) {
            throw new MidiError("Unable to send packet to %s".formatted(getEndpoint()), e);
        }
    }

    private void sendViaTCP(DatagramPacket packet) throws IOException {
        try {
            if (clientTCPSocket != null) {
                tcpOutputStream
                        .write(packet.getData());
                tcpOutputStream.flush();
            } else {
                log.info("TCP link closed !");
            }
        } catch (SocketException e) {
            log.info("Reconnect with TCP... {}", e.getMessage());
            close();
            open();
            tcpOutputStream
                    .write(packet.getData());
            tcpOutputStream.flush();
        }
    }

    private void sendViaUDP(DatagramPacket packet) throws IOException {
        if (packet.getLength() <= MAX_UDP_PACKET_SIZE) {
            clientUDPSocket.send(packet);
        } else {
            log.warn("Packet size %d exceed max UDP size %d, use only TCP...".formatted(packet.getLength(), MAX_UDP_PACKET_SIZE));
        }
    }

    private DatagramPacket serializeSessionOpenMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4);
        buffer.put((byte) UDPPacketType.SESSION_OPEN.ordinal());
        buffer.putInt((int) networkId);
        byte[] payload = buffer.array();
        int payloadSize = buffer.position();
        return new DatagramPacket(payload, payloadSize, addr, port);
    }

    private DatagramPacket serializeSessionCloseMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4);
        buffer.put((byte) UDPPacketType.SESSION_CLOSE.ordinal());
        buffer.putInt((int) networkId);
        byte[] payload = buffer.array();
        int payloadSize = buffer.position();
        return new DatagramPacket(payload, payloadSize, addr, port);
    }

    private DatagramPacket serializeMidiMessage(MidiMessage msg) {
        byte[] data = msg.getMessage();
        int size = msg.getLength();
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 8 + 8 + 2 + size);
        buffer.put((byte) UDPPacketType.MIDI_EVENT.ordinal());
        buffer.putInt((int) networkId);
        long packetId = udpSession.getNextPacketId();
        buffer.putLong(packetId);
        buffer.putLong(udpSession.getCurrentTimestamp());
        buffer.putShort((short) size);
        buffer.put(data);
        byte[] payload = buffer.array();
        int payloadSize = buffer.position();
        log.info("Send {} bytes (0x {}) in networkId {} packet {} to {}", msg.getLength(), getHexValuesSpaced(msg.getMessage()), "%8X".formatted(networkId), packetId, name);
        return new DatagramPacket(payload, payloadSize, addr, port);
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
