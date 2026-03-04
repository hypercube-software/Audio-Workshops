package com.hypercube.workshop.midiworkshop.api.devices.remote.server;

public record NetworkPacket(NetworkServerSession session, long packetId, long receptionTimestamp, long sendTimestamp,
                            byte[] payload) {
}
