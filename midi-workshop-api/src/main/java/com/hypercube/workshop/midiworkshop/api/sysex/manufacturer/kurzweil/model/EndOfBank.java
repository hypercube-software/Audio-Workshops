package com.hypercube.workshop.midiworkshop.api.sysex.manufacturer.kurzweil.model;

import lombok.Getter;

/**
 * ENDOFBANK response: sent after the last WRITE or INFO of a READBANK or DIRBANK answer.
 * Carries the bank the preceding objects belong to. Size is always 0.
 */
@Getter
public final class EndOfBank extends BaseObject {
    private final int bankId;

    public EndOfBank(int objectType, int bankId) {
        super(objectType, 0, 0);
        this.bankId = bankId;
    }
}
