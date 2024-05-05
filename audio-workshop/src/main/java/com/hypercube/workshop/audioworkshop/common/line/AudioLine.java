package com.hypercube.workshop.audioworkshop.common.line;

import lombok.Getter;

@Getter
public abstract class AudioLine {
    protected final AudioLineFormat format;

    protected AudioLine(AudioLineFormat format) {
        this.format = format;
    }
}
