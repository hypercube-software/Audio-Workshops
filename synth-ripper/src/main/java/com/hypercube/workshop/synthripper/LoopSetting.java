package com.hypercube.workshop.synthripper;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoopSetting {
    private int cc;
    private int note;
    private long sampleStart;
    private long sampleEnd;
}
