package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a Kurzweil KDFX input strip (FXInput).
 * The structure is based on the `FXInput` definition found in `lisaFXObjs.h`.
 *
 * <pre><code>
 * typedef struct {
 *   ubyte	version;
 *   ubyte	busID;
 *   ubyte  mode;
 *   ubyte	useStereo;
 *   ubyte  eqEnable;
 *
 *   ubyte	eq1Type;
 *   ubyte	eq1Level;
 *   ubyte	eq1Freq;
 *   ubyte	eq1Q;
 *   ubyte  rfu1;
 *
 *   ubyte	eq2Type;
 *   ubyte	eq2Level;
 *   ubyte	eq2Freq;
 *   ubyte	eq2Q;
 *   ubyte  rfu2;
 *
 *   ubyte	send1Type;
 *   ubyte	send1Assign;
 *   ubyte	send1Level;
 *   ubyte	send1Pan;
 *   ubyte	send1Width;
 *   ubyte  rfu3;
 *
 *   ubyte	send2Type;
 *   ubyte	send2Assign;
 *   ubyte	send2Level;
 *   ubyte	send2Pan;
 *   ubyte	send2Width;
 *   ubyte  rfu4;
 *   ubyte  rfu5;
 *   long  rfu6; // 4 bytes
 * } FXInput;
 * </code></pre>
 */
@Getter
@Setter
@JsonPropertyOrder({
        "version", "busID", "mode", "useStereo", "eqEnable",
        "eq1Type", "eq1Level", "eq1Freq", "eq1Q", "rfu1",
        "eq2Type", "eq2Level", "eq2Freq", "eq2Q", "rfu2",
        "send1Type", "send1Assign", "send1Level", "send1Pan", "send1Width", "rfu3",
        "send2Type", "send2Assign", "send2Level", "send2Pan", "send2Width", "rfu4", "rfu5",
        "rfu6"
})
public class FXInput {
    private int version;
    private int busID;
    private int mode;
    private int useStereo;
    private int eqEnable;

    private int eq1Type;
    private int eq1Level;
    private int eq1Freq;
    private int eq1Q;
    private int rfu1;

    private int eq2Type;
    private int eq2Level;
    private int eq2Freq;
    private int eq2Q;
    private int rfu2;

    private int send1Type;
    private int send1Assign;
    private int send1Level;
    private int send1Pan;
    private int send1Width;
    private int rfu3;

    private int send2Type;
    private int send2Assign;
    private int send2Level;
    private int send2Pan;
    private int send2Width;
    private int rfu4;
    private int rfu5;
    private int rfu6;
}
