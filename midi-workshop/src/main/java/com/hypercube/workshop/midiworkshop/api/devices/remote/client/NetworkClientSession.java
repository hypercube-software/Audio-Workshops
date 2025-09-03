package com.hypercube.workshop.midiworkshop.api.devices.remote.client;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class NetworkClientSession {
    private final AtomicLong packetCounter = new AtomicLong();
    private final long startTimestamp = System.nanoTime();

    public long getNextPacketId() {
        return packetCounter.getAndIncrement();
    }

    public long getCurrentTimestamp() {
        return System.nanoTime() - startTimestamp;
    }
}
