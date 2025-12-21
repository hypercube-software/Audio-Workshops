package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.remote.msg.*;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
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
        try (ByteArrayInputStream in = new ByteArrayInputStream(packet.getData())) {
            onNewNetworkPacket(NetWorkMessageOrigin.UDP, null, in);
        } catch (IOException e) {
            throw new MidiError(e);
        }
    }

    private void tcpListener() {
        log.info("TCP listener started, Wait TCP clients on port {}", listenPort);
        try (ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE)) {
            try {
                try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
                    serverSocket.setReuseAddress(true);
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
        try {
            var in = clientSocket.getInputStream();
            while (!shutdown) {
                if (onNewNetworkPacket(NetWorkMessageOrigin.TCP, clientSocket, in)) {
                    break; // session close event received, exit this thread
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in TCP thread {}", clientSocket.getRemoteSocketAddress()
                    .toString(), e);
        }
        log.info("TCP thread {} terminated", clientSocket.getRemoteSocketAddress()
                .toString());
    }

    private void onRealMidiEvent(NetworkServerSession session, CustomMidiEvent customMidiEvent) {
        try {
            log.info("Send back to TCP midi message from {}: {}", session.getMidiInDevice()
                    .getName(), customMidiEvent.getHexValuesSpaced());
            MidiNetworkMessage msg = new MidiNetworkMessage(NetWorkMessageOrigin.TCP, session.getNetworkId(), session.getNextSentPacketCounter()
                    .incrementAndGet(), customMidiEvent.getTick(), customMidiEvent);
            msg.serialize(session.getClientSocket()
                    .getOutputStream());
        } catch (IOException e) {
            log.error("Unable to serialize midi event to TCP", e);
        }
    }

    private void onNetworkMidiEvent(MidiNetworkMessage msg) {
        MidiDeviceDefinition device = config.getDeviceByNetworkId(msg.getNetworkId());
        log.info("Receive {} bytes , packet {}, networkId {} => {}", msg.getEvent()
                .getMessage()
                .getLength(), msg.getPacketId(), msg.getNetworkId(), device.getDeviceName());
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
        session.addNewPacket(msg.getOrigin(), msg.getPacketId(), msg.getTimeStamp(), data);
    }


    private void onCloseSession(CloseSessionNetworkMessage msg) {
        log.info("close session {}", msg.getNetworkId());
        Optional.ofNullable(getSessions().get(msg.getNetworkId()))
                .ifPresent(s -> {
                    var inputMidiPort = s.getMidiInDevice();
                    if (inputMidiPort != null) {
                        try {
                            inputMidiPort.close();
                        } catch (Exception e) {
                            log.error("Unable to close device '{}'", inputMidiPort.getName(), e);
                        }
                    }
                });
        getSessions()
                .remove(msg.getNetworkId());
    }

    private void onOpenSession(Socket clientSocket, OpenSesssionNetworkMessage msg) {
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
        log.info("open session {}", msg.getNetworkId());
        if (inputPort == null) {
            log.warn("No Midi input port found for device '{}'", device.getDeviceName());
        } else {
            log.info("Listening Midi port {}", inputPort.getName());
            inputPort.open();
            inputPort.addListener((midiInDevice, event) -> onRealMidiEvent(session, event));
            inputPort.startListening();
        }
    }


    private void udpListener() {
        log.info("UDP Listener started, Wait UDP clients on port {}", listenPort);
        try (DatagramSocket serverSocket = new DatagramSocket(listenPort)) {
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
        log.info("dispatcherThread started, waiting ordered queued MIDI events...");
        long startTimestamp = System.nanoTime();
        while (!shutdown) {
            long currentTimestamp = System.nanoTime() - startTimestamp;
            sessions.values()
                    .forEach(s -> s.peekPacket()
                            .filter(p -> p.sendTimestamp() <= currentTimestamp)
                            .flatMap(p -> s.takePacket())
                            .ifPresent(midiDispatcher::onNewNetworkPacket));
        }
        log.info("dispatcherThread terminated");
    }
}
