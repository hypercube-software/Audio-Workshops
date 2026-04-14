package com.hypercube.workshop.midiworkshop.api.sysex.library.io.response;

import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;

public interface SysExUpdateListener {
    void onBufferUpdate(MidiInDevice device, MidiRequestResponse partialResponse);
}
