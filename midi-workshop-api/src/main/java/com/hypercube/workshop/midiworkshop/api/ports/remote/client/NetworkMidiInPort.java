package com.hypercube.workshop.midiworkshop.api.ports.remote.client;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NetworkMidiInPort extends MidiInPort {
    private final String definition;
    private final NetworkMidiOutPort owner;

    public NetworkMidiInPort(String definition, NetworkMidiOutPort owner) {
        super(definition);
        this.definition = definition;
        this.owner = owner;
    }

    @Override
    public boolean isOpen() {
        return owner.isOpen();
    }

    @Override
    protected void effectiveOpen() throws Exception {
        // Parent NetworkMidiOutPort handles the connection
    }

    void onNewMidiEvent(CustomMidiEvent event) {
        for (MidiListener listener : listeners) {
            try {
                Thread.currentThread()
                        .setName("MIDI Listener for '%s'".formatted(getName()));
                listener.onEvent(this, event);
            } catch (RuntimeException e) {
                log.error("Unexpected error in midi listener", e);
            }
        }
    }
}
