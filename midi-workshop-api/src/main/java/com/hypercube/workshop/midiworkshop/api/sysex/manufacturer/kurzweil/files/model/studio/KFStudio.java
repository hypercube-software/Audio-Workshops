package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.studio;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.KObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.KFObject;
import com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.RawData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Kurzweil Studio object, which contains parameters for effects wiring.
 * The structure is based on the `FXStudio` definition found in `lisaFXObjs.h` (assembly section).
 *
 * <pre><code>
 * typedef struct {
 *     ubyte fxs_version;
 *     ubyte fxs_mixSend;
 *     word fxs_auxFXID;
 *     ubyte fxs_auxMixLevel;
 *     ubyte fxs_auxMixBal;
 *     ubyte fxs_mixLevel;
 *     ubyte fxs_mixBal;
 *     ubyte fxs_auxWetDry;
 *     ubyte fxs_auxWetDry2;
 *     ubyte fxs_auxWetDrySel;
 *     ubyte fxs_auxWetDrySel2;
 *     ubyte mixCoeffs[20];
 *     ubyte theOutputs[4]; // kNumOutputs is 4.
 *     FXInput theInputs[8]; // kInputsPerStudio is 8.
 *     FXBus theBusses[4]; // kBussesPerStudio is 4.
 * } FXStudio;
 * </code></pre>
 */
@JsonPropertyOrder({"type", "objectId", "name", "data", "version", "mixSend", "auxFXID", "auxMixLevel", "auxMixBal", "mixLevel", "mixBal", "auxWetDry", "auxWetDry2", "auxWetDrySel", "auxWetDrySel2", "mixCoeffs", "theOutputs", "theInputs", "theBusses"})
@Getter
@Setter
public class KFStudio extends KFObject {

    private int version;
    /**
     * not selectable, must be zero
     */
    private int mixSend;
    private int auxFXID;
    /**
     * 0 to 127
     */
    private int auxMixLevel;
    /**
     * 0 to 127
     */
    private int auxMixBal;
    /**
     * 0 to 127 main mixer level
     */
    private int mixLevel;
    /**
     * 0 to 127 main mixer balance
     */
    private int mixBal;
    private int auxWetDry;
    private int auxWetDry2;
    private int auxWetDrySel;
    private int auxWetDrySel2;
    private int[] mixCoeffs = new int[20];
    private List<Integer> theOutputs = new ArrayList<>(4);
    private List<FXInput> theInputs = new ArrayList<>(8);
    private List<FXBus> theBusses = new ArrayList<>(4);

    public KFStudio(RawData data, String name, int objectId) {
        super(data, KObject.STUDIO, name, objectId);
    }
}
