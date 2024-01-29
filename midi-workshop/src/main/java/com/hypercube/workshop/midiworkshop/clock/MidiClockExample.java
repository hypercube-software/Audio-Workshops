package com.hypercube.workshop.midiworkshop.clock;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.clock.*;
import com.hypercube.workshop.midiworkshop.common.seq.TimeSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MidiClockExample {

    void startClock(MidiClockType clockType, MidiOutDevice out, int tempo) {

        try (MidiClock clock = clockType == MidiClockType.SEQ ? new SequencerBasedMidiClock(out) : new TimerBasedMidiClock(out)) {

            TimeSignature ts = new TimeSignature(3, 4);
            MidiPosition pos = new MidiPosition(ts);
            long currentTickPosition = 0;
            clock.start();
            out.sendPlay();
            clock.updateTempo(tempo);
            for (; ; ) {
                clock.waitNextMidiTick();
                if (currentTickPosition % pos.getTickPerDiv() == 0) {
                    log.info(pos.getPosition(currentTickPosition));
                }
                currentTickPosition++;
            }
        } catch (RuntimeException | IOException e) {
            if (e.getCause() instanceof InterruptedException)
                return;
            log.error("Unexpected error", e);
        }

    }
}
