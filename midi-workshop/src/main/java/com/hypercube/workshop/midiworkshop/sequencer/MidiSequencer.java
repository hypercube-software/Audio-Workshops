package com.hypercube.workshop.midiworkshop.sequencer;

import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.clock.MidiClockType;
import com.hypercube.workshop.midiworkshop.common.seq.KeySignature;
import com.hypercube.workshop.midiworkshop.common.seq.MidiSequence;
import com.hypercube.workshop.midiworkshop.common.seq.RelativeTimeUnit;
import com.hypercube.workshop.midiworkshop.common.seq.TimeSignature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.io.InputStream;

import static com.hypercube.workshop.midiworkshop.common.seq.RelativeTimeUnit.*;

@Component
@Slf4j
public class MidiSequencer {

    public void playResource(MidiOutDevice out, String path, int tempo) {
        try (com.hypercube.workshop.midiworkshop.common.seq.MidiSequencer sequencer = new com.hypercube.workshop.midiworkshop.common.seq.MidiSequencer(tempo, MidiClockType.NONE, null, out)) {
            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new RuntimeException("Resource not found: " + path);
                }
                sequencer.setSequence(in);

                int duration = (int) (sequencer.getMicrosecondLength() / 1000000f);
                log.info("Play for " + duration + " sec");

                sequencer.start();

                sequencer.waitEndOfSequence();
                sequencer.stop();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }


    public void playSequence(MidiClockType clockType, MidiOutDevice clock, MidiOutDevice out, int tempo) {
        try (com.hypercube.workshop.midiworkshop.common.seq.MidiSequencer sequencer = new com.hypercube.workshop.midiworkshop.common.seq.MidiSequencer(tempo, clockType, clock, out)) {

            MidiSequence s = new MidiSequence(4, new TimeSignature(4, 4), new KeySignature(0, false));
            generateTrack1(s);
            generateTrack2(s);
            generateTrack3(s);

            sequencer.setSequence(s, true);


            int duration = (int) (sequencer.getMicrosecondLength() / 1000000f);
            log.info("Play for " + duration + " sec");

            sequencer.start();
            sequencer.waitEndOfSequence();
            sequencer.stop();
        } catch (RuntimeException e) {
            if (!(e.getCause() instanceof InterruptedException)) {
                log.error("Unexpected error", e);
            }
        } catch (InvalidMidiDataException | IOException e) {
            log.error("Unexpected error", e);
        }
    }

    private void generateTrack1(MidiSequence seq) throws InvalidMidiDataException {
        final int trackId = 1;
        final RelativeTimeUnit _2Bar = _1_1.mult(2);
        final RelativeTimeUnit _4Bar = _1_1.mult(4);
        final RelativeTimeUnit _6Bar = _1_1.mult(6);
        seq.addNote(trackId, "C2", 0, _2Bar);
        seq.addNote(trackId, "C3", 0, _2Bar);
        seq.addNote(trackId, "C4", 0, _2Bar);

        seq.addNote(trackId, "A#1", _2Bar, _2Bar);
        seq.addNote(trackId, "A#2", _2Bar, _2Bar);
        seq.addNote(trackId, "A#3", _2Bar, _2Bar);

        seq.addNote(trackId, "F1", _4Bar, _2Bar);
        seq.addNote(trackId, "F2", _4Bar, _2Bar);
        seq.addNote(trackId, "F3", _4Bar, _2Bar);

        seq.addNote(trackId, "G1", _6Bar, _2Bar);
        seq.addNote(trackId, "G2", _6Bar, _2Bar);
        seq.addNote(trackId, "G3", _6Bar, _2Bar);
    }

    private void generateTrack2(MidiSequence seq) throws InvalidMidiDataException {
        final int trackId = 0;
        final RelativeTimeUnit _2Bar = _1_1.mult(2);
        final RelativeTimeUnit _4Bar = _1_1.mult(4);
        final RelativeTimeUnit _6Bar = _1_1.mult(6);
        seq.addNote(trackId, "C2", 0, _2Bar);
        seq.addNote(trackId, "A#2", _2Bar, _2Bar);
        seq.addNote(trackId, "F2", _4Bar, _2Bar);
        seq.addNote(trackId, "G2", _6Bar, _2Bar);
    }

    private void generateTrack3(MidiSequence seq) throws InvalidMidiDataException {
        final int trackId = 2;

        // Kick and Snare
        for (int i = 0; i < 8; i++) {
            seq.addNote(trackId, "C1", _1_1.mult(i), RelativeTimeUnit._1_8);
            seq.addNote(trackId, "D1", _1_1.mult(i).plus(_1_2), RelativeTimeUnit._1_8);
            seq.addNote(trackId, "C1", 90, _1_1.mult(i + 1).minus(_1_8), RelativeTimeUnit._1_8);
        }

        // HitHat
        for (int i = 0; i < 64; i++) {
            String note = "F#1";
            if (i % 2 == 0) {
                note = "G#1";
            }
            if ((i + 1) % 16 == 0) {
                note = "A#1";
            }
            seq.addNote(trackId, note, 64, _1_8.mult(i), RelativeTimeUnit._1_8);
        }
    }

}
