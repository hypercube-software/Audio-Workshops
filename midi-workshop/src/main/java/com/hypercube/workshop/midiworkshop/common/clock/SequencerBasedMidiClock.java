package com.hypercube.workshop.midiworkshop.common.clock;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.errors.MidiError;
import com.hypercube.workshop.midiworkshop.common.seq.MidiSequence;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.io.IOException;

@Slf4j
public class SequencerBasedMidiClock implements MidiClock {
    public static final int CLOCK_RESOLUTION_PPQ = 24 * 8;
    private final Sequencer clockSequencer;

    private final Object midiTickSignal = new Object();

    public SequencerBasedMidiClock(MidiOutDevice clock) {
        try {
            clockSequencer = MidiSystem.getSequencer(false);
            // javax.sound.midi.Sequencer send all note off on loop end which is a VERY bad idea for the MIDI Clock accuracy
            // so we need to filter out this and only send MIDI CLOCK to the output device
            clockSequencer.getTransmitter()
                    .setReceiver(new Receiver() {
                        @Override
                        public void send(MidiMessage message, long timeStamp) {
                            if (message.getStatus() == ShortMessage.TIMING_CLOCK) {
                                clock.send(new MidiEvent(message, timeStamp));
                                synchronized (midiTickSignal) {
                                    midiTickSignal.notifyAll();
                                }
                            }
                        }

                        @Override
                        public void close() {
                            // Nothing to do
                        }
                    });
            clockSequencer.open();
            MidiSequence cs = new MidiSequence(CLOCK_RESOLUTION_PPQ);
            cs.addClock(0, 1024);
            cs.assignToSequencer(clockSequencer);
            clockSequencer.setLoopStartPoint(0);
            clockSequencer.setLoopEndPoint(cs.getTickDuration());
            clockSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            throw new MidiError(e);
        }
    }

    @Override
    public void close() throws IOException {
        stop();
        clockSequencer.close();
    }

    @Override
    public void start() {
        clockSequencer.start();
    }

    @Override
    public void stop() {
        clockSequencer.stop();
    }

    @Override
    public void updateTempo(int tempo) {
        clockSequencer.setTempoFactor(1);
        clockSequencer.setTempoInBPM(tempo);
    }

    @Override
    public void waitNextMidiTick() {
        synchronized (midiTickSignal) {
            try {
                midiTickSignal.wait();
            } catch (InterruptedException e) {
                log.warn("Interrupted", e);
            }
        }
    }

    @Override
    public int getResolutionPPQ() {
        return CLOCK_RESOLUTION_PPQ;
    }
}
