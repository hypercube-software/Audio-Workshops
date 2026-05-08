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

    /**
     * Connection host for TCP and UDP
     */
    private final String host;
    /**
     * Connection port for TCP and UDP
     */
    private final int port;
    /**
     * Device name which is defined in the midi device library
     */
    private final String deviceName;
    /**
     * CRC32 of the deviceName
     */
    private final long networkId;
    /**
     * Something like "hypercube.serverpit.com:10192:DS-330"
     */
    private final String definition;
    /**
     * The key of this hashmap is the "definition" string
     * <p>MidiOutNetworkDevice is multi-clients, this mean connections are shared between instances using the same definition
     */
    private final Map<String, MidiOutConnection> connections = new ConcurrentHashMap<>();
    /**
     * IP Address of the server
     */
    private InetAddress inetAddress;
    /**
     * Thread waiting network messages
     */
    private Thread listener;
    /**
     * Input port receiving the MIDI messages from the network
     */
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
    public void send(MidiMessage msg, long timestamp) {
        sendViaTCPAndUDP(new MidiNetworkMessage(NetWorkMessageOrigin.BOTH, networkId, getSession().getNextPacketId(), new CustomMidiEvent(msg)));
    }

    @Override
    protected void effectiveOpen() throws IOException {
        connections.putIfAbsent(definition, openMidiOutConnection());
        sendViaTCP(new OpenSesssionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
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
                var connection = getMidiOutConnection();
                try {
                    connection.tcpOutputStream().close();
                } catch (IOException e) {
                    log.error("Unable to close output TCP stream for {}", definition);
                }
                try {
                    connection.tcpInputStream().close();
                } catch (IOException e) {
                    log.error("Unable to close input TCP stream for {}", definition);
                }
                try {
                    connection.clientTCPSocket().close();
                } catch (IOException e) {
                    log.error("Unable to close TCP socket for {}", definition);
                }
                connection.clientUDPSocket().close();
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
            listener = new Thread(() -> listenNetworkMessages(tcpInputStream));
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

    /**
     * Wait for incoming messages and push them to the {@link NetworkMidiInPort}
     */
    private void listenNetworkMessages(InputStream tcpInputStream) {
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
        } finally {
            log.info("Network listener for remote device {} terminated", deviceName);
        }
    }

    private InetAddress resolveHostIP() {
        try {
            return InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw new RemoteDeviceError("Unable to resolve IP for host " + host, e);
        }
    }

    /**
     * The whole idea here is sent as fast as possible MIDI messages over the network
     * <ul>
     *     <li>UDP is fast but there is no guaranty the packet will not be lost</li>
     *     <li>TCP is slow but there is a guaranty the packet will not be lost</li>
     * </ul>
     * So we send in TCP and UDP and the fastest win. The server take care of ordering and duplicate packets
     */
    private void sendViaTCPAndUDP(NetworkMessage msg) {
        try {
            sendViaUDP(msg);
        } catch (Exception e) {
            log.warn("Unable to send packet via UDP to {}", getEndpoint(), e);
        }
        try {
            sendViaTCP(msg);
        } catch (IOException e) {
            throw new RemoteDeviceError("Unable to send packet via TCP to %s".formatted(getEndpoint()), e);
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
        var connection = getMidiOutConnection();
        synchronized (connection.tcpOutputStream()) {
            var out = connection.tcpOutputStream();
            msg.serialize(out);
            out.flush();
        }
    }

    private void sendViaUDP(NetworkMessage msg) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            msg.serialize(outputStream);
            byte[] payload = outputStream.toByteArray();
            if (payload.length <= MAX_UDP_PACKET_SIZE) {
                var socket = getUdpDatagramSocket();
                synchronized (socket) {
                    socket.send(new DatagramPacket(payload, payload.length, inetAddress, port));
                }
            } else {
                log.warn("Packet size {} exceed max UDP size {}, use only TCP...", payload.length, MAX_UDP_PACKET_SIZE);
            }
        }
    }

    private String getEndpoint() {
        return "%s:%d".formatted(host, port);
    }
}
