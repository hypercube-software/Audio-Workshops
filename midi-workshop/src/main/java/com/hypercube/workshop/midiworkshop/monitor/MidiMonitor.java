package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.common.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;


@Slf4j
@Component
public class MidiMonitor {
    private MidiInDevice midiInDevice;

    public void monitor(MidiInDevice midiInDevice) {
        monitor(midiInDevice, null);
    }

    public void monitor(MidiInDevice midiInDevice, MidiMonitorEventListener eventListener) {
        try {
            this.midiInDevice = midiInDevice;
            midiInDevice.listen((device, event) -> onMidiEvent(device, event, eventListener));
        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + midiInDevice.getName());
        }
    }

    public void close() {
        if (midiInDevice != null) {
            midiInDevice.stopListening();
        }
    }

    private void onMidiEvent(MidiInDevice midiInDevice, CustomMidiEvent evt, MidiMonitorEventListener eventListener) {
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
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.ACTIVE_SENSING) {
            // silent
        } else if (evt.getMessage()
                .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            var msg = (SysexMessage) (evt.getMessage());
            log.info("MIDI: System Exclusive:" + msg.getData().length + " bytes");
        } else {
            log.info("MIDI: " + evt.getHexValues());
        }
        if (eventListener != null) {
            eventListener.onEvent(midiInDevice, evt);
        }
    }

    void filter(MidiInDevice in, MidiOutDevice out) {
        try {
            in.listen((device, evt) -> filterEvent(evt, out));
        } catch (MidiUnavailableException e) {
            log.error("The Output device is Unavailable: " + in.getName());
        }
    }

    private void filterEvent(CustomMidiEvent evt, MidiOutDevice out) {
        log.info("MIDI: " + evt.getHexValues());
        out.send(evt);
    }

}
