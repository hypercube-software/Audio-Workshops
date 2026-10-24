package com.hypercube.workshop.midiworkshop.api.ports.remote.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkMidiOutPortTest {

    @Test
    void detectFullRemoteAddress() {
        assertTrue(NetworkMidiOutPort.isRemoteAddress("hypercube.serverpit.com:10192:DS-330"));
        assertTrue(NetworkMidiOutPort.isRemoteAddress("192.168.1.5:10192:TG-500"));
    }

    @Test
    void rejectHardwarePortNamesContainingColon() {
        // Hardware MIDI port names must never be confused with remote device definitions
        assertFalse(NetworkMidiOutPort.isRemoteAddress("debug: port 1"));
        assertFalse(NetworkMidiOutPort.isRemoteAddress("taurus"));
        assertFalse(NetworkMidiOutPort.isRemoteAddress("FromDAW"));
        assertFalse(NetworkMidiOutPort.isRemoteAddress("loopMIDI Port"));
        // a bare "host:port" is a partial remote definition, not a full remote address
        assertFalse(NetworkMidiOutPort.isRemoteAddress("192.168.1.5:10192"));
    }

    @Test
    void detectPartialRemoteHostPort() {
        assertTrue(NetworkMidiOutPort.isRemoteHostPort("hypercube.serverpit.com:10192"));
        assertTrue(NetworkMidiOutPort.isRemoteHostPort("192.168.1.5:10192"));
    }

    @Test
    void rejectHardwarePortNamesFromPartialRemoteDetection() {
        // A hardware port name containing a colon is NOT a remote endpoint
        assertFalse(NetworkMidiOutPort.isRemoteHostPort("debug: port 1"));
        assertFalse(NetworkMidiOutPort.isRemoteHostPort("taurus"));
        assertFalse(NetworkMidiOutPort.isRemoteHostPort("Port:abc"));
        // full remote definitions must not be treated as a partial host:port
        assertFalse(NetworkMidiOutPort.isRemoteHostPort("192.168.1.5:10192:TG-500"));
    }
}
