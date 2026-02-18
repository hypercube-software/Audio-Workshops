package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

public record MidiRequestResponse(byte[] request, byte[] response, String errorMessage) {
}
