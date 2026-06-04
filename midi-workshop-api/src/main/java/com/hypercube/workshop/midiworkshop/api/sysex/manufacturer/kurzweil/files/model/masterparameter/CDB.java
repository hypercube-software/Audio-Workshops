package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil MIDI Channel Parameter Block (cdb).
 * The structure is based on the `cdb` definition found in `cdb.h`.
 *
 * <pre><code>
 * typedef struct {
 * 	ubyte tag;
 * 	ubyte chan;
 * 	ubyte nlyrs;
 * 	ubyte flags; // channel enable state (1 bit), ignore program changes (1 bit)
 * 	word prog;
 * 	ubyte volume; // MIDI volume (7 bits), vollock (1 bit)
 * 	ubyte pan;    // MIDI pan (7 bits), panlock (1 bit)
 * 	ubyte trans;
 * 	ubyte dtune;
 * 	ubyte brange;
 * 	ubyte playflags; // mono mode override (2 bits), portamento override (2 bits)
 * 	ubyte portRate;
 * 	ubyte outflags; // output pair override (3 bits), output headroom override (4 bits)
 * 	ubyte rfu[2];
 * } cdb;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"tag", "chan", "nlyrs", "flags", "prog", "volume", "pan", "trans", "dtune", "brange", "playflags", "portRate", "outflags", "rfu"})
public class CDB {
    private int tag;
    /**
     * channel #, set and forget
     */
    private int chan;
    /**
     * # layers to allocate, usually 3, except drum channel is 32
     */
    private int nlyrs;
    private int flags; // channel enable state (1 bit), ignore program changes (1 bit)
    /**
     * program number
     */
    private int prog;
    private int volume; // MIDI volume (7 bits), vollock (1 bit)
    private int pan;    // MIDI pan (7 bits), panlock (1 bit)
    /**
     * channel transpose; semis
     */
    private int trans;
    /**
     * channel detune; cents
     */
    private int dtune;
    /**
     * bend range override; prog/+-60 !semitones damn it!
     */
    private int brange;
    private int playflags; // mono mode override (2 bits), portamento override (2 bits)
    /**
     * portamento rate for use with override
     */
    private int portRate;
    private int outflags; // output pair override (3 bits), output headroom override (4 bits)
    /**
     * Reserved for future use
     */
    private int[] rfu = new int[2];
}
