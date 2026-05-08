package com.hypercube.workshop.midiworkshop.api.ports.remote.server;

/**
 * Network packets use no timestamps on purpose. We rely on a monotonic counter with packetId because client and server will never share the same clock.
 * <p>This mean we don't try to fix any jitter or network congestion, we receive MIDI messages and send them ASAP to the device</p>
 */
public record NetworkPacket(NetworkServerSession session, long packetId, byte[] payload) {
}
