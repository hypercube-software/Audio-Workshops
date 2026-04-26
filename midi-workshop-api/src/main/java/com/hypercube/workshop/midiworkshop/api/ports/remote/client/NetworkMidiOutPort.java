package com.hypercube.workshop.midiworkshop.api.ports.remote.client;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.errors.RemoteDeviceError;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import com.hypercube.workshop.midiworkshop.api.ports.remote.NetworkIdBuilder;
import com.hypercube.workshop.midiworkshop.api.ports.remote.msg.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A little experimentation to send MIDI over UDP
 */
@Slf4j
public class NetworkMidiOutPort extends MidiOutPort {
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
    private Thread listener;
    @Getter
    private NetworkMidiInPort networkMidiInPort;

    public NetworkMidiOutPort(String definition) {
        super(definition);
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
    public boolean isOpen() {
        return connections.containsKey(definition);
    }

    @Override
    public void open() {
        super.open();
        if (getOpenCount() == 1) {
            try {
                connections.putIfAbsent(definition, openMidiOutConnection());
                sendViaTCP(new OpenSesssionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
            } catch (IOException e) {
                log.error("Unexpected error opening {}", definition, e);
                return;
            }
        }
    }

    @Override
    public void send(MidiMessage msg, long timestamp) {
        sendViaTCPAndUDP(new MidiNetworkMessage(NetWorkMessageOrigin.BOTH, networkId, getSession().getNextPacketId(), timestamp, new CustomMidiEvent(msg)));
    }

    @Override
    protected void effectiveClose() {
        log.info("Close network connection from {}", name);
        try {
            if (hasMidiOutConnection()) {
                sendViaTCP(new CloseSessionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
            }
        } catch (MidiError | IOException e) {
            log.error("Unexpected error closing {}", definition, e);
        } finally {
            if (getNetworkMidiInPort() != null) {
                getNetworkMidiInPort().close();
                listener.interrupt();
            }
            if (hasMidiOutConnection()) {
                try {
                    getTcpOutputStream().close();
                } catch (IOException e) {
                    log.error("Unable to close output TCP stream for {}", definition);
                }
                try {
                    getTcpInputStream().close();
                } catch (IOException e) {
                    log.error("Unable to close input TCP stream for {}", definition);
                }
                connections.remove(definition);
            }
        }
    }

    private MidiOutConnection openMidiOutConnection() throws IOException {
        try {
            log.info("Open connection towards {}...", name);
            inetAddress = resolveHostIP();
            DatagramSocket clientUDPSocket = new DatagramSocket();
            Socket clientTCPSocket = new Socket();
            clientTCPSocket.setKeepAlive(true);
            clientTCPSocket.setTcpNoDelay(true);
            SocketAddress socketAddress = new InetSocketAddress(inetAddress, port);
            clientTCPSocket.connect(socketAddress, CONNECTION_TIMEOUT_MS);
            OutputStream tcpOutputStream = clientTCPSocket.getOutputStream();
            InputStream tcpInputStream = clientTCPSocket.getInputStream();
            NetworkClientSession session = new NetworkClientSession();
            networkMidiInPort = new NetworkMidiInPort(definition, this);
            networkMidiInPort.open();
            listener = new Thread(() -> {
                try {
                    for (; ; ) {
                        NetworkMessage msg = NetworkMessage.deserialize(NetWorkMessageOrigin.TCP, tcpInputStream);
                        if (msg instanceof MidiNetworkMessage midiNetworkMessage) {
                            networkMidiInPort.onNewMidiEvent(midiNetworkMessage.getEvent());
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("Stop MidiInNetworkDevice '{}': {}", networkMidiInPort.getName(), e.getMessage());
                }
            });
            listener.start();
            return new MidiOutConnection(
                    session,
                    clientUDPSocket,
                    clientTCPSocket,
                    tcpOutputStream,
                    tcpInputStream);
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

    private InputStream getTcpInputStream() {
        return getMidiOutConnection().tcpInputStream();
    }

    private boolean hasMidiOutConnection() {
        return connections.containsKey(definition);
    }

    private MidiOutConnection getMidiOutConnection() {
        return Optional.ofNullable(connections.get(definition))
                .orElseThrow(() -> new MidiError("##################### There is no connection for " + definition));
    }

    private DatagramSocket getUdpDatagramSocket() {
        return getMidiOutConnection()
                .clientUDPSocket();
    }

    private NetworkClientSession getSession() {
        return Optional.ofNullable(getMidiOutConnection()
                        .session())
                .orElseThrow(() -> new MidiError("##################### There is no connection for " + definition));
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
