package com.hypercube.mpm.udp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
@Getter
@Slf4j
public final class UDPSessionState {
    public static final long CLEANUP_INTERVAL_SECONDS = 30;
    public static final long MAX_PACKET_AGE_SECONDS = 1;

    private final long networkId;
    private final Map<Long, UDPPacket> packets = new ConcurrentHashMap<>();
    private final AtomicLong nextPacketCounter = new AtomicLong();
    private final long startTimestamp = System.nanoTime();
    private final BlockingQueue<UDPPacket> blockingQueue = new LinkedBlockingQueue<>();
    private Instant lastReceivedPacket;

    public long getExpectedPacketNumber() {
        return nextPacketCounter.get();
    }

    public void addNewPacket(Long packetNumber, long sendTimeStamp, byte[] data) {
        if (getExpectedPacketNumber() <= packetNumber) {
            packets.put(packetNumber, new UDPPacket(System.nanoTime() - startTimestamp, sendTimeStamp, data));
            lastReceivedPacket = Instant.now();
        } else {
            log.info("Packet {} already handled", packetNumber);
        }
    }

    public Optional<UDPPacket> peekPacket() {
        return Optional.ofNullable(blockingQueue.peek());
    }

    public Optional<UDPPacket> takePacket() {
        try {
            return Optional.ofNullable(blockingQueue.poll(100, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void pushOrderedPackets() {
        long packetNumber = nextPacketCounter.get();
        Instant waitPacketStart = Instant.now();
        for (; ; ) {
            UDPPacket packet = packets.get(packetNumber);
            if (packet != null) {
                //consumer.accept(packet);
                blockingQueue.add(packet);
                packets.remove(packetNumber);
                packetNumber = nextPacketCounter.incrementAndGet();
                waitPacketStart = Instant.now();
                if (packets.isEmpty()) {
                    lastReceivedPacket = null;
                }
            } else {
                log.info("Wait packet {}", packetNumber);
                if (lastReceivedPacket != null && Duration.between(waitPacketStart, lastReceivedPacket)
                        .getSeconds() > MAX_PACKET_AGE_SECONDS) {
                    log.info("Drop Packet {}", packetNumber);
                    packetNumber = nextPacketCounter.incrementAndGet();
                    waitPacketStart = Instant.now();
                }
                break;
            }
        }
    }

    public void cleanupStalePaquets(long maxPacketAgeSeconds) {
        long cutoffTime = System.nanoTime() - (maxPacketAgeSeconds * 1_000_000_000L);
        Long nextPacketNumber = getExpectedPacketNumber();
        packets.forEach((sequenceNumber, packetInfo) -> {
            if (packetInfo.receptionTimestamp() < cutoffTime && sequenceNumber < nextPacketNumber) {
                packets.remove(sequenceNumber);
                log.info("Purged stale packet with sequence number: " + sequenceNumber);
            }
        });
    }
}
