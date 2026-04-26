package com.hypercube.workshop.midiworkshop.monitor;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiControl;
import com.hypercube.workshop.midiworkshop.api.MidiNote;
import com.hypercube.workshop.midiworkshop.api.errors.MidiError;
import com.hypercube.workshop.midiworkshop.api.ports.local.in.MidiInPort;
import com.hypercube.workshop.midiworkshop.api.ports.local.out.MidiOutPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;


@Slf4j
@Component
public class MidiMonitor {
    private MidiInPort midiInPort;

    public void monitor(MidiInPort midiInPort) {
        monitor(midiInPort, null);
    }

    public void monitor(MidiInPort midiInPort, MidiMonitorEventListener eventListener) {
        try {
            this.midiInPort = midiInPort;
            midiInPort.listen((port, event) -> onMidiEvent(port, event, eventListener));
        } catch (MidiError e) {
            log.error("The Output device is Unavailable: " + midiInPort.getName());
        }
    }

    public void close() {
        if (midiInPort != null) {
            midiInPort.close();
        }
    }

    private void logHexValues(CustomMidiEvent evt) {
        log.info("MIDI: {}", evt.getHexValues());
    }

    private void onMidiEvent(MidiInPort midiInPort, CustomMidiEvent evt, MidiMonitorEventListener eventListener) {
        if (evt.getMessage()
                .getStatus() == ShortMessage.NOTE_ON) {
            MidiNote note = MidiNote.fromValue(evt.getMessage()
                    .getMessage()[1]);
            int velocity = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: {} Note ON:{} Velocity: {}", evt.getHexValues(), note.name(), velocity);
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.NOTE_OFF) {
            MidiNote note = MidiNote.fromValue(evt.getMessage()
                    .getMessage()[1]);
            int velocity = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: {} Note OFF:{} Velocity: {}", evt.getHexValues(), note.name(), velocity);
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.CONTROL_CHANGE) {
            MidiControl ctrl = MidiControl.fromId(evt.getMessage()
                    .getMessage()[1]);
            int value = evt.getMessage()
                    .getMessage()[2];
            log.info("MIDI: {} Control Change:{} Value: {}", evt.getHexValues(), ctrl.name(), value);
        } else if (evt.getMessage()
                .getStatus() == ShortMessage.ACTIVE_SENSING) {
            // silent
        } else if (evt.getMessage()
                .getStatus() == SysexMessage.SYSTEM_EXCLUSIVE) {
            var msg = (SysexMessage) (evt.getMessage());
            log.info("MIDI: System Exclusive:{} bytes", msg.getData().length);
        } else {
            logHexValues(evt);
        }
        if (eventListener != null) {
            eventListener.onEvent(midiInPort, evt);
        }
    }

    private void filterEvent(CustomMidiEvent evt, MidiOutPort out) {
        logHexValues(evt);
        out.send(evt);
    }

    void filter(MidiInPort in, MidiOutPort out) {
        try {
            in.listen((device, evt) -> filterEvent(evt, out));
        } catch (MidiError e) {
            log.error("The Output device is Unavailable: " + in.getName());
        }
    }

}
