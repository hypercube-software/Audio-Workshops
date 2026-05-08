package com.hypercube.workshop.midiworkshop.api.ports.remote.client;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class NetworkClientSession {
    private final AtomicLong packetCounter = new AtomicLong();

    public long getNextPacketId() {
        return packetCounter.getAndIncrement();
    }
}
