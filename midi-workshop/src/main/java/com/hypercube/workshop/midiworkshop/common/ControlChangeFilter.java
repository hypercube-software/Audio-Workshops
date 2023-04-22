package com.hypercube.workshop.midiworkshop.common;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class ControlChangeFilter implements Receiver {

    private final Receiver receiver;

    private final List<Integer> filter;

    @Override
    public void send(MidiMessage message, long timeStamp) {
        if (message.getStatus() == ShortMessage.CONTROL_CHANGE) {
            if (filter.contains(Integer.valueOf(message.getMessage()[1]))) {
                return;
            }
        }
        try {
            receiver.send(message, timeStamp);
        } catch (IllegalStateException e) {
        }
    }

    @Override
    public void close() {
        receiver.close();
    }

}
