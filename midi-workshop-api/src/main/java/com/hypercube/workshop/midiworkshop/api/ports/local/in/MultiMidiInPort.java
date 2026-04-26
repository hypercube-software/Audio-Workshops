package com.hypercube.workshop.midiworkshop.api.ports.local.in;

import com.hypercube.workshop.midiworkshop.api.listener.MidiListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MultiMidiInPort extends MidiInPort {
    private final List<MidiInPort> ports;

    public MultiMidiInPort(List<MidiInPort> ports) {
        super(ports.stream()
                .map(MidiInPort::getName)
                .collect(Collectors.joining(",")));
        this.ports = ports;
    }

    @Override
    public void open() {
        super.open();
        for (var port : ports) {
            port.open();
        }
    }

    @Override
    public boolean isOpen() {
        boolean allOpen = true;
        for (var port : ports) {
            allOpen &= port.isOpen();
        }
        return allOpen;
    }

    @Override
    public void close() {
        for (var port : ports) {
            port.close();
        }
    }

    @Override
    public String getName() {
        var name = new StringBuilder();
        for (var port : ports) {
            if (!name.isEmpty())
                name.append(",");
            name.append(port.getName());
        }
        return name.toString();
    }

    @Override
    public void listen(MidiListener listener) {
        for (var port : ports) {
            port.addListener(listener);
        }
        waitNotListening();
        for (var port : ports) {
            port.removeListener(listener);
        }
    }

    @Override
    protected void effectiveClose() {
    }
}
