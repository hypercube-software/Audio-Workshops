package com.hypercube.workshop.audioworkshop.api.line;

import com.hypercube.workshop.audioworkshop.api.format.PCMBufferFormat;
import lombok.Getter;

@Getter
public abstract class AudioLine {
    protected final PCMBufferFormat format;

    protected AudioLine(PCMBufferFormat format) {
        this.format = format;
    }
}
