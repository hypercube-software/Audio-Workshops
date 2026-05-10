package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.soundblock;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KFSoundBlockHeader {
    private int rootk;/* MIDI key # */
    private int flags;
    private int amp1;/* normal attack amp adjust ) */
    private int amp2;/* alt attack amp adjust */
    private int pitch;/* pitch at highest playback rate*/
    private int nameOffset;/* offset to name if any, 0 if none */
    private long sos;/* normal start of span */
    private long alt;/* alt (legato)  start of span */
    private long los; /* loop of span */
    private long eos;/* end of span */
    private int env1;/* normal expansion envelope */
    private int env2;/* alt (legato) expansion env */
    private long srate;/* 1/sampling rate in nanosecs */

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
