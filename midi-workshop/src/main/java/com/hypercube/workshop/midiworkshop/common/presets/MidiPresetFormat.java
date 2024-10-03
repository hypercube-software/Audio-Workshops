package com.hypercube.workshop.midiworkshop.common.presets;

public enum MidiPresetFormat {
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
     * Send CC "BANK Select MSB" only, then Program Change -1
     */
    BANK_MSB_PRG1,
    /**
     * Send CC "BANK Select LSB" only, then Program Change -1
     */
    BANK_LSB_PRG1,
    /**
     * Send CC "BANK Select MSB" then "BANK Select LSB", then Program Change -1
     */
    BANK_MSB_LSB_PRG1
}
