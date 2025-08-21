package com.hypercube.mpm.udp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelStateTest {

    public static final int NETWORK_ID = 1;
    ChannelState state;

    @BeforeEach
    void init() {
        state = new ChannelState(NETWORK_ID);
    }

    @Test
    void regularOrder() {
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            state.addNewPacket((long) i, new byte[]{(byte) i});
            int finalI = i;
            state.consumePackets(payload -> {
                assertEquals(finalI, payload[0]);
                count.incrementAndGet();
            });
        }
        assertEquals(10, count.get());
    }

    @Test
    void reorderPackets() {
        AtomicInteger count = new AtomicInteger();
        Consumer<byte[]> consumer = payload -> {
            assertEquals(count.get(), payload[0]);
            count.incrementAndGet();
        };
        state.addNewPacket(2L, new byte[]{2});
        state.addNewPacket(1L, new byte[]{1});
        state.addNewPacket(0L, new byte[]{0});
        state.consumePackets(consumer);
        assertEquals(3, count.get());
    }

    @Test
    void reorderPacketsWithPause() {
        AtomicInteger count = new AtomicInteger();
        Consumer<byte[]> consumer = payload -> {
            assertEquals(count.get(), payload[0]);
            count.incrementAndGet();
        };
        state.addNewPacket(2L, new byte[]{2});
        state.consumePackets(consumer);
        state.addNewPacket(1L, new byte[]{1});
        state.consumePackets(consumer);
        state.addNewPacket(0L, new byte[]{0});
        state.consumePackets(consumer);
        assertEquals(3, count.get());
    }

    @Test
    void shouldCleanUpStalePackets() throws InterruptedException {
        Consumer<byte[]> consumer = (payload) -> {

        };

        // --- Step 1: Set up the scenario ---
        // Consume packets 0, 1, and 2. The next expected number is 3.
        state.addNewPacket(0L, new byte[]{0});
        state.addNewPacket(1L, new byte[]{1});
        state.addNewPacket(2L, new byte[]{2});
        state.consumePackets(consumer);
        assertEquals(3, state.getNextPacketNumber()
                .get(), "Next expected packet should be 3.");
        assertEquals(0, state.getPackets()
                .size(), "The map should be empty.");

        // --- Step 2: Add a packet with a sequence number lower than the next expected one ---
        // This simulates an out-of-order packet that we already consumed, or a duplicate.
        // It has a sequence number of 1, which is less than the expected 3.
        // The packet will be considered "stale" later.
        state.addNewPacket(1L, new byte[]{1});
        assertEquals(1, state.getPackets()
                .size(), "Packet 1 should be in the map.");

        // --- Step 3: Add a new, non-stale packet to ensure it is not removed ---
        // This packet (with seq # 4) should be kept, because its sequence number is
        // greater than the next expected number (3).
        state.addNewPacket(4L, new byte[]{4});
        assertEquals(2, state.getPackets()
                .size(), "Packets 1 and 4 should be in the map.");

        // --- Step 4: Simulate time passing to make packet 1 stale ---
        // We wait for 2 seconds to make the packet from step 2 old enough.
        Thread.sleep(2000);

        // --- Step 5: Trigger cleanup and assert ---
        // The cleanup should remove packet 1 because:
        // 1. Its sequence number (1) < the next expected number (3).
        // 2. Its age is greater than 1 second.
        // Packet 4 should be kept.
        state.cleanupStalePaquets(1);

        assertEquals(1, state.getPackets()
                .size(), "Only packet 4 should remain after cleanup.");
        assertTrue(state.getPackets()
                .containsKey(4L), "Packet 4 should be present.");
    }
}