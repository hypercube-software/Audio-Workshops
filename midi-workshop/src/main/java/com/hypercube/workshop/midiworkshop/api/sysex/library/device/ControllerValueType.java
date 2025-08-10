package com.hypercube.workshop.midiworkshop.api.sysex.library.device;

import com.hypercube.workshop.midiworkshop.api.sysex.macro.CommandCall;

public enum ControllerValueType {
    /**
     * 8 bits values
     */
    CC,
    /**
     * 16 bits values
     */
    CC_MSB_LSB,
    /**
     * 7 bits values via MSB only, no LSB. This is what MIDI spec recommend
     */
    NRPN_MSB,
    /**
     * 7 bits values via LSB only, no MSB. This is what Alesis often uses
     */
    NRPN_LSB,
    /**
     * 14 bits values via MSB and LSB
     */
    NRPN_MSB_LSB,
    /**
     * Send a SYSEX as controller (rely on a {@link CommandCall})
     */
    SYSEX

}
