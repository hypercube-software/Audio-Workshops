package com.hypercube.workshop.midiworkshop.api.ports.remote.server;

import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.remote.msg.NetWorkMessageOrigin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.Socket;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A network session is bound to a specific device
 */
@Getter
@Slf4j
public final class NetworkServerSession {
    /**
     * TCP socket bound to this session
     */
    private final Socket clientSocket;
    /**
     * Which device is bound to this session
     */
    private final long networkId;
    /**
     * Reception buffer, the key is the packet number
     */
    private final Map<Long, NetworkPacket> receivedPackets = new ConcurrentHashMap<>();
    /**
     * Counter of the next expected packet number
     */
    private final AtomicLong nextPacketCounter = new AtomicLong();
    /**
     * Counter of the next sent packet number
     */
    private final AtomicLong nextSentPacketCounter = new AtomicLong();
    /**
     * Received packets ordered by their number
     */
    private final BlockingQueue<NetworkPacket> blockingQueue = new LinkedBlockingQueue<>();
    /**
     * Listen also the real device and send back to the network
     */
    private final MidiInPort hardwareMidiInPort;
    /**
     * The listener attached to the hardware device
     */
    @Setter
    private MidiListener hardwareMidiListener;

    public NetworkServerSession(Socket clientSocket, long networkId, MidiInPort hardwareMidiInPort) {
        this.clientSocket = clientSocket;
        this.networkId = networkId;
        this.hardwareMidiInPort = hardwareMidiInPort;
    }

    /**
     * Block until new network packets are available
     */
    public List<NetworkPacket> getCurrentPackets() {
        return receivedPackets.values()
                .stream()
                .sorted(Comparator.comparingLong(NetworkPacket::packetId))
                .toList();
    }

    public long getExpectedPacketNumber() {
        return nextPacketCounter.get();
    }

    public void addNewPacket(NetWorkMessageOrigin origin, Long packetNumber, byte[] data) {
        if (getExpectedPacketNumber() <= packetNumber) {
            receivedPackets.put(packetNumber, new NetworkPacket(this, packetNumber, data));
        } else {
            log.info("Packet {} from {} already handled via {}", packetNumber, origin, origin.opposite());
        }
        // see if something can be pushed to the ordered queue
        pushOrderedPackets();
    }

    public Optional<NetworkPacket> peekPacket() {
        return Optional.ofNullable(blockingQueue.peek());
    }

    public Optional<NetworkPacket> takePacket() {
        return Optional.ofNullable(blockingQueue.poll());
    }

    private synchronized void pushOrderedPackets() {
        long packetNumber = nextPacketCounter.get();
        NetworkPacket packet;
        while ((packet = receivedPackets.get(packetNumber)) != null) {
            blockingQueue.add(packet);
            receivedPackets.remove(packetNumber);
            packetNumber = nextPacketCounter.incrementAndGet();
        }
        log.info("Wait next packet {}", packetNumber);
    }
}
