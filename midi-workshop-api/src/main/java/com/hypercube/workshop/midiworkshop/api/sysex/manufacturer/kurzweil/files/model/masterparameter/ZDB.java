package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.masterparameter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil Keyboard Zone Parameter Block (zdb).
 * The structure is based on the `zdb` definition found in `zdb.h`.
 *
 * <pre><code>
 * typedef struct {
 * 	ubyte tag;
 * 	ubyte chan;
 * 	word prog;
 * 	ubyte lokey;
 * 	ubyte hikey;
 * 	ubyte flags; // xmit mode (2 bits), transmit prog chng (1 bit), transmit pitch wheel (1 bit)
 * 	ubyte trans;
 * 	ubyte ctls[8]; // control mapping (on/off in high bit, dest in low 7 bits)
 * } zdb;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"tag", "chan", "prog", "lokey", "hikey", "flags", "trans", "ctls"})
public class ZDB {
    /**
     * = zoneSegTag+{0...nZONES-1}
     */
    private int tag;
    /**
     * xmit channel number
     */
    private int chan;
    /**
     * program number
     */
    private int prog;
    /**
     * Zone low key
     */
    private int lokey;
    /**
     * Zone High key
     */
    private int hikey;
    private int flags; // xmit mode (2 bits), transmit prog chng (1 bit), transmit pitch wheel (1 bit)
    /**
     * transpose
     */
    private int trans;
    /**
     * control mapping (on/off in high bit, dest in low 7 bits)
     */
    private int[] ctls = new int[8];
}
