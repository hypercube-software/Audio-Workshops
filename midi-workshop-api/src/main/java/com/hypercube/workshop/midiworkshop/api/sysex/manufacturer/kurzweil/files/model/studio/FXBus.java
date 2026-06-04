package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil KDFX insert effects bus (FXBus).
 * The structure is based on the `FXBus` definition found in `lisaFXObjs.h`.
 *
 * <pre><code>
 * typedef struct {
 * 	ubyte	version;
 * 	ubyte	busID;
 * 	word	multiEffect;
 * 	ubyte	alloc;
 *
 * 	ubyte	auxLevel;
 * 	ubyte	auxBal;
 * 	ubyte	mixLevel;
 * 	ubyte	mixBal;
 * 	ubyte   wetDry;
 * 	ubyte   wetDry2;
 * 	ubyte   auxSend;
 * 	ubyte   wetDrySel;
 * 	ubyte   wetDrySel2;
 * 	ubyte   mixSend;
 * 	ubyte   rfu;
 * 	ubyte   rfu2[4];
 * } FXBus;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({
    "version", "busID", "multiEffect", "alloc",
    "auxLevel", "auxBal", "mixLevel", "mixBal", "wetDry", "wetDry2", "auxSend",
    "wetDrySel", "wetDrySel2", "mixSend", "rfu", "rfu2"
})
public class FXBus {
    private int version;
    private int busID;
    private int multiEffect; // word in C, stored as int (unsigned short)
    private int alloc;

    private int auxLevel;
    private int auxBal;
    private int mixLevel;
    private int mixBal;
    private int wetDry;
    private int wetDry2;
    private int auxSend;
    private int wetDrySel;
    private int wetDrySel2;
    private int mixSend;
    private int rfu;
    private int[] rfu2 = new int[4];
}
