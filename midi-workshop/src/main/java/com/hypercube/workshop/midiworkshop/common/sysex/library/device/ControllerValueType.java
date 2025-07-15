package com.hypercube.workshop.midiworkshop.common.sysex.library.device;

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
     * 7 bits values via MSB only, no LSB
     */
    NRPN_MSB,
    /**
     * 7 bits values via LSB only, no MSB
     */
    NRPN_LSB,
    /**
     * 14 bits values via MSB and LSB
     */
    NRPN_MSB_LSB,

}
