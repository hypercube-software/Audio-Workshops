package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.files.model.keymap;

import lombok.Getter;

@Getter
public enum KeyMapMask {
    TUNING_SHORT(0x10),
    TUNING_BYTE(0x80),
    VOLUME_ATTEN(0x04),
    SAMPLE_ID(0x02),
    SAMPLE_ROOT(0x01);

    private int mask;

    KeyMapMask(int mask) {
        this.mask = mask;
    }
}
