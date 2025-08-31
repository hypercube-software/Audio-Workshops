package com.hypercube.mpm.udp;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.udp.UDPMidiOutDevice;
import com.hypercube.workshop.midiworkshop.api.devices.udp.UDPPacketType;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.sysex.library.device.MidiDeviceDefinition;
import com.hypercube.workshop.midiworkshop.api.sysex.util.SysExBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MidiUDPProxy {
    private static final int THREAD_POOL_SIZE = 10;
    private final ConfigurationFactory configurationFactory;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    @Getter
    private final Map<Long, UDPSessionState> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean shutdown = false;
    private ProjectConfiguration config;
    private int listenPort;

    public void start(int port) {
        listenPort = port;
        config = configurationFactory.loadConfig();
        cleanupScheduler.scheduleAtFixedRate(() -> sessions.forEach((networkId, state) -> state.cleanupStalePaquets(UDPSessionState.CLEANUP_INTERVAL_SECONDS)), UDPSessionState.CLEANUP_INTERVAL_SECONDS, UDPSessionState.CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
        Thread sender = new Thread(this::senderThread);
        sender.setPriority(Thread.MAX_PRIORITY);
        sender.start();
        Thread server = new Thread(this::tcpListener);
        server.start();
        Thread shutdownHook = new Thread(() -> {
            log.info("JVM Shutdown...");
            shutdown = true;
        });
        Runtime.getRuntime()
                .addShutdownHook(shutdownHook);

        try {
            log.info("Wait UDP clients on port %d".formatted(listenPort));
            DatagramSocket serverSocket = new DatagramSocket(listenPort);
            while (!shutdown) {
                DatagramPacket packet = listen(serverSocket);
                executor.execute(new MIDIUDPTask(config, this, packet));
            }
            serverSocket.close();
            sender.interrupt();
        } catch (SocketException e) {
            throw new MidiError("Unable to listen on port " + listenPort, e);
        }
        cleanupScheduler.shutdown();
        log.info("MIDIUDPProxy done");
    }

    private void tcpListener() {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        log.info("Wait TCP clients on port " + listenPort);
        try {
            ServerSocket serverSocket = new ServerSocket(listenPort);
            serverSocket.setReuseAddress(true);
            for (; ; ) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(() -> onNewTcpClient(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
    }

    private int readByte(InputStream in) {
        try {
            int v = in.read();
            if (v == -1) {
                throw new RuntimeException("Client socket closed");
            }
            return v;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] read(InputStream in, int size) {
        byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) readByte(in);
        }
        return data;
    }

    private int readInt16(InputStream in) {
        return ((int) readByte(in) << 8) | (int) readByte(in);
    }

    private long readInt32(InputStream in) {
        return ((long) readByte(in) << 24) | ((long) readByte(in) << 16) | ((long) readByte(in) << 8) | (long) readByte(in);
    }

    private long readLong(InputStream in) {
        return readInt32(in) << 32 | readInt32(in);
    }

    private void onNewTcpClient(Socket clientSocket) {
        UDPSessionState session = null;
        log.info("TCP Client connected: {}", clientSocket.getRemoteSocketAddress()
                .toString());
        try {
            var in = clientSocket.getInputStream();
            var connected = true;
            while (connected) {
                UDPPacketType type = UDPPacketType.values()[readByte(in)];
                switch (type) {
                    case SESSION_OPEN -> session = onOpenSession(in);
                    case SESSION_CLOSE -> {
                        onCloseSession(in);
                        connected = false;
                    }
                    case MIDI_EVENT -> onMidiEvent(in);
                }
            }
        } catch (Exception e) {
            log.info("=====================> TCP Client {} gone", clientSocket.getRemoteSocketAddress()
                    .toString());
        } finally {
            if (session != null) {
                log.info("Close TCP session {}", session.getNetworkId());
                getSessions()
                        .remove(session.getNetworkId());
            }
        }
    }

    private void onMidiEvent(InputStream in) {
        long networkId = readInt32(in);
        long packetNumber = readLong(in);
        long sendTimeStamp = readLong(in);
        int size = readInt16(in);
        byte[] data = read(in, size);
        MidiDeviceDefinition device = config.getMidiDeviceLibrary()
                .getDeviceByNetworkId(networkId);
        log.info("Receive {} bytes , packet {}, networkId {} => {}", data.length, packetNumber, networkId, device.getDeviceName());
        if (!getSessions()
                .containsKey(networkId)) {
            log.warn("Session closed, ignore the msg");
            return;
        }
        // MIDI is a unique stream of serialized data, so only one thread can write towards the device at a time
        synchronized (device) {
            UDPSessionState session = getSessions()
                    .get(networkId);
            session.addNewPacket(packetNumber, sendTimeStamp, data);
            session.pushOrderedPackets();
        }
    }


    private void onCloseSession(InputStream in) {
        long networkId = readInt32(in);
        getSessions()
                .remove(networkId);
        log.info("close session {}", networkId);
    }

    private UDPSessionState onOpenSession(InputStream in) {
        long networkId = readInt32(in);
        UDPSessionState state = new UDPSessionState(networkId);
        getSessions()
                .put(networkId, state);
        log.info("open session {}", networkId);
        return state;
    }

    private void senderThread() {
        log.info("Wait queued events...");
        long startTimestamp = System.nanoTime();
        for (; ; ) {
            long currentTimestamp = System.nanoTime() - startTimestamp;
            sessions.values()
                    .forEach(s -> {
                        s.peekPacket()
                                .filter(p -> p.sendTimestamp() <= currentTimestamp)
                                .flatMap(p -> s.takePacket())
                                .ifPresent(packet -> {
                                    MidiDeviceDefinition device = config.getMidiDeviceLibrary()
                                            .getDeviceByNetworkId(s.getNetworkId());

                                    config.getMidiDeviceManager()
                                            .getOutput(device.getOutputMidiDevice())
                                            .ifPresentOrElse(port -> {
                                                CustomMidiEvent evt = SysExBuilder.forgeCustomMidiEvent(packet.payload(), packet.sendTimestamp());
                                                log.info("[" + currentTimestamp + "] Send " + evt.getHexValues() + " to device " + device.getDeviceName());
                                                if (!(port instanceof UDPMidiOutDevice)) {
                                                    if (!port.isOpen()) {
                                                        port.open();
                                                    }
                                                    port.send(evt);
                                                } else {
                                                    log.error("The port " + port.getName() + " is not a MIDI port");
                                                }
                                            }, () -> log.error("The port '{}' for device '{}' is not available. Did you switch on the device ?", device.getOutputMidiDevice(), device.getDeviceName()));
                                });
                    });
        }
    }


    private DatagramPacket listen(DatagramSocket serverSocket) {
        byte[] receiveData = new byte[65507];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        try {
            serverSocket.receive(receivePacket);
            return receivePacket;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
