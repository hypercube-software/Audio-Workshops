package com.hypercube.workshop.midiworkshop.common.presets;

public enum MidiBankFormat {
    /**
     * Send Program Change only
     */
    NO_BANK,
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
    BANK_MSB_LSB_PRG
}