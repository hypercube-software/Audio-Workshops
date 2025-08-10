package com.hypercube.mpm.midi;

import com.hypercube.workshop.midiworkshop.api.errors.MidiError;

import java.util.HashMap;
import java.util.Map;

/**
 * This class take care of keeping track of all MSB values, in order to blend them with their LSB value
 */
public class MidiTransformerState {
    public int nrpnMSBParam;
    public int nrpnLSBParam;
    public int nrpnMSBValue;
    public int nrpnLSBValue;
    public int currentOutputNRPN;
    private Map<Integer, Integer> ccMSB = new HashMap<>();

    public MidiTransformerState() {
        reset();
    }

    public boolean value14BitReady() {
        return nrpnMSBValue != -1 && nrpnLSBValue != -1;
    }

    public void reset() {
        resetParamId();
        nrpnMSBValue = -1;
        nrpnLSBValue = -1;
        ccMSB.forEach((k, v) -> ccMSB.put(k, -1));
    }

    public void resetParamId() {
        nrpnMSBParam = -1;
        nrpnLSBParam = -1;
    }

    public void consumeNRPNValue() {
        if (value14BitReady()) {
            nrpnLSBValue = -1;
            nrpnMSBValue = -1;
        } else {
            nrpnLSBValue = -1;
        }
    }

    public int getNrpnId() {
        if (is14BitsNrpnId()) {
            return (nrpnMSBParam << 8) | nrpnLSBParam;
        } else if (nrpnMSBParam != -1) {
            return nrpnMSBParam; // some devices use MSB only as ID (Novation Submit)
        } else {
            return nrpnLSBParam;
        }
    }

    public int getRealNrpnId() {
        if (is14BitsNrpnId()) {
            return (nrpnMSBParam << 7) | nrpnLSBParam;
        } else if (nrpnMSBParam != -1) {
            return nrpnMSBParam;
        } else {
            return nrpnLSBParam;
        }
    }

    public boolean is14BitsNrpnId() {
        return nrpnMSBParam != -1 && nrpnLSBParam != -1;
    }

    public int getValue14Bits() {
        return ((nrpnMSBValue << 7) | nrpnLSBValue) & 0x3FFF;
    }

    /**
     * Rescale the received NRPN value to 7 bits
     */
    public int getValue7Bits() {
        if (nrpnMSBValue != -1 && nrpnLSBValue != -1) {
            // we just drop LSB value
            return nrpnMSBValue;
        } else if (nrpnMSBValue != -1) {
            return nrpnMSBValue; // Novation Summit use only MSB value
        } else if (nrpnLSBValue != -1) {
            return nrpnLSBValue;
        } else {
            throw new MidiError("no NRPN value available yet");
        }
    }
}
