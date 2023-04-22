package com.hypercube.workshop.midiworkshop.common.seq;

import com.hypercube.workshop.midiworkshop.common.MidiMetaMessages;
import com.hypercube.workshop.midiworkshop.common.MidiOutDevice;
import com.hypercube.workshop.midiworkshop.common.clock.*;
import lombok.extern.slf4j.Slf4j;

import javax.sound.midi.*;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * javax.sound.midi.Sequencer does not send MIDI clock
 * <p>This class add this feature, using a separate MIDI device for the clock
 * <p>Note: Using the same MIDI out device for the song and the clock would increase sync issues
 * <p>If clock device is NULL, the sequencer won't send clock
 */
@Slf4j
public class MidiSequencer implements Closeable {
    private int tempo;
    private final MidiOutDevice out;
    private MidiClock clock;

    private static final int MIDI_CLOCK_PPQ = 24;
    private static final int SEQUENCER_PPQ = MIDI_CLOCK_PPQ;
    private Sequencer sequencer;

    private TimeSignature timeSignature = new TimeSignature(4, 4);
    private KeySignature keySignature = new KeySignature(0, true);
    private Thread shutdownHook;

    /**
     * @param tempo       init tempo
     * @param clockDevice can be null
     * @param out         MIDI out device
     */
    public MidiSequencer(int tempo, MidiClockType clockType, MidiOutDevice clockDevice, MidiOutDevice out) {
        this.tempo = tempo;
        this.out = out;
        if (clockDevice != null && clockType != MidiClockType.NONE) {
            if (clockType == MidiClockType.TMR)
                this.clock = new TimerBasedMidiClock(clockDevice);
            else
                this.clock = new SequencerBasedMidiClock(clockDevice);
        }
        try {
            out.open();
            out.sendAllOff();
            sequencer = MidiSystem.getSequencer(false);

            out.bindToSequencer(sequencer);
            sequencer.addMetaEventListener(this::OnMetaEvent);
            sequencer.open();
            shutdownHook = new Thread(this::onJVMShutdown);
            Runtime.getRuntime().addShutdownHook(shutdownHook);

        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    private void onJVMShutdown() {
        log.info("Terminating due to JVM Shutdown...");
        if (sequencer != null && out.isOpen()) {
            try {
                close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getHexValues(byte[] data) {
        StringBuilder sb = new StringBuilder((data.length + 1) * 2);
        sb.append("0x");
        for (byte b : data)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private int getUnsignedByte(byte data) {
        return data & 0xFF;
    }

    private int get24BitInteger(byte[] data, int offset) {
        return (getUnsignedByte(data[0 + offset]) << 16 | getUnsignedByte(data[1 + offset]) << 8 | getUnsignedByte(data[2 + offset]));
    }

    /**
     * Meta events comes from the MIDI file specification
     *
     * @param metaMessage
     * @see <a href="http://midi.teragonaudio.com/tech/midifile.htm"></a>
     */
    private void OnMetaEvent(MetaMessage metaMessage) {
        byte[] data = metaMessage.getMessage();
        if (metaMessage.getType() == MidiMetaMessages.META_END_OF_TRACK_TYPE) {
            log.info("Midi Meta message: end of track");
        } else if (metaMessage.getType() == MidiMetaMessages.META_TIME_SIGNATURE_TYPE) {
            timeSignature = new TimeSignature(data[3], data[4]);
            int midiClocksPerMetronomeClick = data[5];
            int shouldBe8 = data[6]; // the number of notated 32nd notes in a MIDI quarter note (24 MIDI clocks)
            log.info(String.format("Midi Meta message: TIME SIGNATURE %s", timeSignature));
        } else if (metaMessage.getType() == MidiMetaMessages.META_TRACK_NAME_TYPE) {
            int len = data[2];
            String name = new String(data, 3, len, StandardCharsets.US_ASCII);
            log.info(String.format("Midi Meta message: TRACK NAME %s", name));
        } else if (metaMessage.getType() == MidiMetaMessages.META_TEXT_TYPE) {
            int len = data[2];
            String name = new String(data, 3, len, StandardCharsets.US_ASCII);
            log.info(String.format("Midi Meta message: TEXT %s", name));
        } else if (metaMessage.getType() == MidiMetaMessages.META_INSTRUMENT_TYPE) {
            int len = data[2];
            String name = new String(data, 3, len, StandardCharsets.US_ASCII);
            log.info(String.format("Midi Meta message: INSTRUMENT %s", name));
        } else if (metaMessage.getType() == MidiMetaMessages.META_COPYRIGHT_TYPE) {
            int len = data[2];
            String name = new String(data, 3, len, StandardCharsets.US_ASCII);
            log.info(String.format("Midi Meta message: COPYRIGHT %s", name));
        } else if (metaMessage.getType() == MidiMetaMessages.META_MIDI_CHANNEL_PREFIX_TYPE) {
            int channel = data[3] + 1;
            log.info(String.format("Midi Meta message: MIDI CHANNEL PREFIX %s", channel));
        } else if (metaMessage.getType() == MidiMetaMessages.META_KEY_SIGNATURE_TYPE) {
            keySignature = new KeySignature(data[3], data[4] == 0);
            log.info(String.format("Midi Meta message: KEY SIGNATURE %s", keySignature));
        } else if (metaMessage.getType() == MidiMetaMessages.META_TEMPO_TYPE) {
            // microseconds per quarternote
            long mpqn = get24BitInteger(data, 3);
            float songTempo = 60000000f / mpqn;
            log.info(String.format("Midi Meta message: SONG TEMPO %.2f", songTempo));
            updateTempo((int) songTempo);
        } else if (metaMessage.getType() == MidiMetaMessages.META_PROPRIETARY) {

        } else {
            log.info(String.format("Midi Meta message: %2X)", metaMessage.getType()));
        }
    }

    public void setTempo(int tempo) {
        this.tempo = tempo;
    }

    private void updateTempo(int tempo) {
        sequencer.setTempoFactor(1);
        sequencer.setTempoInBPM(tempo);
    }

    public void waitEndOfSequence() {
        MidiPosition pos = new MidiPosition(timeSignature);
        long tickPos = 0;
        while (sequencer.isRunning()) {
            if (clock != null) {
                clock.waitNextMidiTick();
                if (tickPos % pos.getTickPerDiv() == 0) {
                    log.info(pos.getPosition(tickPos));
                }
                tickPos++;
            } else {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        out.sendAllOff();
        out.close();
        if (sequencer != null) {
            sequencer.close();
        }
        if (clock != null) {
            clock.close();
        }
    }

    public void setSequence(InputStream in) throws InvalidMidiDataException, IOException {
        sequencer.setSequence(in);
    }

    public void setSequence(MidiSequence sequence, boolean infiniteLoop) throws InvalidMidiDataException, IOException {
        sequence.assignToSequencer(sequencer);
        if (infiniteLoop) {
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setLoopStartPoint(0);
            sequencer.setLoopEndPoint(sequence.getTickDuration());
        }
    }

    public long getMicrosecondLength() {
        return sequencer.getMicrosecondLength();
    }

    public void start() {
        if (clock != null) {
            clock.start();
            clock.updateTempo(tempo);
            // give some time to catch the new tempo
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            clock.waitNextMidiTick();
        }
        out.sendPlay();
        sequencer.setTickPosition(0);
        sequencer.start();
        updateTempo(tempo);
    }

    public void stop() {
        out.sendPause();
        sequencer.stop();
        if (clock != null) {
            clock.stop();
        }
    }
}
