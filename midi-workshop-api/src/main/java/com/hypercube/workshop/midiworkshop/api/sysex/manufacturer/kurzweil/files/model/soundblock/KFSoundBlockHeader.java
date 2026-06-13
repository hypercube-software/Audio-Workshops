package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 * sound file header, contained in SBLK
 * typedef struct {
 *   ubyte	rootk;     // MIDI key #
 *   ubyte flags;
 *   byte	amp1;    // normal attack amp adjust )
 *   byte	amp2;    // alt attack amp adjust
 *   word	pitch;    // pitch at highest playback rate
 *   word	name;     // offset to name if any, 0 if none
 *   long	sos;     // normal start of span
 *   long	alt;     // alt (legato)  start of span
 *   long	los;      // loop of span
 *   long	eos;      // end of span
 *   word	env1;     // normal expansion envelope
 *   word	env2;     // alt (legato) expansion env
 *   long	srate;    // 1/sampling rate in nanosecs
 * } SFH;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"rootk", "flags", "amp1", "amp2", "pitch", "nameOffset", "sos", "alt", "los", "eos", "env1", "env2", "srate"})
public class KFSoundBlockHeader {
    /**
     * Root MIDI key
     */
    private int rootk;
    private int flags;
    private int amp1;
    private int amp2;
    private int pitch;
    private int nameOffset; // 'name' in C struct is actually an offset
    /**
     * Sample start
     */
    private long sos;
    /**
     * Alt Sample start
     */
    private long alt;
    /**
     * sample loop start
     */
    private long los;
    /**
     * Sample end
     */
    private long eos;
    private int env1;
    private int env2;
    /**
     * Sample period in nanoseconds
     */
    private long srate;
    @JsonIgnore
    private byte[] pcmData;

    public long sampleStart() {
        return sos;
    }

    public long altSampleStart() {
        return alt;
    }

    public long sampleLoopStart() {
        return los;
    }

    public long sampleEnd() {
        return eos;
    }

    public long samplePeriodNs() {
        return srate;
    }

    @JsonGetter("sampleLength")
    public long sampleLength() {
        return sampleEnd() - sampleStart() + 1;
    }

    @JsonGetter("bitDepth")
    public int bitDepth() {
        return byteDepth() * 8;
    }

    public int byteDepth() {
        return 2;
    }

    public void setPcmData(byte[] data) {
        this.pcmData = data;
        relativize();
    }

    @JsonGetter("sampleRate")
    public int sampleRate() {
        return samplePeriodNs() == 0 ? 0 : (int) Math.floor((1F / samplePeriodNs()) * 1_000_000_000L);
    }

    public void relocate(long baseMemoryAddress) {
        relativize();
        sos += baseMemoryAddress;
        alt += baseMemoryAddress;
        los += baseMemoryAddress;
        eos += baseMemoryAddress;
    }

    private void relativize() {
        long offset = sos;
        sos = relativize(sos, offset);
        alt = relativize(alt, offset);
        los = relativize(los, offset);
        eos = relativize(eos, offset);
    }

    private long relativize(long value, long startPos) {
        return value != 0 ? value - startPos : 0;
    }
}
