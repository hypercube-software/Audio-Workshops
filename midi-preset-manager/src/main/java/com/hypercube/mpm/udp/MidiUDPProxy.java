package com.hypercube.mpm.udp;

import com.hypercube.mpm.config.ConfigurationFactory;
import com.hypercube.mpm.config.ProjectConfiguration;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MidiUDPProxy {


    private final ConfigurationFactory configurationFactory;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<Long, ChannelState> channels = new HashMap<>();
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean shutdown = false;

    public void start(int port) {
        ProjectConfiguration config = configurationFactory.loadConfig();
        cleanupScheduler.scheduleAtFixedRate(() -> {
            channels.forEach((networkId, state) -> state.cleanupStalePaquets(ChannelState.CLEANUP_INTERVAL_SECONDS));
        }, ChannelState.CLEANUP_INTERVAL_SECONDS, ChannelState.CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
        Thread shutdownHook = new Thread(() -> {
            log.info("JVM Shutdown...");
            shutdown = true;
        });
        Runtime.getRuntime()
                .addShutdownHook(shutdownHook);

        try {
            log.info("Wait incoming messages on UDP port %d".formatted(port));
            DatagramSocket serverSocket = new DatagramSocket(port);
            while (!shutdown) {
                DatagramPacket packet = listen(serverSocket);
                executor.execute(new MIDIUDPTask(config, channels, packet));
            }
            serverSocket.close();
        } catch (SocketException e) {
            throw new MidiError("Unable to listen on port " + port, e);
        }
        cleanupScheduler.shutdown();
        log.info("MIDIUDPProxy done");
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
