package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;

public interface SysExUpdateListener {
    void onBufferUpdate(MidiInPort midiInPort, MidiRequestResponse partialResponse);
}
