package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

public record MidiRequestResponse(byte[] request, byte[] response, String errorMessage) {
    public static MidiRequestResponse of(String msg) {
        return new MidiRequestResponse(null, null, msg);
    }

    public boolean hasError() {
        return errorMessage != null;
    }
}
