package com.hypercube.mpm.udp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
@Slf4j
public final class ChannelState {
    public static final long CLEANUP_INTERVAL_SECONDS = 30;
    public static final long MAX_PACKET_AGE_SECONDS = 60;

    private final long networkId;
    private final Map<Long, UDPPacket> packets = new ConcurrentHashMap<>();
    private final AtomicLong nextPacketNumber = new AtomicLong();

    public long getExpectedPacketNumber() {
        return nextPacketNumber.get();
    }

    public void addNewPacket(Long packetNumber, byte[] data) {
        packets.put(packetNumber, new UDPPacket(Instant.now(), data));

    }

    public void consumePackets(Consumer<byte[]> consummer) {
        long packetNumber = nextPacketNumber.get();
        for (; ; ) {
            UDPPacket packet = packets.get(packetNumber);
            if (packet != null) {
                consummer.accept(packet.payload());
                packets.remove(packetNumber);
                packetNumber = nextPacketNumber.incrementAndGet();
            } else {
                break;
            }
        }
    }

    public void cleanupStalePaquets(long maxPacketAgeSeconds) {
        Instant cutoffTime = Instant.now()
                .minusSeconds(maxPacketAgeSeconds);
        Long nextPacketNumber = getExpectedPacketNumber();
        packets.forEach((sequenceNumber, packetInfo) -> {
            if (packetInfo.receptionTime()
                    .isBefore(cutoffTime) && sequenceNumber < nextPacketNumber) {
                packets.remove(sequenceNumber);
                log.info("Purged stale packet with sequence number: " + sequenceNumber);
            }
        });
    }
}
