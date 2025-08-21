package com.hypercube.mpm.udp;

import java.time.Instant;

public record UDPPacket(Instant receptionTime, byte[] payload) {
}
