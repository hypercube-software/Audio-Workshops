package com.hypercube.mpm.udp;

public record UDPPacket(long receptionTimestamp, long sendTimestamp, byte[] payload) {
}
