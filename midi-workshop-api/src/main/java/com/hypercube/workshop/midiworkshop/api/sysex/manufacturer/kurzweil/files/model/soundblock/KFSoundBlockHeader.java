package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock;

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

    public long sampleLength() {
        return sampleEnd() - sampleStart() + 1;
    }

    public int sampleFrequency() {
        return (int) Math.ceil((1F / samplePeriodNs()) * 1_000_000_000L);
    }
}
