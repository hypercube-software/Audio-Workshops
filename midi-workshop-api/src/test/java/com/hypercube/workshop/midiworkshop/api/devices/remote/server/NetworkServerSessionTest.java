package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

import com.hypercube.workshop.midiworkshop.api.devices.remote.msg.NetWorkMessageOrigin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkServerSessionTest {
    public static final int NETWORK_ID = 1;
    NetworkServerSession session;

    @BeforeEach
    void init() {
        session = new NetworkServerSession(null, NETWORK_ID, null);
    }

    @Test
    void regularOrder() {
        long sendTimeStamp = 0; // we don't test this here
        AtomicInteger count = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            session.addNewPacket(NetWorkMessageOrigin.TCP, (long) i, sendTimeStamp, new byte[]{(byte) i});
            int finalI = i;
            session.takePacket()
                    .ifPresent(p -> count.incrementAndGet());
        }
        assertEquals(10, count.get());
    }

    @Test
    void reorderPackets() {
        long sendTimeStamp = 0; // we don't test this here
        AtomicInteger count = new AtomicInteger();
        session.addNewPacket(NetWorkMessageOrigin.TCP, 2L, sendTimeStamp, new byte[]{2});
        session.addNewPacket(NetWorkMessageOrigin.TCP, 1L, sendTimeStamp, new byte[]{1});
        session.addNewPacket(NetWorkMessageOrigin.TCP, 0L, sendTimeStamp, new byte[]{0});

        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        assertEquals(3, count.get());
    }

    @Test
    void reorderPacketsWithPause() {
        AtomicInteger count = new AtomicInteger();
        long sendTimeStamp = 0; // we don't test this here
        session.addNewPacket(NetWorkMessageOrigin.TCP, 2L, sendTimeStamp, new byte[]{2});

        session.addNewPacket(NetWorkMessageOrigin.TCP, 1L, sendTimeStamp, new byte[]{1});

        session.addNewPacket(NetWorkMessageOrigin.TCP, 0L, sendTimeStamp, new byte[]{0});

        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        session.takePacket()
                .ifPresent(p -> count.incrementAndGet());
        assertEquals(3, count.get());
    }
}