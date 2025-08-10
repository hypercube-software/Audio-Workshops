package com.hypercube.workshop.midiworkshop.api.seq;

import com.hypercube.workshop.midiworkshop.api.CustomMidiEvent;
import com.hypercube.workshop.midiworkshop.api.MidiMetaMessages;
import com.hypercube.workshop.midiworkshop.api.MidiNote;

import javax.sound.midi.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulate javax.sound.midi.Sequence
 */
public class MidiSequence {
    private static final int DEFAULT_SEQUENCE_RESOLUTION_PPQ = 24 * 4;
    private final TimeSignature timeSignature;
    private final KeySignature keySignature;
    private final Sequence sequence;

    private final int sequenceResolutionPPQ;
    private final Map<Integer, Integer> trackToChannel = new HashMap<>();
    private final int tickPerWhole;
    private final int nbTracks;

    public MidiSequence(int sequenceResolutionPPQ) throws InvalidMidiDataException {
        this.sequenceResolutionPPQ = sequenceResolutionPPQ;
        this.sequence = new Sequence(Sequence.PPQ, sequenceResolutionPPQ, 1);
        this.timeSignature = new TimeSignature(4, 4);
        this.keySignature = new KeySignature(0, true);
        this.tickPerWhole = sequenceResolutionPPQ * 4;
        this.nbTracks = 1;
        initChannelMap();
    }

    public MidiSequence(int nbTracks, TimeSignature timeSignature, KeySignature keySignature) throws InvalidMidiDataException {
        this.sequenceResolutionPPQ = DEFAULT_SEQUENCE_RESOLUTION_PPQ;
        this.sequence = new Sequence(Sequence.PPQ, sequenceResolutionPPQ, nbTracks + 1);
        this.timeSignature = timeSignature;
        this.keySignature = keySignature;
        this.tickPerWhole = sequenceResolutionPPQ * 4;
        this.nbTracks = nbTracks;
        initChannelMap();
    }

    /**
     * By default track i is assigned to midi channel i
     */
    private void initChannelMap() {
        for (int t = 0; t < nbTracks; t++) {
            trackToChannel.put(t, t);
        }
    }


    public void addClock(int track, int tickDuration) throws InvalidMidiDataException {
        final long _ticksPerClock = sequenceResolutionPPQ / 24;
        for (long i = 0; i < tickDuration / _ticksPerClock; i++) {
            sequence.getTracks()[track].add(new CustomMidiEvent(new ShortMessage(ShortMessage.TIMING_CLOCK), i * _ticksPerClock));
        }
    }

    private int toTick(RelativeTimeUnit duration) {
        return tickPerWhole * duration.numerator() / duration.denominator();
    }

    public void assignToSequencer(Sequencer sequencer) throws InvalidMidiDataException {
        sequencer.setSequence(sequence);
    }

    public void setTrackChannel(int track, int midiChannel) {
        trackToChannel.put(track, midiChannel);
    }

    public void addNote(int track, String noteName, int timestamp, RelativeTimeUnit duration) throws InvalidMidiDataException {
        addNote(track, noteName, 127, timestamp, duration);
    }

    public void addNote(int track, String noteName, RelativeTimeUnit timestamp, RelativeTimeUnit duration) throws InvalidMidiDataException {
        addNote(track, noteName, 127, toTick(timestamp), duration);
    }

    public void addNote(int track, String noteName, int velocity, RelativeTimeUnit timestamp, RelativeTimeUnit duration) throws InvalidMidiDataException {
        addNote(track, noteName, velocity, toTick(timestamp), duration);
    }

    public void addNote(int track, String noteName, int velocity, long tickPosition, RelativeTimeUnit duration) throws InvalidMidiDataException {
        MidiNote note = MidiNote.fromName(noteName);
        int channel = trackToChannel.get(track);
        sequence.getTracks()[track].add(new CustomMidiEvent(new ShortMessage(ShortMessage.NOTE_ON, channel, note.value(), velocity), tickPosition));
        sequence.getTracks()[track].add(new CustomMidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, channel, note.value(), 0), tickPosition + toTick(duration) - 1));
    }

    private void addTempoChange(int track, int tempo, int tickPosition) throws InvalidMidiDataException {
        byte[] data = new byte[3];
        // microseconds per quarternote
        long mpqn = 60000000 / tempo;
        data[0] = (byte) ((mpqn >> 16) & 0xFF);
        data[1] = (byte) ((mpqn >> 8) & 0xFF);
        data[2] = (byte) ((mpqn >> 0) & 0xFF);
        sequence.getTracks()[track].add(new CustomMidiEvent(new MetaMessage(MidiMetaMessages.META_TEMPO_TYPE, data, data.length), tickPosition));
    }

    public int getTickDuration() {
        return (int) sequence.getTickLength();
    }
}
