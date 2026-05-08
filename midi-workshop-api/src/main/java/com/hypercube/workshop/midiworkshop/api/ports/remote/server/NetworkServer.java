package com.hypercube.workshop.midiworkshop.api.ports.remote.server;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.listener.SysExMidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.remote.msg.*;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sound.midi.ShortMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkServer {
    private static final int THREAD_POOL_SIZE = 10;
    @Getter
    private final NetworkServerConfig config;
    private final MidiDispatcher midiDispatcher = new MidiDispatcher(this);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    @Getter
    private final Map<Long, NetworkServerSession> sessions = new ConcurrentHashMap<>();
    private boolean shutdown = false;
    private int listenPort;

    public void start(int port) {
        listenPort = port;
        Thread dispatcher = new Thread(this::dispatcherThread);
        dispatcher.setPriority(Thread.MAX_PRIORITY);
        dispatcher.start();
        Thread tcpServer = new Thread(this::tcpListener);
        tcpServer.start();
        Thread udpServer = new Thread(this::udpListener);
        udpServer.start();
        Thread shutdownHook = new Thread(() -> {
            log.info("JVM Shutdown...");
            shutdown = true;
            executor.shutdown();
        });
        Runtime.getRuntime()
                .addShutdownHook(shutdownHook);
        try {
            udpServer.join();
            tcpServer.join();
            dispatcher.join();
            log.info("MIDIUDPProxy done");
        } catch (InterruptedException e) {
            log.info("Interrupted");
        }
    }

    private void onNewUDPPacket(DatagramPacket packet) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength())) {
            onNewNetworkPacket(NetWorkMessageOrigin.UDP, null, in);
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }

    private void tcpListener() {
        log.info("TCP listener started, Wait TCP clients on port {}", listenPort);
        try (ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE)) {
            try {
                try (ServerSocket serverSocket = new ServerSocket()) {
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new java.net.InetSocketAddress(listenPort));
                    while (!shutdown) {
                        Socket clientSocket = serverSocket.accept();
                        pool.execute(() -> tcpThread(clientSocket));
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error in TCP listener", e);
            } finally {
                pool.shutdown();
            }
        }
        shutdown();
        log.info("TCP listener terminated");
    }

    private void shutdown() {
        shutdown = true;
    }

    private boolean onNewNetworkPacket(NetWorkMessageOrigin origin, Socket clientSocket, InputStream in) {
        NetworkMessage msg = NetworkMessage.deserialize(origin, in);
        switch (msg.getType()) {
            case OPEN_SESSION -> onOpenSession(clientSocket, (OpenSesssionNetworkMessage) msg);
            case CLOSE_SESSION -> onCloseSession((CloseSessionNetworkMessage) msg);
            case MIDI_EVENT -> onNetworkMidiEvent((MidiNetworkMessage) msg);
        }
        return msg.getType() == NetworkMessageType.CLOSE_SESSION;
    }

    private void tcpThread(Socket clientSocket) {
        log.info("TCP Client connected: {}", clientSocket.getRemoteSocketAddress()
                .toString());
        Long networkId = null;
        String hexNetworkId = null;
        try {
            var in = clientSocket.getInputStream();
            while (!shutdown) {
                try {
                    NetworkMessage msg = NetworkMessage.deserialize(NetWorkMessageOrigin.TCP, in);
                    if (msg instanceof OpenSesssionNetworkMessage openMsg) {
                        networkId = openMsg.getNetworkId();
                        hexNetworkId = openMsg.getHexNetworkId();
                        onOpenSession(clientSocket, openMsg);
                    } else if (msg instanceof CloseSessionNetworkMessage closeMsg) {
                        onCloseSession(closeMsg);
                        networkId = null;
                        hexNetworkId = null;
                        break; // session close event received, exit this thread
                    } else if (msg instanceof MidiNetworkMessage midiMsg) {
                        onNetworkMidiEvent(midiMsg);
                    }
                } catch (Exception e) {
                    log.error("Error processing TCP network packet from {}: {}", clientSocket.getRemoteSocketAddress()
                            .toString(), e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Fatal error in TCP thread {}", clientSocket.getRemoteSocketAddress()
                    .toString(), e);
        } finally {
            if (networkId != null) {
                // Unexpected termination, clean up the session
                log.warn("TCP thread for networkId {} terminated unexpectedly, cleaning up session", hexNetworkId);
                onCloseSession(new CloseSessionNetworkMessage(NetWorkMessageOrigin.TCP, networkId));
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.error("Error closing socket", e);
            }
        }
        log.info("TCP thread {} terminated", clientSocket.getRemoteSocketAddress()
                .toString());
    }

    private void onRealMidiEvent(NetworkServerSession session, CustomMidiEvent customMidiEvent) {
        try {
            if (customMidiEvent.getMessage()
                    .getStatus() != ShortMessage.ACTIVE_SENSING && customMidiEvent.getMessage()
                    .getStatus() != ShortMessage.TIMING_CLOCK) {
                log.info("Send back to TCP midi message from '{}': {}", session.getHardwareMidiInPort()
                        .getName(), customMidiEvent.getHexValuesSpaced());
            }
            MidiNetworkMessage msg = new MidiNetworkMessage(NetWorkMessageOrigin.TCP, session.getNetworkId(), session.getNextSentPacketCounter()
                    .incrementAndGet(), customMidiEvent);
            msg.serialize(session.getClientSocket()
                    .getOutputStream());
        } catch (IOException e) {
            log.error("Unable to serialize midi event to TCP", e);
        }
    }

    private void onNetworkMidiEvent(MidiNetworkMessage msg) {
        MidiDeviceDefinition device = config.getDeviceByNetworkId(msg.getNetworkId());
        log.info("Receive {} bytes , packet {}, networkId {} => {}",
                msg.getEvent()
                        .getMessage()
                        .getLength(),
                msg.getPacketId(),
                msg.getHexNetworkId(),
                device.getDeviceName());
        if (!getSessions()
                .containsKey(msg.getNetworkId())) {
            log.warn("Session closed, ignore the msg");
            return;
        }
        NetworkServerSession session = getSessions()
                .get(msg.getNetworkId());
        byte[] data = msg.getEvent()
                .getMessage()
                .getMessage();
        session.addNewPacket(msg.getOrigin(), msg.getPacketId(), data);
    }


    private void onCloseSession(CloseSessionNetworkMessage msg) {
        log.info("close session {}", msg.getHexNetworkId());
        NetworkServerSession session = getSessions().remove(msg.getNetworkId());
        if (session != null) {
            var inputMidiPort = session.getHardwareMidiInPort();
            if (inputMidiPort != null) {
                if (session.getHardwareMidiListener() != null) {
                    inputMidiPort.removeListener(session.getHardwareMidiListener());
                }
                try {
                    inputMidiPort.close();
                } catch (Exception e) {
                    log.error("Unable to close device '{}'", inputMidiPort.getName(), e);
                }
            }
        }
    }

    private void onOpenSession(Socket clientSocket, OpenSesssionNetworkMessage msg) {
        // clean up existing session if any
        if (getSessions().containsKey(msg.getNetworkId())) {
            log.info("Clean up existing session for {}", msg.getHexNetworkId());
            onCloseSession(new CloseSessionNetworkMessage(NetWorkMessageOrigin.TCP, msg.getNetworkId()));
        }

        MidiDeviceDefinition device = config.getDeviceByNetworkId(msg.getNetworkId());
        var inputPort = config
                .midiPortsManager()
                .getInput(device.getInputMidiDevice())
                .orElse(null);
        if (inputPort == null) {
            inputPort = config
                    .midiPortsManager()
                    .getInput("debug")
                    .orElse(null);
        }
        NetworkServerSession session = new NetworkServerSession(clientSocket, msg.getNetworkId(), inputPort);
        getSessions()
                .put(msg.getNetworkId(), session);
        log.info("open session {}", msg.getHexNetworkId());
        if (inputPort == null) {
            log.warn("No Midi input port found for device '{}'", device.getDeviceName());
        } else {
            log.info("Listening Midi port {}", inputPort.getName());
            inputPort.open();
            // TODO: release this listener
            SysExMidiListener listener = new SysExMidiListener((midiInDevice, event) -> onRealMidiEvent(session, event));
            session.setHardwareMidiListener(listener);
            inputPort.addListener(listener);
        }
    }


    private void udpListener() {
        log.info("UDP Listener started, Wait UDP clients on port {}", listenPort);
        try (DatagramSocket serverSocket = new DatagramSocket(null)) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(listenPort));
            byte[] receiveData = new byte[65507];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            while (!shutdown) {
                serverSocket.receive(receivePacket);
                executor.execute(() -> onNewUDPPacket(receivePacket));
            }
        } catch (Exception e) {
            log.error("Unexpected error in UDP listener", e);
        }
        shutdown();
        log.info("UDP Listener terminated");
    }

    private void dispatcherThread() {
        log.info("DispatcherThread started, waiting ordered queued MIDI events...");
        try {
            while (!shutdown) {
                boolean packetProcessed = false;
                for (NetworkServerSession s : sessions.values()) {
                    packetProcessed |= s.takePacket()
                            .map(p -> {
                                midiDispatcher.onNewNetworkPacket(p);
                                return true;
                            })
                            .orElse(false);
                }
                // prevent active loop if no packet is processed
                if (!packetProcessed) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread()
                                .interrupt();
                        break;
                    }
                }
            }
        } finally {
            log.info("DispatcherThread terminated");
        }
    }
}
