package com.hypercube.workshop.midiworkshop.api.devices.remote.client;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.devices.MidiInDevice;
import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MidiInNetworkDevice extends MidiInDevice {
    private final String definition;
    private final MidiOutNetworkDevice owner;

    public MidiInNetworkDevice(String definition, MidiOutNetworkDevice owner) {
        super(null);
        this.definition = definition;
        this.owner = owner;
    }

    @Override
    public void close() {
    }

    @Override
    public void open() {
    }

    @Override
    public boolean isOpen() {
        return owner.isOpen();
    }

    @Override
    public String getName() {
        return definition;
    }

    @Override
    protected void setupTransmitter() {
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
