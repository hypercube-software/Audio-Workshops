package com.hypercube.workshop.midiworkshop.common.presets;

public enum MidiBankFormat {
    /**
     * Send Program Change only
     */
    NO_BANK_PRG,
    /**
     * Send CC "BANK Select MSB" only, then Program Change
     */
    BANK_MSB_PRG,
    /**
     * Send CC "BANK Select LSB" only, then Program Change
     */
    BANK_LSB_PRG,
    /**
     * Send CC "BANK Select MSB" then "BANK Select LSB", then Program Change
     */
    BANK_MSB_LSB_PRG,
    /**
     * Send 2 Program Changes (used by Yamaha TG-77 in 'direct mode')
     */
    BANK_PRG_PRG
}
