package com.hypercube.workshop.midiworkshop.api.devices;

import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class MultiMidiInDevice extends MidiInDevice {
    private final List<MidiInDevice> devices;

    public MultiMidiInDevice(List<MidiInDevice> devices) {
        super(null);
        this.devices = devices;
    }

    @Override
    public void open() {
        for (var device : devices) {
            device.open();
        }
    }

    @Override
    public boolean isOpen() {
        boolean allOpen = true;
        for (var device : devices) {
            allOpen &= device.isOpen();
        }
        return allOpen;
    }

    @Override
    public void close() {
        for (var device : devices) {
            device.close();
        }
    }

    @Override
    public String getName() {
        var name = new StringBuilder();
        for (var device : devices) {
            if (!name.isEmpty())
                name.append(",");
            name.append(device.getName());
        }
        return name.toString();
    }

    @Override
    public void listen(MidiListener listener) {
        for (var device : devices) {
            device.addListener(listener);
        }
        waitNotListening();
        for (var device : devices) {
            device.removeListener(listener);
        }
    }
}
