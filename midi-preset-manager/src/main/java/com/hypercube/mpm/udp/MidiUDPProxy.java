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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MidiUDPProxy {
    private final ConfigurationFactory configurationFactory;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public void start(int port) {
        ProjectConfiguration config = configurationFactory.loadConfig();
        try {
            log.info("Wait incoming messages on UDP port %d".formatted(port));
            DatagramSocket serverSocket = new DatagramSocket(port);
            for (; ; ) {
                DatagramPacket packet = listen(serverSocket);
                executor.execute(new MIDIUDPTask(config, packet));
            }
        } catch (SocketException e) {
            throw new MidiError("Unable to listen on port " + port, e);
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
