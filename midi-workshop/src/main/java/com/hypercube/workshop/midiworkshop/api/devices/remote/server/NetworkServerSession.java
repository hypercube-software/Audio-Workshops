package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

import com.hypercube.workshop.midiworkshop.api.devices.remote.msg.NetWorkMessageOrigin;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
@RequiredArgsConstructor
@Getter
@Slf4j
public final class NetworkServerSession {
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
     * When the session is created
     */
    private final long startTimestamp = System.nanoTime();
    /**
     * Received packets ordered by their number
     */
    private final BlockingQueue<NetworkPacket> blockingQueue = new LinkedBlockingQueue<>();

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

    public void addNewPacket(NetWorkMessageOrigin origin, Long packetNumber, long sendTimeStamp, byte[] data) {
        if (getExpectedPacketNumber() <= packetNumber) {
            receivedPackets.put(packetNumber, new NetworkPacket(this, packetNumber, System.nanoTime() - startTimestamp, sendTimeStamp, data));
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
        try {
            return Optional.ofNullable(blockingQueue.poll(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void pushOrderedPackets() {
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
