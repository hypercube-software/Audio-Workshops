package com.hypercube.workshop.midiworkshop.api.devices.remote.client;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.devices.remote.NetworkIdBuilder;
import com.hypercube.workshop.midiworkshop.api.devices.remote.msg.*;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.errors.RemoteDeviceError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A little experimentation to send MIDI over UDP
 */
@Slf4j
public class MidiOutNetworkDevice extends MidiOutDevice {
    public static final int MAX_UDP_PACKET_SIZE = 65507;
    private static final int CONNECTION_TIMEOUT_MS = 2000;

    private final String host;
    private final int port;
    private final String deviceName;
    private final long networkId;
    private final String definition;
    // MidiOutNetworkDevice is multi-clients, this mean connections are shared between instances using the same definition
    // The key of this hashmap is the "definition" string
    private final Map<String, MidiOutConnection> connections = new ConcurrentHashMap<>();
    private InetAddress inetAddress;

    public MidiOutNetworkDevice(String definition) {
        super(null);
        this.definition = definition;
        String[] parts = definition.split(":");
        this.host = parts[0];
        this.port = Integer.parseInt(parts[1]);
        this.deviceName = parts[2];
        this.networkId = NetworkIdBuilder.getDeviceNetworkId(deviceName);
    }

    public static boolean isRemoteAddress(String name) {
        return name.split(":").length == 3;
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            sendViaTCP(new CloseSessionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
        } catch (MidiError e) {
            log.error("Unexpected error closing {}", definition, e);
        } finally {
            if (getOpenCount() == 0) {
                try {
                    getTcpOutputStream().close();
                } catch (IOException e) {
                    log.error("Unable to close TCP stream for {}", definition);
                }
                connections.remove(definition);
            }
        }
    }

    @Override
    public boolean isOpen() {
        return connections.containsKey(definition);
    }

    @Override
    public void open() {
        if (getOpenCount() == 0) {
            try {
                connections.putIfAbsent(definition, openMidiOutConnection());
                sendViaTCP(new OpenSesssionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
            } catch (IOException e) {
                log.error("Unexpected error opening {}", definition, e);
                return;
            }
        }
        super.open();
    }

    @Override
    public void send(MidiMessage msg, long timestamp) {
        sendViaTCPAndUDP(new MidiNetworkMessage(NetWorkMessageOrigin.BOTH, networkId, getSession().getNextPacketId(), timestamp, new CustomMidiEvent(msg)));
    }

    @Override
    public String getName() {
        return definition;
    }

    private MidiOutConnection openMidiOutConnection() throws IOException {
        try {
            inetAddress = resolveHostIP();
            DatagramSocket clientUDPSocket = new DatagramSocket();
            Socket clientTCPSocket = new Socket();
            clientTCPSocket.setKeepAlive(true);
            clientTCPSocket.setTcpNoDelay(true);
            SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);
            clientTCPSocket.connect(socketAddress, CONNECTION_TIMEOUT_MS);
            OutputStream tcpOutputStream = clientTCPSocket.getOutputStream();
            NetworkClientSession session = new NetworkClientSession();
            return new MidiOutConnection(
                    session,
                    clientUDPSocket,
                    clientTCPSocket,
                    tcpOutputStream);
        } catch (IOException e) {
            throw new RemoteDeviceError("Unable to connect to %s (ip: %s)".formatted(getEndpoint(), inetAddress.toString()), e);
        }
    }

    private InetAddress resolveHostIP() {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RemoteDeviceError("Unable to resolve IP for host " + host, e);
        }
    }

    private void sendViaTCPAndUDP(NetworkMessage msg) {
        try {
            // the fastest win
            sendViaUDP(msg);
            sendViaTCP(msg);
        } catch (IOException e) {
            throw new RemoteDeviceError("Unable to send packet to %s".formatted(getEndpoint()), e);
        }
    }

    private OutputStream getTcpOutputStream() {
        return getMidiOutConnection()
                .tcpOutputStream();
    }

    private MidiOutConnection getMidiOutConnection() {
        return Optional.ofNullable(connections.get(definition))
                .orElseThrow(() -> new MidiError("There is no connection for " + definition));
    }

    private DatagramSocket getUdpDatagramSocket() {
        return getMidiOutConnection()
                .clientUDPSocket();
    }

    private NetworkClientSession getSession() {
        return Optional.ofNullable(getMidiOutConnection()
                        .session())
                .orElseThrow(() -> new MidiError("There is no connection for " + definition));
    }

    private void sendViaTCP(NetworkMessage msg) throws IOException {
        var out = getTcpOutputStream();
        if (out != null) {
            msg.serialize(out);
            out.flush();
        } else {
            log.info("TCP link closed for device {} !", deviceName);
        }
    }

    private void sendViaUDP(NetworkMessage msg) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            msg.serialize(outputStream);
            byte[] payload = outputStream.toByteArray();
            if (payload.length <= MAX_UDP_PACKET_SIZE) {
                getUdpDatagramSocket().send(new DatagramPacket(payload, payload.length, inetAddress, port));
            } else {
                log.warn("Packet size {} exceed max UDP size {}, use only TCP...", payload.length, MAX_UDP_PACKET_SIZE);
            }
        }
    }

    private String getEndpoint() {
        return "%s:%d".formatted(host, port);
    }
}
