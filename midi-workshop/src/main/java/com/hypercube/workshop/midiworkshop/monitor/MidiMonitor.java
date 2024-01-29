package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;


@Slf4j
@Component
public class MidiMonitor {

    void monitor(MidiInDevice midiInDevice) {
        try {
            midiInDevice.listen(this::onMidiEvent);
        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + midiInDevice.getName());
        }
    }

    private void onMidiEvent(CustomMidiEvent evt) {
        if (evt.getMessage()
                .getStatus() == ShortMessage.NOTE_ON) {
            MidiNote note = MidiNote.fromValue(evt.getMessage()
                    .getMessage()[1]);
            int velocity = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: " + evt.getHexValues() + " Note ON:" + note.name() + " Velocity: " + velocity);
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.NOTE_OFF) {
            MidiNote note = MidiNote.fromValue(evt.getMessage()
                    .getMessage()[1]);
            int velocity = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: " + evt.getHexValues() + " Note OFF:" + note.name() + " Velocity: " + velocity);
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.CONTROL_CHANGE) {
            MidiControl ctrl = MidiControl.fromId(evt.getMessage()
                    .getMessage()[1]);
            int value = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: " + evt.getHexValues() + " Control Change:" + ctrl.name() + " Value: " + value);
        } else {
            log.info("MIDI: " + evt.getHexValues());
        }
    }

    void filter(MidiInDevice in, MidiOutDevice out) {
        try {
            out.open();
            in.listen(evt -> filterEvent(evt, out));

        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + in.getName());
        } finally {
            out.close();
        }
    }

    private void filterEvent(CustomMidiEvent evt, MidiOutDevice out) {
        log.info("MIDI: " + evt.getHexValues());
        out.send(evt);
    }

}
