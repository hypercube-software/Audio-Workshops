package com.hypercube.workshop.midiworkshop.api.sysex.util;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class CheckSumDef {
    int size;
    int position;

    public int getStartPosition() {
        return position - size;
    }

    public int getEndPosition() {
        return position - 1;
    }
}
