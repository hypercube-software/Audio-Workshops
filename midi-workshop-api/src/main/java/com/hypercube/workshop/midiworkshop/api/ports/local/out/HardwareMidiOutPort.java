package com.hypercube.workshop.midiworkshop.api.ports.local.out;

import com.hypercube.workshop.midiworkshop.api.ControlChangeFilter;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.util.List;
import java.util.Optional;

@Slf4j
public class HardwareMidiOutPort extends MidiOutPort {
    private final MidiDevice port;
    private Receiver receiver;

    public HardwareMidiOutPort(MidiDevice port) {
        super(port.getDeviceInfo()
                .getName());
        this.port = port;
    }

    @Override
    public boolean isOpen() {
        return port != null && port.isOpen();
    }

    public void bind(Sequencer sequencer) throws MidiUnavailableException {
        sequencer.getTransmitter()
                .setReceiver(receiver);
    }

    public void bindToSequencer(Sequencer sequencer) throws MidiUnavailableException {
        sequencer.getTransmitter()
                .setReceiver(new ControlChangeFilter(receiver, List.of(
                        MidiOutPort.CTRL_ALL_CONTROLLERS_OFF,
                        MidiOutPort.CTRL_ALL_NOTE_OFF,
                        MidiOutPort.CTRL_ALL_SOUND_OFF
                )));
    }

    public void send(MidiMessage msg, long timestamp) {
        if (!isOpen()) {
            throw new MidiError(port, "Open the device before sending anything");
        }
        receiver.send(msg, timestamp);
    }

    @Override
    protected void effectiveOpen() throws MidiUnavailableException {
        if (!port.isOpen()) {
            port.open();
        }
        if (receiver == null) {
            receiver = port.getReceiver();
        } else {
            log.info("Receiver already set for {}", getName());
        }
        if (receiver == null) {
            throw new MidiError("Unable to listen '%s' receiver is null".formatted(getName()));
        }
    }

    @Override
    protected void effectiveClose() {
        log.info("Effective close on {}", name);
        Optional.ofNullable(receiver)
                .ifPresent(Receiver::close);
        receiver = null;
        port.close();
    }
}
