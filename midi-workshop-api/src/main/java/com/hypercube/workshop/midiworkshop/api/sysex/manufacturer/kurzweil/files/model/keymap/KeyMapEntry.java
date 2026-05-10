package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *  typedef struct  {
 *      byte    tuneMsb;
 *      byte    tuneLsb;
 *      byte    atten;
 *      ubyte   sblkMsb;
 *      ubyte   sblkLsb;
 *      ubyte   root;
 * } Entry;
 * </pre>
 */
@Getter
@Setter
@JsonPropertyOrder({"tuning", "atten", "sblk", "root"})
public class KeyMapEntry {
    private int tuning;
    private int atten;
    private int sblk;
    private int root;
}
