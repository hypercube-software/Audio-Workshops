package com.hypercube.workshop.midiworkshop.api.ports.remote.client;

import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.Socket;

public record MidiOutConnection(NetworkClientSession session, DatagramSocket clientUDPSocket, Socket clientTCPSocket,
                                OutputStream tcpOutputStream, java.io.InputStream tcpInputStream) {
}
